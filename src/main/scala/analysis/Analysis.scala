package edu.mit.csail.cap.query
package analysis

import scala.collection.mutable
import util._

/** Framework-aware analysis of a full trace */
trait BoundaryAnalysis extends TraceProxy {
  implicit def b: FrameworkBoundary

  override def select(cond: Query) =
    BoundaryView(t.select(cond), b)

  override def trees =
    new BoundaryCallAnalysis(this, calls.trees)

  def f = trees.projected
  def userMethods = methods.filter(b.isUser)
  def frameworkMethods = methods.filter(b.isFramework)
  def surfaceMethods = f.methods.filter(b.isFramework)

  /** Methods overriding framework */
  def overriding = for (m <- userMethods; if b.overrides(m).isDefined) yield m

  /** Overridden framework method definitions */
  def overridden = for (m <- userMethods; n <- b.overrides(m)) yield n.definition

  /** Distinct queries */
  lazy val queries: Scores = {
    val ds = trees.depths

    Scores(overridden.map {
      case m => Score(q = Extends(m), depth = 0, heuristics = Heuristics(m))
    } ++
      frameworkMethods.map {
        case m => Score(q = Invokes(m), depth = ds(m), heuristics = Heuristics(m))
      })
  }

  /** This computes binary queries (quadratic in the depths of the trees!) */
  lazy val binaryQueries: Scores = {
    // walk call trees and populate the mutable set of outputs with the minimum depth
    val out = new mutable.HashMap[NestedQuery, Int]

    // update minimum depth for a nested query
    def add(q: NestedQuery, depth: Int) {
      out.put(q, Math.min(depth, out.getOrElse(q, Int.MaxValue)))
    }

    // remove internal user methods
    val proj = trees.roots.map(_.project[Option[Int]] {
      case t if b.isUser(t.method) => None
      case t                       => Some(t.counter)
    })

    // walk the tree
    def visit(root: CallTree, under: Option[Method], depth: Int = 0) {
      if (b.isUser(root.method)) {
        root.children.map(visit(_, b.overrides(root.method)))
      } else {
        if (b.isFramework(root.method)) {
          under match {
            case Some(m) => add(NestedQuery(Extends(m.definition), Invokes(root.method)), depth)
            case None    =>
          }
        }
        root.children.map(visit(_, under, depth + 1))
      }
    }
    
    info(s"computing binary queries for $this")
    proj.map(visit(_, None))
    info(s"computed ${out.size} queries")
    
    Scores(out.map {
      case (q, depth) => Score(
          q = q, 
          depth = depth, 
          heuristics = 0)
    }.toSet)
  }
}

case class BoundaryView(t: Trace, b: FrameworkBoundary) extends BoundaryAnalysis {
  override def toString = "boundary view"
}

class BoundaryCallAnalysis(a: BoundaryAnalysis, roots: List[CallTree])
    extends CallTrace(a.c, roots) {
  def b = a.b

  def filtered =
    filter { (m: Method) => b.isUser(m) || b.isFramework(m) }

  def projected = new BoundaryCallAnalysis(a,
    // remove intermediate framework and user methods
    // remove singleton framework methods
    filtered.roots.project {
      (t: CallTree) => b.isUser(t.method)
    }.filterNot {
      t => t.isEmpty && b.isFramework(t.method)
    })

  /** Restrict to a subset of methods: framework invoked and user overriding */
  def restrict(invokes: Set[Method], overrides: Set[Method]) =
    filter { (m: Method) =>
      m match {
        case m if b.isFramework(m) => invokes.contains(m)
        case m if b.isUser(m)      => !(overrides intersect m.overridden.toSet).isEmpty
        case _                     => false
      }
    }

  /** Remove second appearance of a method */
  def unique = {
    def helper(in: CallTree, out: CallTree, seen: mutable.HashSet[Method]) {
      for (inc <- in.children)
        if (!seen(inc.method)) {
          seen += inc.method
          helper(inc, out + inc.copy, seen)
        } else {
          helper(inc, out, seen)
        }
    }

    val seen = new mutable.HashSet[Method]
    new BoundaryCallAnalysis(a, for (in <- roots) yield {
      val out = in.copy
      helper(in, out, seen)
      out
    })
  }

  /** Conservative compression */
  def compress = {
    import scala.util.hashing.Hashing

    /** Eliminate structurally similar trees with similar prefixes */
    def apply(trees: List[CallTree],
              prefix: Set[Method] = Set(),
              hashes: mutable.Set[(Long, Long)] = new mutable.HashSet): List[CallTree] = {
      val out = new mutable.ListBuffer[CallTree]
      val pre = prefix.map(_.id).sum
      for (tree <- trees) {
        val kid = tree.copy.addAll(apply(tree.children, prefix + tree.method, hashes))
        val h = (pre, kid.hash)
        if (!hashes.contains(h)) {
          hashes += h
          // empty sub tree
          hashes += ((pre, 31 * kid.method.id))
          out += kid
        }
      }
      out.toList
    }

    new BoundaryCallAnalysis(a, apply(roots))
  }

  /**
   *  Framework surface calls have depth 0.
   *  Filters to user and framework calls only first.
   *  Depth incremented at every call from framework to framework.
   *  Top-level framework calls have depth 1.
   */
  def depths: Map[Method, Int] = {
    val out = new mutable.HashMap[Method, Int]
    def apply(t: CallTree, i: Int) {
      if (b.isUser(t.method)) {
        for (kid <- t.children)
          apply(kid, 0)
      } else {
        // update framework method entry
        out(t.method) = Math.min(i, out.getOrElse(t.method, Int.MaxValue))
        for (kid <- t.children)
          apply(kid, i + 1)
      }
    }

    for (r <- filtered.roots)
      apply(r, 1)

    out.toMap
  }
}

/** Analysis of a complete trace */
trait TraceAnalysis
    extends BoundaryAnalysis
    with Synthesizer
    with CriticalChain
    with Ordered[TraceAnalysis] {
  def name: String

  def search(q: CallQuery)(implicit p: Parameters) =
    q.search(this).take(p.Matches).toList

  def synthesize(q: CallQuery)(implicit p: Parameters): List[CodeResult] =
    q.search(this).take(p.Matches).toList.map(events => compile(events.flatMap(_.seeds)))

  override def compare(that: TraceAnalysis) = this.name.compare(that.name)
}

/** Analysis over several traces with positive and negative signals */
case class Group(traces: List[TraceAnalysis]) extends Statistics {
  def ++(that: Group) = Group((this.traces ++ that.traces).distinct)
  def isEmpty = traces.isEmpty
  def size = traces.map(_.size).sum

  /** Extract match queries common for traces but not present in baseline */
  def demoMatch(
    baseline: Group = Group(Nil),
    scope: Option[CallQuery] = None,
    under: Boolean = false): Scores = {

    if (traces.isEmpty)
      return Scores(Set())

    val pos = (scope, under) match {
      case (None, _)             => traces.map(_.queries)
      case (Some(parent), true)  => traces.map(t => parent.under(t).queries)
      case (Some(parent), false) => traces.map(t => parent.over(t).queries)
    }

    val specific = pos.map(_.scores.map(_.q)).reduce(_ intersect _)

    val neg = (baseline.traces diff traces).map(_.queries)
    val common = neg.map(_.scores.map(_.q)).fold(Set())(_ union _)

    val qs = specific diff common
    val scores = pos.toList.flatMap(_.scores).groupBy(_.q)

    // aggregate scores
    Scores(qs.map {
      case q =>
        Score(
          heuristics = scores(q).map(_.heuristics).max,
          depth = scores(q).map(_.depth).min,
          q = (scope, under) match {
            case (None, _)             => q
            case (Some(parent), true)  => NestedQuery(parent, q)
            case (Some(parent), false) => NestedQuery(q, parent)
          })
    })
  }

  /** Variation of trying to extract binary queries */
  def binaryDemoMatch(baseline: Group = Group(Nil)): Scores = {
    if (traces.isEmpty)
      return Scores(Set())

    val pos = traces.map(_.binaryQueries)
    val specific = pos.map(_.scores.map(_.q)).reduce(_ intersect _)

    val neg = (baseline.traces diff traces).map(_.binaryQueries)
    val common = neg.map(_.scores.map(_.q)).fold(Set())(_ union _)

    val qs = specific diff common
    val scores = pos.toList.flatMap(_.scores).groupBy(_.q)

    // aggregate scores
    Scores(qs.map {
      case q =>
        Score(
          heuristics = scores(q).map(_.heuristics).max,
          depth = scores(q).map(_.depth).min,
          q = q)
    })
  }

  /** Concept lattice based on call queries */
  def concepts(baseline: Group = Group(Nil)) = {
    val neg = baseline.traces diff traces
    val common = neg.map(_.queries.scores.map(_.q)).fold(Set())(_ union _)

    Concepts[TraceAnalysis, CallQuery](traces.map(t => t ->
      (t.queries.scores.map(_.q) diff common)).toMap)
  }

  /** Concept lattice based on methods */
  val methods =
    Concepts[TraceAnalysis, Method](traces.map(t => t -> t.methods).toMap)

  def search(q: CallQuery)(implicit p: Parameters) =
    for (t <- traces)
      yield (t, t.search(q))

  def synthesize(q: CallQuery)(implicit p: Parameters) =
    for (t <- traces)
      yield (t, t.synthesize(q))
}

