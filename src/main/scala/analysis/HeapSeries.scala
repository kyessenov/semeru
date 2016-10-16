package edu.mit.csail.cap.query
package analysis

import util._

/** Label in the heap graph. */
trait Label {
  def field: Field
  def interval: Interval
}

/** Label in the heap series. */
case class HeapLabel(field: Field, interval: Segment) extends Label {
  override def toString = "" + field + interval
}

case class NormalLabel(field: Field, interval: NormalForm) extends Label {
  override def toString = "" + interval
}

/**
 * Heap series.
 *  there are possibly many labels for the same field between same objects
 *    (e.g. it's a multigraph)
 *  intervals for each field are monotonic over time (artifact of construction)
 *    (e.g. not intersecting)
 *  contains only object instances (class types and array types)
 *    not null and not unknown
 */
trait HeapSeries extends Graph[Object, HeapLabel] with Concretizer {
  /** Database of the heap. */
  def c: Connection

  /** Heap abstraction */
  def abstraction: HeapAbstraction

  /** Total duration of all heap edges. */
  lazy val interval = 
    if (c.size == 0)
      Segment(1, 2)
    else
      Segment(1, c.size + 1)

  /** Events assigning to fields of the object */
  def writes(o: Object): Traversable[Event] =
    for ((_, l) <- outbound(o))
      yield c.at(l.interval.low)

  /** Events assigning the object to fields */
  def stores(o: Object): Traversable[Event] =
    for ((_, l) <- inbound(o))
      yield c.at(l.interval.low)

  /**
   * Extracts a heap at particular point in time.
   */
  def sample(t: Int) = {
    val out = new Digraph[Object, Field]
    for (Edge(from, l, to) <- this)
      if (l.interval.contains(t))
        out.add(from, l.field, to)
    out
  }

  override def concretize(from: Object, to: Option[Object], candidate: Path[Cluster], avoid: Edge[Object, Label] => Boolean, during: Interval) = {
    assert(c.typeOf(from) == candidate.from.typ)
    assert(has(from))

    val time = Intersection(during, candidate.life).normalize
    val nodes = candidate.nodes

    debug("concretizing from " + from)
    for (cluster <- nodes)
      debug(" " + cluster)

    var furthest = nodes

    def helper(rest: List[Cluster], cur: Path[Object]): Option[Path[Object]] = {
      if (rest.size < furthest.size)
        furthest = rest

      rest match {
        case Nil =>
          to match {
            case Some(o) if o == cur.to => Some(cur)
            case None                   => Some(cur)
            case _                      => None
          }
        case head :: tail =>
          head match {
            case InstanceCluster(o, t) =>
              for (
                (to, l) <- outbound(cur.to);
                if !avoid(Edge(cur.to, l, to));
                if to == o;
                path = Extend(cur, l, to);
                if path.isSimple && !Intersection(path.life, time).isEmpty
              ) return helper(tail, path)
              return None
            case TypeCluster(t) =>
              // order is important because of typeOf
              for (
                (to, l) <- outbound(cur.to);
                if !avoid(Edge(cur.to, l, to));
                path = Extend(cur, l, to);
                if path.isSimple && !Intersection(path.life, time).isEmpty;
                if c.typeOf(to) == t
              ) helper(tail, path) match {
                case Some(path) => return Some(path)
                case _          =>
              }
              return None
            case _ => None
          }
      }
    }

    // run-time checks and logging
    helper(nodes.tail, Start(from)) match {
      case Some(path) =>
        assert(path.from == from)
        assert(path.isSimple)
        assert(path.isViable && !Intersection(path.life, time).isEmpty)
        debug("done " + path)
        Concrete(path)
      case _ =>
        furthest match {
          case Nil =>
            debug("path concretized but to a wrong end note")
            Failure
          case _ =>
            // we blocked at the first element of farthest
            val prefix = nodes.take(nodes.size - furthest.size + 1)
            debug("unsatisfiable prefix of length " + prefix.size +
              " / " + nodes.size)
            FailedPrefix(prefix)
        }
    }
  }

  override def concretize(t: Type) =
    // Alternative way: o <- nodes filter {o => connection.typeOf(o) == t}
    c.objects(t).filter(o => this.has(o))
}

class HeapSeriesImpl(val c: Connection) extends MultiDigraph[Object, HeapLabel] with HeapSeries {
  var abstraction: HeapAbstraction = _

  /** Add a link to the global heap series */
  def add(field: Field, from: Object, low: Int, high: Int, to: Object) {
    if (from.isObject && to.isObject)
      add(from, HeapLabel(field, Segment(low, high)), to)
  }
}

case class FieldRef(field: Field, start: Int)

object HeapSeries {
  def build(c: Connection): HeapSeries = {
    info("building heap series for " + c)
    val hs = new HeapSeriesImpl(c)
    build(hs)
    c.meta.containers.build(hs)
    assert(!hs.has(Null) && !hs.has(Unknown), "heaps contain only objects")
    hs.abstraction = HeapAbstraction.build(hs)
    hs
  }

  /**
   * Field connectivity.
   */
  def build(hs: HeapSeriesImpl) {
    debug("building field abstraction")

    // active heap: field and creation time
    val active = new MultiDigraph[Object, FieldRef]

    hs.c.select(Write && !Null.receiver).foreach {
      case e: Write =>
        val from = e.receiver.get
        // remove active reference
        active.remove(from, f = _.field == e.field) match {
          case (to, FieldRef(_, start)) :: Nil =>
            // add to heap
            hs.add(from, HeapLabel(e.field, Segment(start, e.counter)), to)
          case Nil =>
          case _   => assert(false, "at most one active value for each object-field")
        }

        // add to active
        if (e.value.isObject)
          active.add(from, FieldRef(e.field, e.counter), e.value)
      case _ => assert(false)
    }

    debug("transferring active fields " + active)

    // add remaining active connections
    for (Edge(from, FieldRef(field, start), to) <- active)
      hs.add(from, HeapLabel(field, Segment(start, hs.interval.high)), to)

    debug("done field heap " + hs)
  }
}
