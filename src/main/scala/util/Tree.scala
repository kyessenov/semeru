package edu.mit.csail.cap.query
package util

import scala.collection.mutable

/**
 * The edges are directed from the root downwards.
 *
 * Each node corresponds to a subtree rooted at the node as a graph.
 *
 * Contains at least one node.
 *
 * The elements in the tree must be distinct for the graph view to function
 * correctly.
 */
trait Tree[V] extends Graph[V, Unit] {
  /** Data element at the root of the tree. */
  def data: V

  /** Number of edges on the longest path to a leaf */
  def depth: Int = children match {
    case Nil => 0
    case l   => 1 + l.map(_.depth).max
  }

  /** Children of the root. */
  def children: Traversable[Tree[V]]

  /** Search for a transitive child node. */
  def search(v: V): Option[Tree[V]] =
    if (data == v)
      Some(this)
    else {
      for (c <- children)
        c.search(v) match {
          case Some(e) => return Some(e)
          case _       =>
        }
      return None
    }

  /** Traverses nodes in DFS fashion. Root then children recursion. */
  override def nodes =
    Traversable(data).view ++ children.view.flatMap(_.nodes)

  /** Traverses edges in DFS fashion. */
  override def foreach[U](f: Edge[V, Unit] => U) {
    for (c <- children) {
      f(Edge(data, (), c.data))
      c.foreach(f)
    }
  }

  override def outbound(v: V) = search(v) match {
    case Some(kid) =>
      for (c <- kid.children)
        yield (c.data, ())
    case None => None
  }

  override def has(v: V) = (data == v) || children.exists(_.has(v))
  override def numEdges = children.map(_.numNodes).foldLeft(0)(_ + _)
  override def numNodes = 1 + numEdges
  override def isEmpty = children.isEmpty
  override def toString = "tree with " + numNodes + " nodes at " + data
}

/**
 * Linked tree data structure.
 *
 * Data elements are mutable.
 * Nodes are mutable as long as the tree property is maintained
 * (no checks that you might try to add this to this.)
 *
 * Operations are local to a node. Data is invoked only when necessary.
 *
 * Children are ordered.
 */
trait LinkedTree[Repr <: LinkedTree[Repr, V], V] extends Tree[V] { self: Repr =>
  /** Add a direct child as last */
  def add(t: Repr)

  /** Add and return a child */
  def +(t: Repr): Repr = {
    add(t)
    t
  }

  /** Add all children */
  def addAll(ts: Traversable[Repr]): Repr = {
    for (t <- ts) add(t)
    this
  }

  /** Remove a direct child by data. */
  def remove(v: V)

  /** Create a new tree with the same data but no children. */
  def copy: Repr

  /** Ordered list of children. */
  override def children: List[Repr]

  /** Grand children. */
  def grandChildren: List[Repr] =
    for (c <- children; c1 <- c.children) yield c1

  /**
   * Make a copy containing only nodes satisfying f.
   * The children of the removed nodes are inherited
   * by the grandparent or made roots.
   */
  def select(f: Repr => Boolean): List[Repr] = {
    def helper(in: Repr, out: Repr) {
      assert(f(out))
      for (inc <- in.children)
        if (f(inc))
          helper(inc, out + inc.copy)
        else
          helper(inc, out)
    }

    if (f(this)) {
      val out = copy
      helper(this, out)
      out :: Nil
    } else
      children.flatMap(_.select(f))
  }
  
  def selectWithParents(f: Repr => Boolean): Option[Repr] = {
    val kids: List[Repr] = children.flatMap(_.selectWithParents(f))
    if (kids.size > 0)
      Some(copy.addAll(kids))
    else if (f(this))
      Some(copy)
    else
      None
  }

  /** Check that data is pairwise similar in both trees. */
  def isSimilar(that: Repr, similar: (V, V) => Boolean): Boolean =
    similar(this.data, that.data) &&
      this.children.corresponds(that.children) { _.isSimilar(_, similar) }

  /**
   * Compute top-most nodes in the tree for which the condition holds
   */
  def find(f: Repr => Boolean): List[Repr] =
    if (f(this))
      this :: Nil
    else
      children.flatMap(_.find(f))

  /** Traverses tree in DFS fashion. Root then children recursion. */
  def trees: Traversable[Repr] =
    Traversable(this).view ++ children.view.flatMap(_.trees)

  /**
   * Make a copy by merging children with parents
   * if they have the same f-value.
   * Preserves only top-level value.
   */
  def project[T](f: Repr => T): Repr = {
    def helper(in: Repr, out: Repr) {
      assert(f(in) == f(out))
      for (inc <- in.children)
        if (f(inc) == f(out))
          helper(inc, out)
        else
          helper(inc, out + inc.copy)
    }

    val out = copy
    helper(this, out)
    out
  }
}

object LinkedTree {
  implicit class Forest[Repr <: LinkedTree[Repr, V], V](val roots: List[LinkedTree[Repr, V]]) extends AnyVal {
    def select(f: Repr => Boolean): List[Repr] =
      for (t <- roots; s <- t.select(f)) yield s
    def project(f: Repr => _): List[Repr] =
      for (t <- roots) yield t.project(f)
    def numNodes = roots.map(_.numNodes).sum
    def numEdges = roots.map(_.numEdges).sum
  }
}

/** Linked list implementation of a linked tree */
trait LinkedListTree[Repr <: LinkedListTree[Repr, V], V] extends LinkedTree[Repr, V] { self: Repr =>
  /** Reverse list of children.*/
  private var rep: List[Repr] = Nil
  override def add(t: Repr) {
    rep = t :: rep
  }
  override def remove(v: V) {
    rep = rep.filterNot(_.data == v)
  }
  override def children = rep.reverse
}

class LinkedTreeImpl[V](val data: V) extends LinkedListTree[LinkedTreeImpl[V], V] {
  override def copy = new LinkedTreeImpl(data)
}
