package edu.mit.csail.cap.query

import scala.collection.mutable
import util.LinkedTree.Forest
import util.LinkedListTree

/** Trace representation in terms of call trees */
class CallTrace(override val c: Connection, val roots: List[CallTree])
    extends TraceProxy {
  /** Filter trees by a method predicate */
  def filter(f: Method => Boolean) =
    new CallTrace(c, for (t <- roots; u <- t.select { case rep => f(rep.method) }) yield u)

  /** Filter trees by a metho predicate but keep parents of passing nodes as well */
  def filterWithParents(f: Method => Boolean) =
    new CallTrace(c, for (t <- roots; u <- t.selectWithParents { case rep => f(rep.method) }) yield u)
  
  override def t =
    ConcTrace(c, roots.flatMap(_.nodes).sortBy(_.counter))

  override def select(f: Query): CallTrace =
    new CallTrace(c, for (t <- roots; u <- t.select { case rep => f(rep.data) }) yield u)

  override def methods: Set[Method] =
    roots.flatMap(_.methods).toSet

  override def size =
    roots.map(_.numNodes).sum

  override def trees =
    this

  override def threadIDs =
    { for (t <- roots) yield t.data.thread }.toSet
    
  override def toString = s"${c.name}(${roots.size} roots)"
}

/** A single execution tree */
class CallTree(val method: Method, val c: Connection, val counter: Int)
    extends LinkedListTree[CallTree, Enter] {
  def this(e: Enter) =
    this(e.method, e.c, e.counter)

  override def copy =
    new CallTree(method, c, counter)

  override def add(that: CallTree) {
    require(this.c == that.c, "must belong to the same trace")
    require(that.counter > this.counter, "call trees are sorted by counter")
    super.add(that)
  }

  /** Note: computing data element requires instantiating an event */
  override def data: Enter =
    c.at(counter).asInstanceOf[Enter]

  def methods: Set[Method] =
    Set(method) ++ children.flatMap(_.methods)

  /** All subtrees of depth 1 */
  def snippets: List[CallTree] =
    copy.addAll(children.map(_.copy)) :: children.flatMap(_.snippets)

  def trace = ConcTrace(c, nodes.toList)

  /** Structural hash */
  lazy val hash: Long =
    children.map(_.hash).sum + 31 * method.id
}

/** Caller-callee relationship analysis */
class CallGraph(t: Trace) extends Traversable[(Option[Enter], Enter)] {
  val trace = t.select(Enter)

  /** Roots are (None, root), leaves are (Some(parent), child) */
  override def foreach[U](f: ((Option[Enter], Enter)) => U) {
    val stacks = new mutable.HashMap[Long, List[Enter]]

    def makeStack(e: Enter) = {
      stacks(e.thread) = List(e)
      f((None, e))
    }

    for (e <- trace) e match {
      case e: Enter =>
        e.caller match {
          case None =>
            makeStack(e)
          case Some(i) =>
            stacks.get(e.thread) match {
              case Some(stk) =>
                var stack = stk
                assert(stack.size > 0, "logic error")

                while (stack.head.counter > i && stack.size > 1)
                  stack = stack.tail

                if (stack.head.counter <= i) {
                  f((Some(stack.head), e))
                  stacks(e.thread) = e :: stack
                } else
                  makeStack(e)
              case None =>
                makeStack(e)
            }
        }
      case _ => assert(false)
    }
  }

  /** Build a forest of call trees. */
  def trees: List[CallTree] = {
    val stacks = new mutable.HashMap[Long, List[CallTree]]
    var roots: List[CallTree] = Nil
    debug("building call graph for " + t)
    this.foreach {
      case (None, e) =>
        roots = (new CallTree(e)) :: roots
        stacks(e.thread) = List(roots.head)
      case (Some(e), f) =>
        var stack = stacks(e.thread)
        while (stack.head.counter != e.counter)
          stack = stack.tail
        stacks(e.thread) = (stack.head + new CallTree(f)) :: stack
    }
    debug("done")
    roots.reverse
  }
}
