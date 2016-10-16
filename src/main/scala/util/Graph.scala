package edu.mit.csail.cap.query
package util

import scala.collection.{ mutable, immutable }
import scala.language.higherKinds
import scala.annotation.tailrec

/** An edge in a directed graph */
case class Edge[V, +L](from: V, l: L, to: V)

/**
 * Directed graph.
 *  Nodes cannot be mutated while in the graph.
 *  Edge labels can be mutated.
 *  Optimized for outbound queries.
 */
trait Graph[V, L] extends Traversable[Edge[V, L]] {
  type E = Edge[V, L]

  /** Nodes of the graph */
  def nodes: Traversable[V]

  /** Edges of the graph. Recommended to implement efficiently. Skips over disconnected nodes. */
  def foreach[U](f: E => U)

  /** Outbound edges */
  def outbound(v: V): Traversable[(V, L)] =
    collect { case Edge(from, l, to) if from == v => (to, l) }

  /** Inbound edges */
  def inbound(v: V): Traversable[(V, L)] =
    collect { case Edge(from, l, to) if to == v => (from, l) }

  /** Tests whether the graph contains a node */
  def has(v: V): Boolean =
    nodes.exists(_ == v)

  /** Test whether the graph contains an edge */
  def has(v: V, w: V): Boolean =
    outbound(v).exists(_._1 == w)

  /** Get edge labels between two nodes */
  def get(v: V, w: V): Traversable[L] =
    outbound(v).collect { case (to, l) if to == w => l }

  /** Number of nodes */
  def numNodes: Int =
    nodes.size

  /** Number of edges */
  def numEdges: Int =
    this.size

  /** Number of inbound edges */
  def inDegree(v: V): Int =
    inbound(v).size

  /** Number of outgoing edges */
  def outDegree(v: V): Int =
    outbound(v).size

  /* 
   * Strongly connected components. 
   * Implementation of the classic Tarjan algorithm.
   */
  def SCC(): List[Set[V]] = {
    var i = 0
    val index = new mutable.HashMap[V, Int]
    val low = new mutable.HashMap[V, Int]
    val s = new mutable.Stack[V]
    var out: List[Set[V]] = Nil

    def tarjan(v: V) {
      index(v) = i
      low(v) = i
      i = i + 1
      s.push(v)

      for ((w, _) <- outbound(v))
        if (!index.contains(w)) {
          tarjan(w)
          low(v) = math.min(low(v), low(w))
        } else if (s.contains(w))
          low(v) = math.min(low(v), index(w))

      if (low(v) == index(v)) {
        var scc = new mutable.LinkedHashSet[V]
        var w = v
        do {
          w = s.pop()
          scc += w
        } while (w != v)
        out = scc.toSet :: out
      }
    }

    for (v <- nodes)
      if (!index.contains(v))
        tarjan(v)

    out
  }

  /** Nodes reachable from the given node. Always includes the node itself. */
  def reachable(v: V): Set[V] = {
    val out = new mutable.LinkedHashSet[V]
    def DFS(v: V) {
      out += v
      for (
        (to, _) <- outbound(v) if !out.contains(to)
      ) DFS(to)
    }
    DFS(v)
    out.toSet
  }

  /** Self loops. */
  def selfloops(): Traversable[V] =
    collect { case Edge(from, _, to) if from == to => from }

  /** Cycle detection. Ignores self loops. */
  def hasCycles(): Boolean =
    SCC().size != numNodes

  /** Cycle detection. */
  def isDAG() =
    !hasCycles && selfloops.size == 0

  /** Optimizes inherited Traversal routines */
  override def hasDefiniteSize = false

  override def toString =
    "graph with " + numNodes + " nodes and " + numEdges + " edges"
}

/** Mutable directed graph. */
trait MutableGraph[V, L] extends Graph[V, L] {
  /** Adds an edge and adds its end-points. */
  def add(from: V, l: L, to: V)

  /** Adds a graph */
  def addAll(that: Graph[V, L]) {
    for (v <- that.nodes)
      this.add(v)
    for (Edge(from, l, to) <- that)
      this.add(from, l, to)
  }

  /** Adds a node. */
  def add(v: V)

  /** Removes edges between given nodes. Returns removed edges. */
  def remove(from: V, to: V, f: L => Boolean): Traversable[E]

  /** Removes a node and all its incoming and outgoing edges. */
  def remove(v: V)
}

/** Sample implementation of a mutable graph. */
trait MutableGraphLike[Repr[W, K] <: MutableGraphLike[Repr, W, K], V, L]
    extends MutableGraph[V, L] { self: Repr[V, L] =>
  /** Create an empty graph. */
  def empty[W, K]: Repr[W, K]

  /** Build a graph using a function to map edges. Ignores singletons. */
  def morphism[W, K](f: PartialFunction[Edge[V, L], Edge[W, K]]): Repr[W, K] = {
    val out = empty[W, K]
    for (
      e <- this;
      if f.isDefinedAt(e);
      Edge(from, l, to) = f(e)
    ) out.add(from, l, to)
    out
  }

  /** Build a graph using a function to map nodes. Ignores singletons. */
  def homomorphism[W](f: PartialFunction[V, W]): Repr[W, L] =
    morphism {
      case Edge(from, l, to) if f.isDefinedAt(from) && f.isDefinedAt(to) =>
        Edge(f(from), l, f(to))
    }

  /** Build a graph without singletons */
  def connected(): Repr[V, L] =
    morphism { case e => e }

  /** Build a graph using a function to select edges. */
  override def filter(f: Edge[V, L] => Boolean): Repr[V, L] =
    morphism { case e if f(e) => e }

  /** Create a graph with all edges reversed. */
  def invert(): Repr[V, L] =
    morphism { case Edge(from, l, to) => Edge(to, l, from) }

  /** Create a graph with end-points in the given set. */
  def subgraph(s: Set[V]): Repr[V, L] =
    subgraph(s.contains(_))

  def subgraph(f: V => Boolean): Repr[V, L] =
    homomorphism { case v if f(v) => v }

  /** Make a graph with all the edges except self-loops. */
  def withoutSelfLoops(): Repr[V, L] =
    filter { case Edge(from, _, to) => from != to }

  /** Nodes that are reachable from node "from" but from which we can reach the node "to". */
  def reachable(from: V, to: V): Repr[V, L] =
    subgraph(reachable(from) intersect invert.reachable(to))

  /**
   * Compute a graph with nodes at SCC and edges from this graph.
   *  The graph is guaranteed to be a DAG.
   */
  def condensation(): Repr[Set[V], Unit] = {
    debug("computing condensation")
    val out = empty[Set[V], Unit]
    val nodes = SCC()
    val m = new mutable.HashMap[V, Set[V]]
    for (vs <- nodes) {
      out.add(vs)
      for (v <- vs)
        m(v) = vs
    }

    for (
      e <- this;
      from = m(e.from);
      to = m(e.to);
      if from != to
    ) out.add(from, (), to)

    debug("done")

    out
  }

  /**
   * Compute the irreflexive transitive closure R^+.
   *  Applies DFS at every node: O(VE)
   */
  def transitiveClosure(): Repr[V, Unit] = {
    val out = empty[V, Unit]
    for (v <- nodes) {
      out.add(v)
      for (w <- reachable(v) - v)
        out.add(v, (), w)
    }
    out
  }

  /**
   * Compute the relational composition:
   *  for every (v, w) in this, (w, u) in that,
   *  (v, u) is in the result
   */
  def compose(that: Graph[V, _]): Repr[V, Unit] = {
    val out = empty[V, Unit]
    for (
      Edge(v, _, w) <- this;
      u <- that.outbound(w).map(_._1)
    ) out.add(v, (), u)
    out
  }

  /**
   * Compute the relational subtraction:
   *  for (v, l, w) in this, (v, l, w) is in the result
   *  if and only if (v, w) is not in that.
   */
  def subtract(that: Graph[V, _]): Repr[V, L] = {
    val out = empty[V, L]
    for (
      Edge(from, l, to) <- this if !that.has(from, to)
    ) out.add(from, l, to)
    out
  }

  /**
   * Compute the transitive reduction R^-.
   *  Definition:  R^- = R - R \compose R^+.
   *  (where \compose is the relation composition)
   *
   *  For DAGs, its their minimal representation.
   */
  def transitiveReduction(): Repr[V, L] =
    subtract(compose(transitiveClosure()))
}

class MultiDigraph[@specialized(Int) V, L] extends MutableGraphLike[MultiDigraph, V, L] {
  private val edges = new mutable.HashMap[V, List[(V, L)]]

  override def empty[V, L] = new MultiDigraph[V, L]

  override def foreach[U](f: Edge[V, L] => U) {
    for ((from, pointers) <- edges)
      for ((to, l) <- pointers)
        f(Edge(from, l, to))
  }

  override def nodes = edges.keys

  override def has(n: V) = edges.contains(n)

  override def outbound(n: V) = edges.getOrElse(n, Nil)

  override def add(v: V) {
    if (!has(v))
      edges(v) = Nil
  }

  override def add(from: V, l: L, to: V) {
    add(from)
    add(to)
    edges(from) = (to, l) :: edges(from)
  }

  def remove(from: V, to: V => Boolean = _ => true, f: L => Boolean = _ => true): List[(V, L)] =
    if (edges.contains(from)) {
      val (remove, remain) = outbound(from).partition { case (v, l) => to(v) && f(l) }
      edges(from) = remain
      remove
    } else
      Nil

  override def remove(from: V, to: V, f: L => Boolean) =
    remove(from, _ == to, f).map { case (to, l) => Edge(from, l, to) }

  override def remove(v: V) {
    edges.remove(v)
    for (w <- edges.keys)
      edges(w) = edges(w).filter(p => p._1 != v)
  }

  override def numNodes = edges.size

  override def numEdges = {
    var i = 0
    for ((n, m) <- edges)
      i = i + m.size
    i
  }
}

/** Simple digraph. Adding multiple edges between same nodes keeps only latest label. */
class Digraph[V, @specialized(Unit) L]
    extends MutableGraphLike[Digraph, V, L] {
  private val edges = new mutable.HashMap[V, mutable.HashMap[V, L]]

  override def empty[V, L] = new Digraph[V, L]

  override def foreach[U](f: Edge[V, L] => U) {
    for ((from, neighbors) <- edges)
      for ((to, l) <- neighbors)
        f(Edge(from, l, to))
  }

  override def nodes = edges.keys

  override def has(n: V) = edges.contains(n)

  override def outbound(n: V) = edges.getOrElse(n, Nil)

  override def inbound(n: V) =
    for (
      (from, neighbors) <- edges if neighbors.contains(n)
    ) yield (from, neighbors(n))

  override def get(n: V, m: V): List[L] =
    if (has(n))
      edges(n).get(m).toList
    else
      Nil

  override def add(v: V) {
    if (!has(v))
      edges(v) = new mutable.HashMap
  }

  override def add(from: V, l: L, to: V) {
    add(from)
    add(to)
    edges(from)(to) = l
  }

  override def remove(v: V) {
    edges.remove(v)
    for ((x, neighbors) <- edges)
      neighbors.remove(v)
  }

  override def remove(from: V, to: V, f: L => Boolean = _ => true) =
    if (has(from))
      edges(from).get(to) match {
        case Some(l) if f(l) =>
          edges(from).remove(to)
          Edge(from, l, to) :: Nil
        case _ =>
          Nil
      }
    else
      Nil

  override def numNodes = edges.size

  override def numEdges = {
    var i = 0
    for ((v, m) <- edges)
      i = i + m.size
    i
  }
}

/** Like digraph, but aggregates labels in add operation. */
class CountingDigraph[V] extends Digraph[V, Int] {
  override def add(from: V, l: Int, to: V) {
    get(from, to) match {
      case count :: Nil => super.add(from, count + l, to)
      case Nil          => super.add(from, l, to)
      case _            => assert(false)
    }
  }
}

object CountingDigraph {
  def from[V](p: Traversable[(V, V)]): CountingDigraph[V] = {
    val out = new CountingDigraph[V]
    for ((a, b) <- p)
      out.add(a, 1, b)
    out
  }
}

object Hasse {
  def lattice[T](objects: List[Set[T]], SkipSingletons: Boolean = true): Graph[(Int, Set[T]), Unit] = {
    // orient from big to small
    val g = new util.Digraph[Set[T], Unit]
    for (
      a <- objects; b <- objects;
      if a.subsetOf(b) && a.size < b.size;
      if (!SkipSingletons) || 1 < a.size
    ) g.add(b, (), a)

    val reduced = g.transitiveReduction

    // remove 
    var i = 0
    var map = Map[Set[T], (Int, Set[T])]()
    for (os <- objects) {
      i = i + 1
      val sub = reduced.outbound(os).map(_._1).flatten.toSet
      map += os -> (i, os.filterNot(sub))
    }

    reduced.homomorphism(map)
  }
}
