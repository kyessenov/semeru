package edu.mit.csail.cap.query
package analysis

import util._

case class Containers(meta: Metadata) {
  val collection = CollectionAbstraction(this)
  val iterator = IteratorAbstraction(this)
  val map = MapAbstraction(this)
  val array = ArrayAbstraction(this)
  val all = collection :: iterator :: map :: array :: Nil
  def apply(t: Type) = all.exists(_.types(t))

  /** Container connectivity. */
  def build(implicit hs: HeapSeriesImpl) {
    debug(s"building container abstractions: ${all.mkString(", ")}")

    for (abs <- all)
      abs.start()

    var i = 0
    for (
      e <- hs.c.select(Or(all.map(_.writes)));
      abs <- all if abs.writes(e)
    ) {
      i = i + 1
      debug(i % 1000000 == 0, s"processed $i events")
      abs.apply(e)
    }

    debug("transferring final state to heap")
    for (abs <- all)
      abs.finish(hs)

    debug("finished constructing container abstractions")
  }
  
  def errors = meta.errors
}

/**
 * A container abstraction models behavior of collections
 * (lists, maps, arrays) as a binary relation
 * (heap graph with the given field)
 *
 * It is an approximation of the actual containment since
 * not everything can be deduced from the input and output values
 * for the calls.
 *
 * It is useful in cases where the container is declared a library
 * and the heap picture is incomplete.
 *
 * It is important that self-calls
 * are not recorded for the container classes.
 */
trait ContainerAbstraction extends SliceRule {
  def all: Containers
  def meta = all.meta

  /** Abstract heap field */
  def field: Field

  /** Writes to the abstract field(s) */
  def writes: Query

  /** Check if type is a container */
  def types(t: Type): Boolean

  /** Events establishing heap connection before read */
  def writes(from: Object, to: Object => Boolean, before: Event): Traversable[Event] =
    for (
      (o, l) <- before.c.heaps.outbound(from) if l.field == field && to(o) if l.interval.contains(before.counter - 1)
    ) yield before.c.at(l.interval.low)

  override def errors = all.errors
    
  override def toString = field.name

  /**
   *  Construction protocol:
   *  1) start
   *  2) for every write event apply in order
   *  3) finish
   */
  def start()

  /** Apply an abstract write (or do nothing) */
  def apply(e: Event)(implicit hs: HeapSeriesImpl)

  def finish(hs: HeapSeriesImpl)
}

/**
 * Container abstractions rely on observations to deduce success of addition
 * or removal from containers.
 */
trait JavaUtilAbstraction extends ContainerAbstraction {
  val JAVA = PrefixMask("java.util.")
  def OP: Map[String, Set[Method]]
  override def writes = Enter && Or(OP.values.flatten.map(_.member).toList)
  override def types(t: Type) = t match {
    case t: ClassType if JAVA(t) && t.isSubtype(field.declarer) => true
    case _ => false
  }

  override def expand = {
    case Seed(e: Enter, o) if o.isObject && types(e.method.declarer) =>
      Seed(e, e.receiver.get) ::
        { for (d <- writes(e.receiver.get, Set(o), e)) yield Seed(d, o) }.toList
  }

  /** Move all active fields to heap series, include event itself */
  def clear(op: Enter, from: Object, to: Object => Boolean = _ => true)(implicit hs: HeapSeriesImpl) {
    for ((x, start) <- heap.remove(from, to))
      hs.add(field, from, start, op.counter, x)
  }
  /** Copy all outgoing fields */
  def copy(op: Enter, from: Object, to: Object) {
    for ((x, _) <- heap.outbound(from))
      heap.add(to, op.counter, x)
  }

  /** Current state of the container heap */
  var heap: MultiDigraph[Object, Int] = null

  override def start() {
    this.heap = new MultiDigraph[Object, Int]
  }

  override def finish(hs: HeapSeriesImpl) {
    for (Edge(from, ref, to) <- heap)
      hs.add(field, from, ref, hs.interval.high, to)

    this.heap = null
  }
}

/** Root interface for collection types */
case class CollectionAbstraction(all: Containers) extends JavaUtilAbstraction {
  import all.meta._

  override lazy val field =
    Field("$contents", 18, CollectionClass, synthetic = true)

  override lazy val OP = Map(
    "add" -> CollectionClass.method("add", ObjectClass).overriding(JAVA),
    "addAll" -> CollectionClass.method("addAll", CollectionClass).overriding(JAVA),
    "remove" -> CollectionClass.method("remove", ObjectClass).overriding(JAVA),
    "removeAll" -> CollectionClass.method("removeAll", CollectionClass).overriding(JAVA),
    "retainAll" -> CollectionClass.method("retainAll", CollectionClass).overriding(JAVA),
    "clear" -> CollectionClass.method("clear").overriding(JAVA),
    "toArray" -> (
      CollectionClass.method("toArray").overriding(JAVA) ++
      CollectionClass.method("toArray", ArrayType(ObjectClass)).overriding(JAVA)),
    "iterator" -> (
      CollectionClass.method("iterator").overriding(JAVA) ++
      ListClass.method("listIterator").overriding(JAVA) ++
      ListClass.method("listIterator", IntType).overriding(JAVA)),
    "clone" -> ObjectClass.method("clone").overriding(JAVA).filter(m => CollectionClass.allSubtypes(m.declarer)),
    // List
    "addIdx" -> ListClass.method("add", IntType, ObjectClass).overriding(JAVA),
    "addAllIdx" -> ListClass.method("addAll", IntType, CollectionClass).overriding(JAVA),
    "removeIdx" -> ListClass.method("remove", IntType).overriding(JAVA),
    "set" -> ListClass.method("set", IntType, ObjectClass).overriding(JAVA),
    // ArrayList
    "init" -> Set(
      ArrayListClass.method("<init>", CollectionClass),
      HashSetClass.method("<init>", CollectionClass),
      LinkedHashSetClass.method("<init>", CollectionClass),
      TreeSetClass.method("<init>", CollectionClass)),
    "removeRange" -> Set(ArrayListClass.method("removeRange", IntType, IntType)))

  override def apply(e: Event)(implicit hs: HeapSeriesImpl) = e match {
    case e: Enter if e.returns.isDefined =>
      val m = e.method
      val r = e.receiver.get
      val v = e.returns.get
      if (OP("add")(m) && v == BooleanType.True)
        heap.add(r, e.counter, e.params2(0))
      else if (OP("addIdx")(m))
        heap.add(r, e.counter, e.params2(1))
      else if (OP("addAll")(m) || OP("init")(m))
        copy(e, e.params2(0), r)
      else if (OP("addAllIdx")(m))
        copy(e, e.params2(1), r)
      else if (OP("remove")(m) && v == BooleanType.True)
        clear(e, r, Set(e.params2(0)))
      else if (OP("removeIdx")(m))
        clear(e, r, Set(v))
      else if (OP("clear")(m) || OP("removeAll")(m) || OP("retainAll")(m) || OP("removeRange")(m))
        clear(e, r)
      else if (OP("set")(m)) {
        clear(e, r, Set(v))
        heap.add(r, e.counter, e.params2(1))
      } else if (OP("clone")(m))
        copy(e, r, v)
      else if (OP("iterator")(m))
        all.iterator.heap.add(v, e.counter, r)
      else if (OP("toArray")(m))
        for ((to, _) <- heap.outbound(r))
          all.array.write(v, e.counter, to)
    case _ =>
  }
}

case class IteratorAbstraction(all: Containers) extends JavaUtilAbstraction {
  // points to collection
  override lazy val field = Field("$iterator", 22, meta.IteratorClass, synthetic = true)
  override val OP = Map[String, Set[Method]]()
  override def apply(e: Event)(implicit hs: HeapSeriesImpl) {}
  override def expand = {
    case Seed(e: Enter, o) if o.isObject && types(e.method.declarer) =>
      Seed(e, e.receiver.get) ::
        { for (d <- writes(e.receiver.get, Function.const(true), e)) yield Seed(d, o) }.toList
  }
}

case class MapAbstraction(all: Containers) extends JavaUtilAbstraction {
  import all.meta._
  override lazy val field = Field("$values", 19, MapClass, synthetic = true)
  override lazy val OP: Map[String, Set[Method]] = Map(
    "put" -> MapClass.method("put", ObjectClass, ObjectClass).overriding(JAVA),
    "putAll" -> MapClass.method("putAll", MapClass).overriding(JAVA),
    "remove" -> MapClass.method("remove", ObjectClass).overriding(JAVA),
    "clear" -> MapClass.method("clear").overriding(JAVA),
    "clone" -> ObjectClass.method("clone").overriding(JAVA).filter(m => MapClass.allSubtypes(m.declarer)),
    "init" -> Set(
      HashMapClass.method("<init>", MapClass),
      TreeMapClass.method("<init>", MapClass)))

  override def apply(e: Event)(implicit hs: HeapSeriesImpl) = e match {
    case e: Enter if e.returns.isDefined =>
      val m = e.method
      val r = e.receiver.get
      val v = e.returns.get
      if (OP("put")(m)) {
        clear(e, r, Set(v))
        heap.add(r, e.counter, e.params2(1))
      } else if (OP("putAll")(m) || OP("init")(m)) {
        clear(e, r)
        for ((to, _) <- heap.outbound(e.params2(0)))
          heap.add(r, e.counter, to)
      } else if (OP("remove")(m))
        clear(e, r, Set(v))
      else if (OP("clear")(m))
        clear(e, r)
      else if (OP("clone")(m))
        copy(e, r, v)
    case _ =>
  }
}

/**
 * Precise modeling of arrays
 * (except for skipped instructions inside library/excluded classes)
 *
 * Index -1 is for unknown values.
 */
case class ArrayAbstraction(all: Containers) extends ContainerAbstraction {
  import scala.collection.mutable

  override lazy val field = Field("$array", 20, meta.ObjectClass, synthetic = true)

  private lazy val arraycopy =
    meta.method(-5829065086251241999L)

  override def writes =
    ArrayWrite || arraycopy.member

  override def types(t: Type) = t match {
    case _: ArrayType => true
    case _            => false
  }

  type ArrayRecord = mutable.HashMap[Int, (Int, Object)]

  /** From object, index, start counter, to object */
  private var heap = new mutable.HashMap[Object, ArrayRecord]

  /** From object, start counter, to object */
  private var stale = new mutable.HashMap[Object, List[(Int, Object)]].withDefaultValue(Nil)

  override def start() {
    heap.clear()
    stale.clear()
  }

  /** Auto-insert array record if not present */
  private def array(from: Object): ArrayRecord = heap.get(from) match {
    case Some(array) => array
    case None =>
      val out = new ArrayRecord
      heap(from) = out
      out
  }

  /** Write to an array */
  private def write(from: Object,
                    index: Int,
                    counter: Int,
                    value: Object)(
                      implicit hs: HeapSeriesImpl,
                      a: ArrayRecord = array(from)) {
    // update known heap
    a.put(index, (counter, value)) match {
      case Some((start, to)) => hs.add(field, from, start, counter, to)
      case _                 =>
    }

    // update unknown heap
    stale.put(from, Nil) match {
      case Some(old) =>
        for ((start, to) <- old)
          hs.add(field, from, start, counter, to)
      case _ =>
    }
  }

  def write(from: Object, counter: Int, to: Object) {
    stale(from) ::= (counter, to)
  }

  override def apply(e: Event)(implicit hs: HeapSeriesImpl) = e match {
    case e: ArrayWrite =>
      write(e.receiver, e.index, e.counter, e.value)

    case e: Enter if e.method == arraycopy =>
      assert(e.method == arraycopy)

      val List(src, srcPos, dest, destPos, length) = e.arguments

      val a = array(src)
      val b = array(dest)

      for (
        (index, (start, to)) <- a;
        if srcPos <= index && index < srcPos + length
      ) write(dest, index - srcPos + destPos, e.counter, to)(hs, b)

    case _ =>
  }

  override def finish(hs: HeapSeriesImpl) {
    val counter = hs.interval.high

    debug(s"transferring heap of ${heap.size} arrays")
    for (
      (from, a) <- heap;
      (index, (start, to)) <- a
    ) hs.add(field, from, start, counter, to)

    debug(s"transferring ${stale.size} arrays")
    for (
      (from, old) <- stale;
      (start, to) <- old
    ) hs.add(field, from, start, counter, to)

    start()
  }

  override def expand = {
    case Seed(e: ArrayRead, o) if o.isObject && e.value == o =>
      Seed(e, e.receiver) ::
        writes(e.receiver, Set(o), e).toList.flatMap(_.seeds)
  }
}
