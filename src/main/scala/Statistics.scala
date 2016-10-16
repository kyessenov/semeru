package edu.mit.csail.cap.query

import scala.collection.mutable
import util._

/** Cumulative counts */
trait Statistics {
  def traces: List[Trace]

  /** Group events by a function: event => T */
  def eventsBy[T](f: PartialFunction[Event, T])(implicit top: Int = 20): List[(T, Int)] = {
    val out = new mutable.OpenHashMap[T, Int]
    for (
      t <- traces;
      e <- t
    ) {
      if (f.isDefinedAt(e)) {
        val i = f(e)
        out.put(i, out.getOrElse(i, 0) + 1)
      }
    }
    out.toList.sortBy(_._2).takeRight(top).reverse
  }

  def eventsByClass(implicit top: Int = 20) =
    eventsBy { case e if e.caller.isDefined => e.parent.get.method.declarer }

  def eventsByPkg(implicit top: Int = 20) =
    eventsBy { case e if e.caller.isDefined => e.parent.get.method.declarer.packag }

  def callsByCut(b: FrameworkBoundary) =
    traces.flatMap(_.calls.collect { case (Some(e), f) if b.cut(e, f) => (e, f) })

  /** Group calls by a function: (caller method, callee method) => T */
  def callsBy[T](f: PartialFunction[(Method, Method), T])(implicit top: Int = 20): List[(T, Int)] = {
    val out = new mutable.HashMap[T, Int]
    for (
      t <- traces;
      (Some(caller), callee) <- t.calls
    ) {
      val input = (caller.method, callee.method)
      if (f.isDefinedAt(input)) {
        val i = f(input)
        out.put(i, out.getOrElse(i, 0) + 1)
      }
    }
    out.toList.sortBy(_._2).takeRight(top).reverse
  }

  /** Count calls for same value of f */
  def selfcalls[T](f: Method => T)(implicit top: Int = 20) =
    callsBy { case (m1, m2) if f(m1) == f(m2) => f(m1) }

  /** Count calls for different values of f */
  def crosscalls[T, U](f: Method => T, g: (T, T) => U)(implicit top: Int = 20) =
    callsBy { case (m1, m2) if f(m1) != f(m2) => g(f(m1), f(m2)) }

  /** Self calls by package */
  def selfByPkg(implicit top: Int = 20) =
    selfcalls(_.declarer.packag)

  /** Cross calls by package */
  def crossByPkg(implicit top: Int = 20) =
    crosscalls(_.declarer.packag, (x: String, y: String) => (x, y))

  /** Cross calls by package target */
  def crossByPkgDst(implicit top: Int = 20) =
    crosscalls(_.declarer.packag, (_: String, y: String) => y)

  /** Cross calls by package source */
  def crossByPkgSrc(implicit top: Int = 20) =
    crosscalls(_.declarer.packag, (x: String, _: String) => x)

  /** Group events by their field and method */
  def eventsByMember(implicit top: Int = 20): List[(Member, Int)] =
    traces match {
      case (t: DeclTrace) :: Nil =>
        t.select(Enter || Read || Write) match {
          case DeclTrace(c, where) =>
            c.read(s"select id, COUNT(*) as x from LOG where ${where.sql} group by id order by x desc limit $top") {
              rs => (t.meta.member(rs.getLong(1)), rs.getInt(2))
            }
        }
      case (proxy: TraceProxy) :: Nil if proxy.t != proxy =>
        proxy.t.eventsByMember
      case _ =>
        eventsBy {
          case e: Enter => e.method
          case e: Read  => e.field
          case e: Write => e.field
        }
    }

  /** Group events by their type */
  def eventsByKind: Map[Kind, Int] = {
    var out: Map[Kind, Int] = Map()
    for (t <- traces) {
      val counts = t match {
        case DeclTrace(c, where) =>
          c.read(s"select event_type, COUNT(*) as x from LOG where ${where.sql} group by event_type order by x desc") {
            rs => (Kinds.find(_.key == rs.getInt(1)).get, rs.getInt(2))
          }
        case proxy: TraceProxy if proxy.t != proxy =>
          proxy.t.eventsByKind
        case _ =>
          debug(s"streaming from $t")
          eventsBy {
            case e => Kinds.find(_.apply(e)).get
          }
      }

      for ((kind, count) <- counts)
        out += kind -> (out.getOrElse(kind, 0) + count)
    }

    out
  }

  /** Group all objects in the connection by the type */
  // TODO: only works for one trace
  def objectsByType(implicit top: Int = 20): List[(Type, Int)] =
    traces(0).c.read(s"select type, COUNT(*) as x from OBJECTS group by type order by x desc limit $top") {
      rs => (traces(0).meta.typ(rs.getLong(1)), rs.getInt(2))
    }

  /** Graph of calls by declarer */
  def classDependencies(): Digraph[ClassType, Int] = {
    val out = new CountingDigraph[ClassType]
    for (
      t <- traces;
      (Some(from), to) <- t.calls
    ) out.add(from.method.declarer, 1, to.method.definition.declarer)
    out
  }

  /**
   * Return a new graph whose vertices are package names.
   * Source package is the concrete package.
   * Destination package is the most abstract possible.
   */
  def packageDependencies(): Digraph[Package, Int] = {
    val out = new CountingDigraph[Package]
    for (
      t <- traces;
      (Some(from), to) <- t.calls
    ) out.add(from.method.declarer.packag, 1, to.method.definition.declarer.packag)
    out
  }

  def moduleDependencies(modules: List[ClassMask] = Nil): Digraph[ClassMask, Int] = {
    // always add two special modules
    val ms = PackageName("java.lang.reflect") :: PackageName("") :: modules

    def module(t: ClassType): ClassMask =
      ms.find(_(t)) match {
        case Some(module) => module
        case None         => PackageName(t.packag)
      }

    val out = new CountingDigraph[ClassMask]
    for (
      t <- traces;
      (Some(from), to) <- t.calls
    ) out.add(module(from.method.declarer), 1, module(to.method.definition.declarer))
    out
  }

  def callsByPkg(from: ClassMask, to: ClassMask) =
    traces.flatMap(_.calls.collect { case (Some(a), b) if from(a.method.declarer) && to(b.method.definition.declarer) => (a, b) })

  def modules(modules: List[ClassMask]): Digraph[Set[ClassMask], Unit] = {
    val out = moduleDependencies(modules).withoutSelfLoops
    out.remove(PackageName(""))
    out.remove(PackageName("java.lang.reflect"))
    out.condensation.transitiveReduction
  }
}
