package edu.mit.csail.cap.query
package analysis

import scala.collection.mutable
import scala.annotation.tailrec

import util.{ Digraph, Edge, Graph, Segment, Duration }

/** Slice consists of seeds connected by dependencies */
trait Slice extends Graph[Seed, SliceRule] {
  /** All events in the slice */
  def events: Set[Event] = nodes.map(_.e).toSet
}

/** Edges point _from_ initial set */
class SliceGraph(val initial: List[Seed]) extends Digraph[Seed, SliceRule] with Slice {
  for (v <- initial) add(v)

  override def add(from: Seed, l: SliceRule, to: Seed) {
    assert(from.e >= to.e, s"edge does not point to earlier event: $from to $to")
    super.add(from, l, to)
  }

  /** Compute homeo-morphically induced subgraph */
  def restrict(sub: Set[Event]): Digraph[Event, Unit] = {
    debug("computing depends graph")

    val in = homomorphism { case s: Seed => s.e }

    // compute outbound by following edges and skipping over missing events
    val m = new mutable.HashMap[Event, Set[Event]]
    def reachable(from: Event): Set[Event] =
      m.getOrElseUpdate(from,
        in.outbound(from).map {
          case (to, _) if from == to => Set()
          case (to, _) if sub(to)    => Set(to)
          case (to, _)               => reachable(to)
        }.toSet.flatten)

    val out = new Digraph[Event, Unit]

    for (
      from <- sub;
      to <- reachable(from)
    ) out.add(from, (), to)

    debug("done")

    out
  }

  def event(e: Event): Iterable[Seed] = nodes.filter(_.e == e)
}

/** Generic rule for following back dependencies */
trait SliceRule {
  def errors: Errors
  
  def expand: PartialFunction[Seed, List[Seed]]
  
  def apply(s: Seed): List[Seed] =
    expand.lift(s).getOrElse(Nil)
}

case class EmptyRule(implicit errors: Errors) extends SliceRule {
  override def toString = ""

  override def expand = {
    case _ => Nil
  }
}

case class CompositeSlicer(slices: List[SliceRule])(implicit val errors: Errors) extends SliceRule {
  override def expand = {
    case s =>
      for (slicer <- slices; seed <- slicer(s)) yield seed
  }

  def expandAll(s: Seed): List[(Seed, SliceRule)] =
    for (slicer <- slices; seed <- slicer(s)) yield (seed, slicer)

  /** Expands the initial slice by applying the rule in BFS manner */
  def expand(start: List[Seed], p: Parameters): (SliceGraph, Map[SliceRule, Duration], Map[String, Int], Map[Event, Int]) = {
    val out = new SliceGraph(start)
    debug("slice expansion of " + out + " with " + p)

    val runtime = new mutable.HashMap[SliceRule, Duration].withDefaultValue(new Duration(0L))
    val depth = new mutable.HashMap[Event, Int]
    var total = new Duration(0L)
    var n = 0
    val queue = new mutable.Queue ++ out.nodes

    while (!queue.isEmpty && n < p.ExpansionSteps && total.duration < p.ExpansionTimeout) {
      n += 1
      val cur = queue.dequeue()

      for (slice <- slices) {
        val (seeds, t) = timed(slice(cur))
        runtime(slice) += t
        total += t

        for (next <- seeds) {
          // increment depth for non-local transitions
          if (!depth.contains(next.e)) {
            val local = cur.e.parent == next.e.parent
            depth(next.e) = depth.getOrElse(cur.e, 0) + (if (local) 0 else 1)
          }

          if (!out.has(next) && depth(next.e) <= p.CoverDepth)
            queue.enqueue(next)

          out.add(cur, slice, next)
        }
      }
    }

    if (n >= p.ExpansionSteps) {
      errors.overflow(start, s"exceeded $n expansion steps")
    }
    
    if (total.duration >= p.ExpansionTimeout) {
      errors.overflow(start, s"exceeded $total execution time")
    }
    
    debug("done expanding to " + out)

    val times = runtime.toMap + (this -> total)
    val counters: Map[String, Int] = Map(
      "#expansions" -> n,
      "#maxExpansions" -> p.ExpansionSteps,
      "#graphNodes" -> out.numNodes,
      "#graphEdges" -> out.numEdges)

    (out, times, counters, depth.toMap)
  }
  override def toString = "all"
}

/** Locate data producer in the same method call */
case class LocalSlicer(implicit errors: Errors) extends SliceRule {
  override def toString = "local"
  override def expand = {
    case s @ Seed(e, o) if o.isObject && e.uses(o) =>
      implicit val c = e.c

      val local = Before(e.counter) && (e.parent match {
        case Some(parent) => ParentIs(parent)
        case None         => Thread(e.thread)
      })

      val produced = o.read || o.aread || o.constructed || o.exited

      (local && produced)(c).lastOption match {
        case Some(d: Enter) if o.exited(d) =>
          if (c.meta.containers(d.method.declarer))
            Seed(d, o) :: Nil
          else
            Seed(d.exit.get, o) :: Nil
        case Some(d) =>
          Seed(d, o) :: Nil
        case None =>
          (e, e.parent) match {
            case (e, Some(d)) if d.participants.contains(o) =>
              Seed(d, o) :: Nil
            case (e: Enter, _) if e.receiver == Some(o) && e.isConstructor =>
              Nil
            case _ if o.typ.isArray || o.typ == c.meta.StringClass =>
              Nil
            case (_, Some(d)) =>
              // d.c.filter(d.children).size == 1
              errors.slicing(s)
              Seed(d, Unknown) :: Nil
            case (e, None) =>
              Nil
          }
      }
  }
}

case class ParameterSlicer(m: Method)(implicit val errors: Errors) extends SliceRule {
  override def toString = "hack"
  override def expand = {
    case Seed(e: Exit, o) if o == e.value && e.method == m =>
      for (p <- e.parent.toList; o <- p.objectArgs)
        yield Seed(p, o)
  }
}

case class CoverSlicer(b: FrameworkBoundary)(implicit val errors: Errors) extends SliceRule {
  override def toString = "cover"
  override def expand = {
    case Seed(e, _) =>
      b.parentCover(e) match {
        case Some(d) if d != e =>
          d.seeds
        case Some(d) if d == e =>
          d.parent match {
            case Some(f) => f.seeds
            case None    => Nil
          }
        case None => Nil
      }
  }
}

/** Captures dependencies via static fields */
case class StaticSlicer(implicit errors: Errors) extends SliceRule {
  override def toString = "static"
  override def expand = {
    case Seed(e: Read, o) if e.isStatic && o.isObject && o.value(e) =>
      (Before(e.counter) && Write && e.field.member)(e.c).lastOption match {
        case Some(d) => d.seeds
        case None    => Nil
      }
  }
}

/** Captures dependencies via field read-writes */
case class HeapSlicer(implicit errors: Errors) extends SliceRule {
  override def toString = "heap"
  override def expand = {
    case Seed(e: Read, o) if !e.isStatic && o.isObject && o.value(e) =>
      e.c.heaps.outbound(e.receiver.get).find {
        case (_, l) => l.field == e.field && l.interval.contains(e.counter)
      } match {
        case Some((to, HeapLabel(_, Segment(x, _)))) =>
          assert(to == e.value, "read different object from the write in heap series")
          Seed(e, e.receiver.get) :: e.c.at(x).seeds
        case None =>
          Seed(e, e.receiver.get) :: Nil
      }
  }

}
