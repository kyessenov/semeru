package edu.mit.csail.cap.query
package analysis

import scala.collection.Traversable
import scala.collection.mutable
import util.{ Duration, Digraph }
import ast._
import java.util.concurrent.Callable

case class CodeResult(start: List[Seed],
                      code: Code,
                      graph: SliceGraph,
                      durations: Map[String, Duration],
                      counters: Map[String, Int],
                      // incremented for non-local transitions
                      depth: Map[Event, Int]) {
  lazy val initial: Set[Event] =
    graph.initial.map(_.e).toSet

  lazy val depends: Digraph[Statement, Unit] = {
    val events = stmts.map { case (s, _) => (s.e, s) }
    graph.restrict(events.keys.toSet).homomorphism { case e => events(e) }
  }

  lazy val stmts: Map[Statement, Int] =
    code.bodies.flatMap(_.stmts).zipWithIndex.toMap
    
  lazy val initialStmts: Set[Statement] = {
    val initial = start.map(_.e).toSet
    stmts.keys.toSet.filter { _.e match {
      case e: Enter => 
        initial.exists(d => e == d || e.contains(d))
      case e => 
        initial(e)
    }}
  }

  def stats: List[(String, String)] =
    durations.toList.sortBy(_._2).reverse.map {
      case (k, v) => (k, v.toString)
    } ::: counters.toList.map {
      case (k, v) => (k, v.toString)
    }
    
  def c = code.c
    
  override def toString = code.toString
}

trait Synthesizer extends BoundaryAnalysis {
  implicit def errors = meta.errors

  def rule = CompositeSlicer(
    LocalSlicer() ::
      HeapSlicer() ::
      StaticSlicer() ::
      CoverSlicer(b) ::
      //    ParameterSlicer(c.meta.method("javax.swing.ActionMap.get(Ljava/lang/Object;)Ljavax/swing/Action;").getOrElse(c.meta.dummify)) ::
      meta.containers.all)

  /** Connect events by following rule in BFS */
  def path(from: Int, to: Int): List[(Seed, SliceRule)] = {
    // BFS
    val q = new mutable.Queue[Seed]
    val out = new mutable.HashMap[Seed, List[(Seed, SliceRule)]]

    // initialize with seeds
    for (seed <- at(from).seeds) {
      q.enqueue(seed);
      out(seed) = (seed, EmptyRule()) :: Nil
    }

    while (!q.isEmpty) {
      val cur = q.dequeue()

      if (cur.e.counter == to)
        return out(cur)

      for ((next, slice) <- rule.expandAll(cur))
        if (!out.contains(next)) {
          q.enqueue(next);
          out(next) = (next, slice) :: out(cur)
        }
    }

    return Nil
  }

  def compile(start: List[Seed])(implicit p: Parameters): CodeResult = {
    debug("ensuring heap series " + heaps)

    val initial = start.map(_.e).toSet
    val (graph, durations, counters, depths) = rule.expand(start, p)
    val (projected, t2) = timed(project(graph.events, initial))
    val (generated, t3) = timed(projected.map(generate))
    val (code, t4) = timed(simplify(generated, initial))

    CodeResult(start, code, graph,
      durations.map { case (k, v) => (k.toString, v) } +
        ("Project" -> t2) +
        ("Generate" -> t3) +
        ("Simplify" -> t4),
      counters +
        ("#postLines" -> code.bodies.map(_.size).sum) +
        ("#preLines" -> generated.map(_.size).sum) +
        ("#deepestStmt" -> ((-1) :: { for (b <- code.bodies; s <- b.stmts) yield depths.getOrElse(s.e, -1) }).max) +
        ("#deepestEvent" -> ((-1) :: depths.values.toList).max),
      depths)
  }

  /** Group events into method body traces */
  def project(events: Set[Event], initial: Set[Event])(implicit p: Parameters): List[Trace] = {
    // place into boundary covers
    val covers: Set[Enter] = events.flatMap(b.parentCover) ++
      (if (p.IncludeSeeds) initial.collect { case e: Enter if b.isUser(e) => e } else Set())

    // closest parent cover 
    def place(e: Event): Option[Enter] = covers.filter(_.contains(e)) match {
      case l if !l.isEmpty => Some(l.maxBy(_.counter))
      case _               => None
    }

    val out = new mutable.HashMap[Enter, mutable.HashSet[Event]]
    for (e <- covers if b.isUser(e)) out += e -> (new mutable.HashSet + e)

    // place into user covers
    for (e <- events; d <- place(e) if b.isUser(d))
      out(d) ++= {
        e match {
          case _: FieldEvent | _: ArrayEvent if b.isUser(e) => Some(e)
          case e: Enter if e.method.isConstructor && b.isUser(e) => Some(e)
          case e: Enter if covers(e) => Some(e)
          case e if Some(e) == d.exit => Some(e)
          case _ => None
        }
      }

    for (e <- out.keySet.toList.sortBy(_.counter))
      yield new ConcTrace(out(e))
  }

  /** Generate code. User constructors are turned into default constructors. */
  def generate(t: Trace): MethodBody = {
    implicit val c = t.c
    val place = t.events.head.asInstanceOf[Enter]
    val events = trimConstructors(t.events.tail)

    // create local environment
    val vars = new mutable.HashMap[Object, Val]

    val arguments =
      for (((o, i), t) <- place.arguments.zipWithIndex zip place.method.parameterTypes)
        yield Var(o, t)

    val receiver = place.receiver match {
      case Some(o) => Some(This(o, o.typ(place.c)))
      case _       => None
    }

    for (v <- arguments ++ receiver)
      vars(v.o) = v

    // translate a used value using declared type
    def use(o: Object, t: Type): Val = (o, t) match {
      case (_, t: PrimitiveType)      => Literal(o, t)
      case (_, UnknownType)           => UnknownVal()
      case (Null, _: InstanceType)    => NullVal
      case (Unknown, _: InstanceType) => UnknownVal()
      case (o, _) if vars.contains(o) => vars(o)
      case (o, _: InstanceType)       => Literal(o, o.typ)
    }

    // translate a used object using its dynamic type
    def refer(o: Object) = use(o, o.typ)

    // translate a produced value to a variable and update local environment
    def produce(o: Object, t: Type): Var = o match {
      case Null | Unknown => Var(o, UnknownType)
      case _ =>
        val v = Var(o, t)
        vars(o) = v
        v
    }

    // translate body
    val stmts = events.flatMap {
      case x: Enter if x.isConstructor && b.isUser(x.method) =>
        New(x, produce(x.receiver.get, x.method.declarer), x.method.declarer, Nil) :: Nil

      case x: Enter =>
        val params = for ((o, t) <- (x.arguments zip x.method.parameterTypes)) yield use(o, t)
        if (x.isConstructor)
          New(x, produce(x.receiver.get, x.method.declarer), x.method.declarer, params) :: Nil
        else
          Call(x, produce(x.returns.getOrElse(Unknown), x.method.returnType), x.receiver.map(refer), x.method, params) :: Nil

      case x: Read =>
        FieldDeref(x, produce(x.value, x.field.typ), x.receiver.map(refer), x.field) :: Nil

      case x: ArrayRead =>
        ArrayDeref(x, produce(x.value, x.arrayType.base), refer(x.receiver), use(x.index, IntType)) :: Nil

      case x: Write =>
        FieldAssignment(x, x.receiver.map(refer), x.field, use(x.value, x.field.typ)) :: Nil

      case x: ArrayWrite =>
        ArrayAssignment(x, refer(x.receiver), refer(x.value), use(x.index, IntType)) :: Nil

      case x: Exit if x.method.returnType == VoidType => Nil

      case x: Exit =>
        Return(x, use(x.value, x.method.returnType), x.method) :: Nil

      case x: Exception =>
        Throw(x, refer(x.exception)) :: Nil
    }

    MethodBody(place, receiver, arguments, stmts)
  }

  /** Place last constructor in place of the first constructor and remove other constructors */
  private def trimConstructors(evs: List[Event]): List[Event] = {
    var top: Map[Object, Enter] = Map()

    // find last constructors
    for (e <- evs.reverse)
      e match {
        case e: Enter if e.isConstructor && !top.contains(e.receiver.get) =>
          top += e.receiver.get -> e
        case _ =>
      }

    // place instead of the first one
    var init: Set[Object] = Set()
    evs.flatMap {
      case e: Enter if e.isConstructor =>
        if (!init.contains(e.receiver.get)) {
          init += e.receiver.get
          Some(top(e.receiver.get))
        } else
          None
      case e => Some(e)
    }
  }

  /** Apply code simplification passes */
  def simplify(bs: List[MethodBody], initial: Set[Event])(implicit p: Parameters): Code = {
    var bodies = bs.map(_
      .replaceThisBySuper
      .addNewArrays)

    var out = MethodBodies(b, initial, bodies)
    var prev = out.size + 1

    // simplification passes
    while (p.Simplify && out.size < prev) {
      debug("simplify: " + prev)
      prev = out.size
      bodies = out.bodies.map {
        case c =>
          var out = c
          out = out.removeDoubleDerefs
          out = out.removeUnusedReturns
          out = out.removeUnusedConstructors
          if (p.RemoveUnusedFrameworkConstructors)
            out = out.removeUnusedFrameworkConstructors
          out = out.removeLocalContainers
          out = out.removeTrivialBody
          out
      }
      out = out.copy(bodies = bodies)
        .removeOwnedFields
        .removeEmpty
    }

    var decls = Code(c, b, out.classes, out.decls)

    if (p.CollapseTypes)
      decls = decls.collapseTypes

    decls
  }
}

trait CachingSynthesizer extends Synthesizer {
  import com.google.common.cache.{ CacheBuilder, Cache }

  val CodeResultCache = 1000

  val out: Cache[(Parameters, List[Seed]), CodeResult] = CacheBuilder.newBuilder()
    .maximumSize(CodeResultCache)
    .build()

  override def compile(seeds: List[Seed])(implicit p: Parameters) = {
    out.get((p, seeds), new Callable[CodeResult] {
      override def call = CachingSynthesizer.super.compile(seeds)
    })
  }
}
