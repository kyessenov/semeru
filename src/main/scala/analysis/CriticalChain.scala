package edu.mit.csail.cap.query
package analysis

import scala.collection.mutable
import util._

object CriticalChain {
  var PATH_BOUND = 20
  var TIME_BOUND = 30 * 1000
  var QUEUE_CLEANUP = true
}

/**
 * Critical chain definitions:
 *
 * Def: given g:Graph[Object, Label], o1 and o2 in Object
 * a chain is a simple viable path in g from o1 to o2.
 *
 * Def: life of a chain is the duration for which chain existed
 * (i.e. intersection of label intervals of path segments).
 *
 * Def: critical event for a chain is the moment when the chain becomes viable
 * it's the first event in its life.
 *
 * Def: critical chain is a chain with earliest critical event; if there are chains
 * between two objects then there is a critical chain between them.
 *
 * Def: if for every point in time, at most one new connection is established (label with an opening interval)
 * then all critical chains share the same label with the critical event. (rephrase)
 *
 * Def: complete critical chain is a critical chain such that any sub-chain is a critical chain between
 * its endpoints; its existence can be shown by construction.
 */
trait CriticalChain extends BoundaryAnalysis {
  import CriticalChain._

  /**
   * Find a path given the concretizer, time view of the graph, and
   * the end-point clusters.
   *
   * Uses BFS-expansion.
   */
  private def allPathsBFS(start: Cluster, end: Cluster, during: Interval, skip: Set[Type])(concretizer: (Path[Cluster], Interval) => Concretization): Option[Path[Object]] = {
    if (!c.heaps.abstraction.has(start) || !c.heaps.abstraction.has(end))
      return None

    debug("expanding from " + start + " to " + end)
    val abstraction = c.heaps.abstraction.sample(during).reachable(start, end)
    debug("abstraction " + abstraction)
    val current = System.currentTimeMillis

    val q = new mutable.Queue[Path[Cluster]]
    q.enqueue(Start(start))

    while (!q.isEmpty) {
      val cur = q.dequeue

      if (System.currentTimeMillis - current > TIME_BOUND) {
        warn("time-out: exceeded " + TIME_BOUND)
        info("queue size: " + q.size)
        info("last path seen: " + cur)
        return None
      }

      if (cur.to == end)
        concretizer(cur, during) match {
          case Concrete(path) =>
            return Some(path)
          case FailedPrefix(prefix) if QUEUE_CLEANUP =>
            // it's ok to remove just using prefixes since the graph is simple 
            // and has fixed times on edges
            val cleanup = q.dequeueAll(p => p.nodes startsWith prefix)
            debug("removed " + cleanup.size + " from queue")
          case _ => // don't extend it
        }
      else {
        val next = abstraction.outbound(cur.to)
        val sorted = next.toList
          .map(e => (e, abstraction.outbound(e._1).size))
          .sortBy(_._2)
          .map(_._1)
        val dummy = c.meta.dummy
        for (
          (to, int) <- sorted;
          if !skip.contains(to.typ);
          path = Extend(cur, NormalLabel(dummy, int.normalize), to);
          if path.length <= PATH_BOUND;
          if path.isSimple && path.isViable
        ) q.enqueue(path)
      }
    }

    return None
  }

  /**
   * Gives a chain between objects of the given types.
   */
  def chain(from: Type,
            to: Type,
            during: Interval = Forever,
            skip: Set[Type] = Set(),
            avoid: Edge[Object, Label] => Boolean = (_ => false)): Option[Path[Object]] =
    allPathsBFS(TypeCluster(from), TypeCluster(to), during, skip) {
      heaps.concretize(None, None, _, avoid, _)
    }

  /**
   * Compute the critical chain between two objects given a chain between them.
   */
  def criticalChain(path: Path[Object],
                    during: Interval = Forever,
                    skip: Set[Type] = Set(),
                    avoid: Edge[Object, Label] => Boolean = (_ => false),
                    keepStart: Boolean = true,
                    keepEnd: Boolean = true): Path[Object] = {
    assert(path.isViable)
    assert(path.length > 1)

    val before = BeforeInt(path.critical) intersect during
    val start = TypeCluster(c.typeOf(path.from))
    val end = TypeCluster(c.typeOf(path.to))

    allPathsBFS(start, end, before, skip) {
      val cstart = if (keepStart) Some(path.from) else None
      val cend = if (keepEnd) Some(path.to) else None
      heaps.concretize(cstart, cend, _, avoid, _)
    } match {
      case Some(path) =>
        // fixed-point
        criticalChain(path, during, skip, avoid, keepStart, keepEnd)
      case _ => path
    }
  }

  /**
   * Compute the next chain between same objects given a chain between them.
   */
  def nextChain(path: Path[Object],
                during: Interval = Forever,
                skip: Set[Type] = Set(),
                avoid: Edge[Object, Label] => Boolean = (_ => false)): Option[Path[Object]] = {
    assert(path.isViable)
    assert(path.length > 1)

    val after = AfterInt(path.critical) intersect during
    val start = TypeCluster(c.typeOf(path.from))
    val end = TypeCluster(c.typeOf(path.to))

    allPathsBFS(start, end, after, skip) {
      heaps.concretize(Some(path.from), Some(path.to), _, avoid, _)
    }
  }

  /** Boundary-aware functions. Search for all subtypes of the given types */
  def matchMake(from: ClassType, to: ClassType, BUDGET: Int = 1, AVOID_USER: Boolean = true): List[Path[Object]] = {
    var out: List[Path[Object]] = Nil

    def skipUserFields(e: Edge[Object, Label]) =
      if (AVOID_USER)
        b.isUser(e.l.field.declarer)
      else
        false

    for (
      s <- from.allSubtypes + from;
      if b.isUser(s);
      t <- to.allSubtypes + to;
      if b.isUser(t);
      if out.size < BUDGET;
      c <- chain(s, t, avoid = skipUserFields)
    ) out = c :: out

    out
  }
}
