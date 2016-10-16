package edu.mit.csail.cap

import scala.collection.mutable
import wire.VMValue

package object query extends util.Logger {
  /** Object reference */
  type Object = Int

  /** Package name */
  type Package = String

  val Null = VMValue.NULL
  val Unknown = VMValue.UNKNOWN
  val FieldEvent = Read || Write
  val CallEvent = Enter || Exit || Exception
  val ArrayEvent = ArrayRead || ArrayWrite
  val Kinds = Enter ::
    Exit ::
    Write ::
    Exception ::
    Read ::
    ArrayRead ::
    ArrayWrite ::
    Nil

  implicit class RichObject(val o: Object) extends AnyVal {
    /** Check if the reference is a special null value */
    def isNull = o == Null

    /** Check if the reference is a special missing value */
    def isUnknown = o == Unknown

    /** Check if the reference is neither null nor missing */
    def isObject = !isNull && !isUnknown

    /** Object is the receiver */
    def receiver = ReceiverIs(o)

    /** Object is the value */
    def value = ValueIs(o)

    /** Object is produced by a field read */
    def read = Read && o.value

    /** Object is produced by an array read */
    def aread = ArrayRead && o.value

    /** Object is returned by a call */
    def returned = Exit && o.value

    /** Object is returned by a call */
    def exited = Enter && o.value

    /** Invocations on an object */
    def invoked = Enter && o.receiver

    /** Objects is argument to a call (may match primitive values) */
    def argument = ArgumentIs(o)

    /** Object is instantiated (using dynamic type constructor method ID) */
    def constructed(implicit t: Trace) =
      Enter && o.receiver && Or {
        o.typ match {
          case ct: ClassType => ct.constructors.map(_.member).toList
          case _             => Nil
        }
      }

    /** Retrieve type of a reference */
    def typ(implicit t: Trace) = t.typeOf(o)

    /** Writes to fields of the object */
    def writes(implicit t: Trace) = t.heaps.writes(o)

    /** Writes of the object to fields */
    def stores(implicit t: Trace) = t.heaps.stores(o)
  }

  trait Trace extends Traversable[Event] with Statistics {
    /** Database connection */
    def c: Connection

    /** Meta data information */
    def meta: Metadata = c.meta

    /** Heap graph */
    def heaps: analysis.HeapSeries = c.heaps

    /** Type of an object, or unknown */
    def typeOf(o: Object): Type = c.typeOf(o)

    /** Objects of the given type */
    def objects(t: Type): List[Object] = c.objects(t)
    def objects[T <: Type](ts: Set[T]): Set[Object] =
      for (t <- ts; o <- objects(t)) yield o

    /** Apply a filter to the trace */
    def select(f: Query): Trace

    /** List of events ordered by ID */
    def events: List[Event]

    /** Apply a function to every event in the order of ID */
    def foreach[U](f: Event => U)

    /** Trace size */
    def size: Int

    /** First event (requires non-empty) */
    def head: Event

    /** First event */
    override def headOption: Option[Event] =
      if (isEmpty) None else Some(head)

    /** Last event (requires non-empty) */
    def last: Event

    /** Last event */
    override def lastOption: Option[Event] =
      if (isEmpty) None else Some(last)

    /** Take first events */
    def take(n: Int): Traversable[Event]

    /** Test whether the trace is empty */
    override def isEmpty = size == 0

    /** Create sub-traces between events (assuming they are sorted) */
    def split(es: List[Event]): List[Trace] =
      for (i <- (0 until es.size - 1).toList)
        yield select(After(es(i).counter + 1) && Before(es(i + 1).counter))

    /** Slice a section of the trace by ID */
    def between(from: Int, to: Int) = select(After(from) && Before(to))

    /** Event by ID */
    def at(t: Int): Event

    /** Set of thread IDs in the trace */
    def threadIDs: Set[Long]

    /** Set of traces for each thread ID in the trace */
    def threads: Set[Trace] = for (t <- threadIDs) yield select(Thread(t))

    /** Enter events */
    def enters = select(Enter)

    /** Methods */
    def methods: Set[Method]

    /** Field read and write events */
    def field = select(FieldEvent)

    /** Array read and write events */
    def array = select(ArrayEvent)

    /** Load the entire trace */
    def fetch = ConcTrace(c, events)

    /** Enter events returning, invoking on, or passing as a parameter the object */
    def history(o: Object) = select(o.exited || o.invoked || o.argument)

    /** Select by member */
    def member[M <: Member](ms: Traversable[M]): Trace =
      select(Or(ms.toSet.map(MemberIs).toList))

    def member(m: Member): Trace = member(Set(m))

    /** Call trees */
    def calls: CallGraph = new CallGraph(this)

    /** Call tree trace */
    def trees: CallTrace = new CallTrace(c, calls.trees)

    def seeds: List[Seed] = events.flatMap(_.seeds)

    def traces = this :: Nil
  }

  /** Declarative trace */
  final case class DeclTrace(c: Connection, cond: Query = True) extends Trace {

    private def query(what: String = "*",
                      where: Query = cond,
                      order: Option[String] = Some("counter"),
                      limit: Option[Int] = None) =
      s"select $what from `LOG` where ${where.sql}" +
        { order match { case Some(s) => s" order by $s asc" case _ => "" } } +
        { limit match { case Some(i) => s" limit $i" case _ => "" } }

    override def foreach[U](f: Event => U) = c(query()) { rs => f(c.event(rs)) }

    override def select(f: Query): DeclTrace = DeclTrace(c, cond && f)

    override def events = c.read(query()) { c.event }

    override def at(t: Int) = select(At(t)).events.head

    override def size = c.readInt(query("count(*)"))

    override def head = c.at(c.readInt(query("min(counter)")))

    override def last = c.at(c.readInt(query("max(counter)")))

    override def take(n: Int) = c.read(query(limit = Some(n))) { c.event }

    override def threadIDs =
      c.read(query("distinct(thread)", order = None)) {
        _.getLong(1)
      }.toSet

    override def methods =
      c.read(query("distinct(id)", Enter && cond, order = None)) {
        rs => meta.method(rs.getLong(1))
      }.toSet
      
    override def toString = s"${c.name}(${cond})"
  }

  /** Concrete trace (list of events) */
  final case class ConcTrace(c: Connection, events: List[Event]) extends Trace {
    require((0 until events.size - 1).forall(i => events(i).counter < events(i + 1).counter), "unsorted events")
    require(events.forall(_.c == c), "must belong to same connection")

    def this(events: Traversable[Event]) = this(events.head.c, events.toList.sortBy(_.counter))

    override def select(f: Query) = ConcTrace(c, events.filter(f apply _))

    override def foreach[U](f: Event => U) = events.foreach(f)

    override def size = events.size

    override def head = events.head

    override def last = events.last

    override def take(n: Int) = events.take(n)

    override def at(t: Int) = events.find(_.counter == t).get

    override def threadIDs = events.map(_.thread).toSet

    override def toString = "Trace(" + events.size + " events)"

    override def methods = events.collect { case e: Enter => e.method }.toSet
  }

  /** Delegate trace */
  trait TraceProxy extends Trace {
    def t: Trace

    override def c = t.c

    override def foreach[U](f: Event => U) = t.foreach(f)

    override def select(f: Query) = t.select(f)

    override def events = t.events

    override def at(i: Int) = t.at(i)

    override def size = t.size

    override def head = t.head

    override def last = t.last

    override def take(n: Int) = t.take(n)

    override def threadIDs = t.threadIDs

    override def methods = t.methods
  }

  sealed trait Event extends Ordered[Event] {
    /** ID starting from 1 */
    def counter: Int

    /** Thread ID */
    def thread: Long

    /** Database connection */
    def c: Connection

    /**
     * Stack depth starts from 0. Stack depth may represent abstract stack wherein
     * we exclude frames from filtered classes.
     */
    def depth: Int

    /** Stack parent  */
    def caller: Option[Int]

    /** Stack parent event */
    def parent: Option[Enter] = caller match {
      case None    => None
      case Some(i) => Some(c.at(i).asInstanceOf[Enter])
    }

    /** Compute stack trace by following parent objects 
     (this is less efficient than stack trace query but works for incomplete traces */
    def parents: List[Event] = 
      this :: { parent match {
        case Some(e) => e.parents
        case None => Nil
        }
      }

    /** Field or method in the event  */
    def member: Option[Member] = this match {
      case e: Enter => Some(e.method)
      case e: Read  => Some(e.field)
      case e: Write => Some(e.field)
      case _        => None
    }

    /** Input objects to read from state (possibly null) */
    def uses: Set[Object] = this match {
      case e: Enter      => e.objectArgs.toSet ++ e.receiver
      case e: Exit       => e.objectValue.toSet
      case e: Exception  => Set(e.exception)
      case e: Write      => Set(e.value) ++ e.receiver
      case e: ArrayWrite => Set(e.value, e.receiver)
      case e: Read       => e.receiver.toSet
      case e: ArrayRead  => Set(e.receiver)
    }

    /** Output objects read from state (possibly null) */
    def produces: Set[Object] = this match {
      case e: Read      => Set(e.value)
      case e: ArrayRead => Set(e.value)
      case _            => Set()
    }

    /** Objects referenced in the event */
    def participants: Set[Object] = uses ++ produces

    /** Combine participants with the event */
    def seeds: List[Seed] = participants.toList match {
      case Nil => Seed(this, Unknown) :: Nil
      case l   => l.map(Seed(this, _))
    }

    /** Stack trace */
    def stack: Query = StackTrace(this)

    /** Direct descendants */
    def children: Query = this match {
      case e: Enter => ParentIs(e)
      case _        => False
    }

    /** Indirect descendants */
    def tree: Query = this match {
      case e: Enter =>
        Thread(thread) && After(counter - 1) && {
          e.successor match {
            case Some(succ) => Before(succ + 1)
            case _          => True
          }
        }
      case _ => At(counter)
    }

    override def toString = s"$counter|${caller.getOrElse(0)}|"

    override def compare(that: Event) = this.counter.compare(that.counter)

    override def hashCode = counter

    override def equals(that: Any) = that match {
      case that: Event =>
        this.c == that.c && this.counter == that.counter
      case _ => false
    }
  }

  final class Enter private[query] (
    /** Receiver object */
    val receiver: Option[Object],
    /** Method */
    val method: Method,
    /** Returned value for successfully exited methods */
    val returns: Option[Object],
    /** First two parameters (optimization) */
    val params2: List[Object],
    /** ID of the corresponding exit/exception event (or 0 if missing) */
    val successor: Option[Int],
    val thread: Long,
    val c: Connection,
    /** Observed stack depth */
    val depth: Int,
    val caller: Option[Int],
    val counter: Int)
      extends Event {
    def exit: Option[Event] = successor match {
      case None    => None
      case Some(i) => Some(c.at(i))
    }

    /** All arguments to the call */
    lazy val arguments: List[Object] =
      if (method.arguments <= 2)
        params2
      else
        c.read(s"select id from PARAMS where counter=$counter order by arg") { _.getInt("id") }

    /** Object arguments */
    def objectArgs: List[Object] =
      (arguments zip method.parameterTypes).collect {
        case (arg, _: InstanceType) => arg
      }

    /** Position of the object in the argument-return list */
    def position(o: Object) =
      if (Some(o) == receiver)
        Some(-1)
      else if (arguments.contains(o))
        Some(arguments.indexOf(o))
      else if (returns == Some(o))
        Some(-2)
      else
        None

    /** Trees during the duration of the call */
    def trees = c.between(counter, successor.getOrElse(c.size)).trees

    /** Indirect descendants */
    def contains = Thread(thread) && (successor match {
      case None    => After(counter + 1)
      case Some(i) => After(counter + 1) && Before(i + 1)
    })

    def isConstructor = method.isConstructor

    def isSuperConstructor = isConstructor && c.typeOf(receiver.get) != method.declarer

    override def toString =
      super.toString +
        (returns match { case Some(o) if method.returnType != VoidType => o + " <- " case _ => "" }) +
        receiver.getOrElse(method.declarer.name) + "." + method.name +
        "(" + params2.mkString(",") + (if (method.arguments > 2) "..." else "") + ")"
  }

  final class Exit private[query] (
    /** Corresponding enter method */
    val method: Method,
    /** Returns value */
    val value: Object,
    val thread: Long,
    val c: Connection,
    val depth: Int,
    val caller: Option[Int],
    val counter: Int)
      extends Event {
    def objectValue: Option[Object] = method.returnType match {
      case _: InstanceType => Some(value)
      case _               => None
    }

    override def toString =
      super.toString + "return " + value
  }

  final class Exception private[query] (
    /** Exception object */
    val exception: Object,
    val thread: Long,
    val c: Connection,
    val depth: Int,
    val caller: Option[Int],
    val counter: Int)
      extends Event {
    override def toString =
      super.toString + "throw " + exception
  }

  sealed trait FieldEvent extends Event {
    def receiver: Option[Object]
    def field: Field
    def value: Object
    def isStatic = field.isStatic
  }

  final class Read private[query] (
    /** Receiver object */
    val receiver: Option[Object],
    /** Field  */
    val field: Field,
    /** Reference value read from the field */
    val value: Object,
    val thread: Long,
    val c: Connection,
    val depth: Int,
    val caller: Option[Int],
    val counter: Int)
      extends FieldEvent {
    override def toString =
      super.toString + value + " <- " + receiver.getOrElse(field.declarer.name) + "." + field.name
  }

  final class Write private[query] (
    /** Receiver object */
    val receiver: Option[Object],
    /** Field */
    val field: Field,
    /** Reference value assigned to the field */
    val value: Object,
    val thread: Long,
    val c: Connection,
    val depth: Int,
    val caller: Option[Int],
    val counter: Int)
      extends FieldEvent {
    override def toString =
      super.toString + receiver.getOrElse(field.declarer.name) + "." + field.name + " <- " + value
  }

  sealed trait ArrayEvent extends Event {
    def receiver: Object
    def index: Int
    def value: Object
    def arrayType = c.typeOf(receiver)
  }

  final class ArrayRead private[query] (
    /** Array object */
    val receiver: Object,
    /** Index */
    val index: Int,
    /** Reference value read from the array */
    val value: Object,
    val thread: Long,
    val c: Connection,
    val depth: Int,
    val caller: Option[Int],
    val counter: Int)
      extends ArrayEvent {
    override def toString =
      super.toString + value + " <- " + receiver + "[" + index + "]"
  }

  final class ArrayWrite private[query] (
    /** Array object */
    val receiver: Object,
    /** Index */
    val index: Int,
    /** Reference value assigned to the array */
    val value: Object,
    val thread: Long,
    val c: Connection,
    val depth: Int,
    val caller: Option[Int],
    val counter: Int)
      extends ArrayEvent {
    override def toString =
      super.toString + receiver + "[" + index + "] <- " + value
  }

  /** Data o arrives to control location e. Use unknown for end-points. */
  case class Seed(e: Event, o: Object)

  /** Declarative event filter */
  sealed trait Query extends (Event => Boolean) {
    def sql: String
    def &&(that: Query): Query = And(this :: Nil) && that
    def ||(that: Query): Query = Or(this :: Nil) || that
    def unary_!(): Query = Not(this)
    def apply(implicit t: Trace) = t.select(this)
    override def toString = sql
  }

  /** Functional event filter, false by default */
  case class Predicate(f: PartialFunction[Event, Boolean]) extends Query {
    override def sql = ???
    override def apply(e: Event) = f.isDefinedAt(e) && f(e)
  }

  /** Event filter with an assumption */
  sealed trait Assuming extends Query {
    def assumption: Query
    def thenSql: String
    def thenApply(e: Event): Boolean
    override def sql = "(" + assumption.sql + ") and (" + thenSql + ")"
    override def apply(e: Event) = assumption(e) && thenApply(e)
  }

  object AssumptionError extends RuntimeException("assumption error")

  object True extends Query {
    override def sql = "true"
    override def apply(e: Event) = true
    override def &&(that: Query) = that
    override def ||(that: Query) = True
    override def unary_! = False
  }

  object False extends Query {
    override def sql = "false"
    override def apply(e: Event) = false
    override def &&(that: Query) = False
    override def ||(that: Query) = that
    override def unary_! = True
  }

  case class Not(a: Query) extends Query {
    override def sql = "(not (" + a.sql + "))"
    override def apply(e: Event) = !a(e)
  }

  case class And(a: List[Query]) extends Query {
    override def sql =
      if (a.isEmpty)
        "true"
      else
        "(" + a.map("(" + _.sql + ")").mkString(" and ") + ")"

    override def apply(e: Event) =
      a.map(_ apply e).foldLeft(true) { _ && _ }

    override def &&(that: Query) = that match {
      case True    => this
      case False   => False
      case And(a0) => And(a ::: a0)
      case that    => And(a :+ that)
    }

    override def toString = "And(" + a.map(_.toString).mkString(", ") + ")"
  }

  case class Or(a: List[Query]) extends Query {
    override def sql =
      if (a.isEmpty)
        "false"
      else
        "(" + a.map("(" + _.sql + ")").mkString(" or ") + ")"

    override def apply(e: Event) =
      a.map(_ apply e).foldLeft(false) { _ || _ }

    override def ||(that: Query) = that match {
      case False  => this
      case True   => True
      case Or(a0) => Or(a ::: a0)
      case that   => Or(a :+ that)
    }

    override def toString = "Or(" + a.map(_.toString).mkString(", ") + ")"
  }

  case class MemberIs(elt: Member) extends Query {
    override def sql = s"(id = ${elt.id} and ${
      elt match {
        case _: Method => Enter.sql
        case _: Field  => FieldEvent.sql
      }
    })"
    override def apply(e: Event) = e.member match {
      case Some(m) => m == elt
      case _       => false
    }
  }

  case class ReceiverIs(obj: Object) extends Assuming {
    override def assumption = Enter || FieldEvent || ArrayEvent
    override def thenSql = "receiver = " + obj
    override def thenApply(e: Event) = e match {
      case x: Read       => x.receiver.getOrElse(Null) == obj
      case x: Write      => x.receiver.getOrElse(Null) == obj
      case x: Enter      => x.receiver.getOrElse(Null) == obj
      case x: ArrayRead  => x.receiver == obj
      case x: ArrayWrite => x.receiver == obj
      case _             => throw AssumptionError
    }
  }

  case class ValueIs(obj: Object) extends Query {
    override def sql = "value = " + obj
    override def apply(e: Event) = e match {
      case x: Enter      => x.returns == Some(obj)
      case x: Exception  => false // x.exception == obj
      case x: Read       => x.value == obj
      case x: Write      => x.value == obj
      case x: Exit       => x.value == obj
      case x: ArrayRead  => x.value == obj
      case x: ArrayWrite => x.value == obj
    }
  }

  /* Direct children of e (excluding e itself but including all inner calls and return) */
  case class ParentIs(e: Enter) extends Query {
    override def sql = "caller = " + e.counter
    override def apply(t: Event) = t.caller == Some(e.counter)
  }

  case class At(i: Int) extends Query {
    override def sql = "counter = " + i
    override def apply(t: Event) = t.counter == i
  }

  case class Before(i: Int) extends Query {
    override def sql = "counter < " + i
    override def apply(t: Event) = t.counter < i
  }

  object Before {
    def apply(e: Event): Before = Before(e.counter)
  }

  case class After(i: Int) extends Query {
    override def sql = "counter >= " + i
    override def apply(t: Event) = t.counter >= i
  }

  object After {
    def apply(e: Event): After = After(e.counter)
  }

  case class Thread(thread: Long) extends Query {
    override def sql = "thread = " + thread
    override def apply(t: Event) = t.thread == thread
  }

  object Thread {
    def apply(e: Event): Thread = Thread(e.thread)
  }

  case class StackDepth(d: Int) extends Query {
    override def sql = "stack_depth = " + d
    override def apply(t: Event) = t.depth == d
  }

  case class StackTrace(e: Event) extends Assuming {
    override def assumption = Enter && Thread(e) && Before(e.counter)
    override def thenSql = "succ >= " + e.counter
    override def thenApply(t: Event) = e match {
      case x: Enter if x.successor.isDefined => x.successor.get >= e.counter
      case _                                 => throw AssumptionError
    }
  }

  case class ArgumentIs(o: Object) extends Query {
    override def sql = s"counter in (select counter from PARAMS where id = $o)"
    override def apply(e: Event) = e match {
      case e: Enter => e.arguments.contains(o)
      case _        => false
    }
  }

  sealed trait Kind extends Query {
    def key: Byte
    override def sql = "event_type = " + key
    override def apply(e: Event) = this.key == {
      e match {
        case _: Enter      => Enter.key
        case _: Exit       => Exit.key
        case _: Exception  => Exception.key
        case _: Read       => Read.key
        case _: Write      => Write.key
        case _: ArrayRead  => ArrayRead.key
        case _: ArrayWrite => ArrayWrite.key
      }
    }
  }

  object Enter extends Kind {
    override def toString = "enter"
    override val key: Byte = 1
  }

  object Exit extends Kind {
    override def toString = "exit"
    override val key: Byte = 2
  }

  object Write extends Kind {
    override def toString = "write"
    override val key: Byte = 3
  }

  object Exception extends Kind {
    override def toString = "exception"
    override val key: Byte = 4
  }

  object Read extends Kind {
    override def toString = "read"
    override val key: Byte = 5
  }

  object ArrayRead extends Kind {
    override def toString = "aread"
    override val key: Byte = 6
  }

  object ArrayWrite extends Kind {
    override def toString = "awrite"
    override val key: Byte = 7
  }
}
