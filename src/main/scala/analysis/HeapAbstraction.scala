package edu.mit.csail.cap.query
package analysis

import util._

/**
 * Cluster of objects of the same type.
 */
sealed trait Cluster {
  def typ: Type
}

case class InstanceCluster(o: Object, c: Connection) extends Cluster {
  assert(o != Unknown, "must not contain unknown object")
  override def toString = o.toString
  lazy val typ = c.typeOf(o)
}

case class TypeCluster(typ: Type) extends Cluster {
  override def toString = "{" + typ + "}"
}

trait HeapAbstraction extends Graph[Cluster, NormalForm] {
  def c: Connection
  def interval: Interval

  /** Extracts a heap snapshot at given interval. */
  def sample(t: Interval) = {
    val out = new HeapAbstractionImpl(c, t)
    for (Edge(from, int, to) <- this) {
      val active = Intersection(int, t).normalize
      if (!active.isEmpty)
        out.add(from, active, to)
    }
    out
  }

  /** Clustering of objects **/
  def cluster(o: Object): Option[Cluster] =
    c.typeOf(o) match {
      case NullType => None
      case UnknownType => None
      case _: PrimitiveType => None
      case a: ArrayType if a.baseType.isInstanceOf[PrimitiveType] => None
      // Manual sink classes
      case t if t == c.meta.StringClass => None
      case t if t == c.meta.ObjectClass => None
      // Special handling of containers
      case t if c.meta.containers(t) =>
        Some(InstanceCluster(o, c))
      // Regular treatment
      case t => Some(TypeCluster(t))
    }

  override def toString = {
    val sb = new StringBuilder
    sb ++= super.toString
    val classTypes = nodes.count { case TypeCluster(_: ClassType) => true; case _ => false }
    val arrayTypes = nodes.count { case TypeCluster(_: ArrayType) => true; case _ => false }
    val instanceTypes = nodes.count { case _: InstanceCluster => true; case _ => false }
    sb ++= " class types: " + classTypes
    sb ++= " array types: " + arrayTypes
    sb ++= " instance types: " + instanceTypes
    sb.toString
  }
}

class HeapAbstractionImpl(val c: Connection, val interval: Interval) extends Digraph[Cluster, NormalForm] with HeapAbstraction {
  def this(c: Connection) = this(c, if (c.size == 0) Segment(1, 2) else Segment(1, c.size + 1))
}

/** Heap abstraction.  */
object HeapAbstraction {
  var THRESHOLD = 10
  var RATE = 128 * 1024

  /** Approximate an interval by scaling it down with the given rate. */
  def bound(r: Segment, rate: Int): Segment =
    Segment(math.floor(r.low.toDouble / rate).toInt * rate,
      math.ceil(r.high.toDouble / rate).toInt * rate)

  /** Builds a heap abstraction. */
  def build(heap: HeapSeries): HeapAbstraction = {
    info("building heap abstraction for " + heap)

    val out = new HeapAbstractionImpl(heap.c)

    heap.c.forceLoadTypes
    for (Edge(from, HeapLabel(_, int), to) <- heap)
      (out.cluster(from), out.cluster(to)) match {
        case (Some(a), Some(b)) if (a != b) =>
          out.get(a, b) match {
            case existing :: Nil =>
              if (existing.segments.size > THRESHOLD)
                out.add(a, Union(bound(int, RATE), existing).normalize, b)
              else
                out.add(a, Union(int, existing).normalize, b)
            case Nil =>
              out.add(a, int.normalize, b)
            case _ =>
              assert(false)
          }
        case _ =>
      }
    heap.c.forceClearTypes

    debug("done " + out)
    out
  }
}

/** Result of concretization. */
sealed abstract class Concretization

/** Successfully found concrete path. */
case class Concrete(path: Path[Object]) extends Concretization

/** The smallest prefix that is not concretizable. */
case class FailedPrefix(prefix: List[Cluster]) extends Concretization

/** Failure to concretize. */
object Failure extends Concretization

trait Concretizer {
  /** Concretizes a candidate path under constraints. */
  def concretize(from: Option[Object],
                 to: Option[Object],
                 candidate: Path[Cluster],
                 avoid: Edge[Object, Label] => Boolean,
                 during: Interval): Concretization =
    from match {
      case Some(o) => concretize(o, to, candidate, avoid, during)
      case None =>
        candidate.from match {
          case InstanceCluster(o, _) => concretize(o, to, candidate, avoid, during)
          case TypeCluster(t) =>
            for (o <- concretize(t))
              concretize(o, to, candidate, avoid, during) match {
                case p: Concrete => return p
                case _           =>
              }
            return Failure
        }
    }

  /** Concretizes a type to objects. */
  def concretize(t: Type): Traversable[Object]

  /** Concretizes a candidate path under constraints given the source. */
  def concretize(from: Object,
                 to: Option[Object],
                 candidate: Path[Cluster],
                 avoid: Edge[Object, Label] => Boolean,
                 during: Interval): Concretization
}

