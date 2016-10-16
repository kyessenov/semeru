package edu.mit.csail.cap.query
package web

import util._
import analysis._
import scala.tools.reflect.ToolBoxError
import scala.collection.mutable
import xml.{ NodeSeq, Elem, Text, Unparsed }
import NodeSeq.Empty
import analysis.ast._

object Render {
  private var ID = 0L
  def makeID: Long = {
    ID = ID + 1
    ID
  }
  var MaxEvents = 100
  var MaxGraph = 1000
  var MaxTree = 10000
}

case class RenderContext(
  b: FrameworkBoundary,
  print: Boolean = true)

/** Generic pretty-printer to HTML */
trait Render extends util.JSON {
  import Render._

  def keyword(k: String) = <div class='keyword'>{ k }</div>

  def primitive(k: String) = <div class='primitive'>{ k }</div>

  def prefix(m: AccessMask) = keyword(m.prefix)

  def commas(l: Seq[NodeSeq]) =
    if (l.size > 0)
      l.reduce(_ ++ Text(", ") ++ _)
    else
      Empty

  /** Render an object */
  def render(o: Object, t: Type, c: Connection): NodeSeq =
    (o, t) match {
      case (_, t: PrimitiveType)      => primitive(t.valueOf(o))
      case (_, UnknownType)           => Text(o.toString)
      case (Null, _) | (_, NullType)  => keyword("null")
      case (Unknown, t: InstanceType) => keyword("??")
      case (o, t: InstanceType) =>
        c.typeOf(o) match {
          case c.meta.StringClass =>
            <div data-id={ o.toString } class='object'>{
              c.stringValue(o) match {
                case Some(s) => "\"" + s.replace("\n", "\\n") + "\""
                case _       => "String@" + o
              }
            }</div>
          case _ =>
            <div data-id={ o.toString } class='object' title={ t.toString }>{ t.prettyName + "@" + o }</div>
        }
    }

  def render(o: Object, c: Connection): NodeSeq =
    render(o, c.typeOf(o), c)

  /** Render anything */
  def render(o: Any)(implicit ctx: RenderContext): NodeSeq = o match {
    case () | Nil | "" =>
      Empty

    case t: PrimitiveType =>
      keyword(t.name)

    case t: ArrayType =>
      render(t.base) ++ Text("[]")

    case t: Type =>
      <div data-id={ t.id.toString } class='type'>{ t.shortName }</div>

    case m: Method =>
      render(m.declarer) ++ Text(".") ++ <div data-id={ m.id.toString } class='method'>{ m.name }</div>

    case f: Field =>
      <div data-id={ f.id.toString } class='field'>{ if (f.isStatic) f.declarer.name + "." + f.name else f.name }</div>

    case e: Event =>
      <div data-id={ e.counter.toString } data-db={ e.c.name } class={
        if (ctx.b.isUser(e)) "event user" else "event"
      }>
        <span class='event-label'>{ e.thread + "|" + e.counter }</span>{
          e match {
            case e: Enter =>
              (e.receiver match {
                case Some(o)=> render(o, e.method.declarer, e.c)
                case _ => render(e.method.declarer)
              }) ++
                "." ++
                <div data-id={ e.method.id.toString } class='method'>{ e.method.name }</div> ++
                "(" ++
                commas(for ((v, t) <- e.arguments zip e.method.parameterTypes) yield render(v, t, e.c)) ++
                ")" ++
                // add returns value
                (e.method.returnType match {
                  case VoidType=> ""
                  case rt => ": " ++ render(e.returns.getOrElse(Unknown), rt, e.c)
                })
            case e: Exit =>
              <xml:group>{ keyword("return") } { render(e.value, e.method.returnType, e.c) }</xml:group>
            case e: Exception =>
              <xml:group>{ keyword("throw") } { render(e.exception, e.c) }</xml:group>
            case e: Read =>
              render(e.value, e.c) ++ " = " ++
                (e.receiver match { case Some(o) => render(o, e.c) ++ "." case _ => Empty }) ++ render(e.field)
            case e: Write =>
              (e.receiver match { case Some(o) => render(o, e.c) ++ "." case _ => Empty }) ++ render(e.field) ++
                " = " ++ render(e.value, e.c)
            case e: ArrayRead =>
              render(e.value, e.c) ++ " = " ++ render(e.receiver, e.c) ++ Text("[" + e.index.toString + "]")
            case e: ArrayWrite =>
              render(e.receiver, e.c) ++ "[" ++ e.index.toString ++ "] = " ++ render(e.value, e.c)
          }
        }
      </div>

    case Seed(e, o) =>
      <div data-e={ e.counter.toString } data-o={ o.toString } data-db={ e.c.name } class='seed'>
        { render(o, e.c) ++ "|" ++ render(e) }
      </div>

    case t: TraceConfig =>
      <code>{ t.name }</code> ++ (t.desc match {
        case Some(html) => Text(": ") ++ Unparsed(html)
        case _          => Empty
      })

    case t: CallTrace =>
      render(t.roots)

    case t: Trace =>
      <div class='trace' title={ t.toString }>
        { render(t.take(MaxEvents)) }
        { if (t.size > MaxEvents) (t.size - MaxEvents) + " events remaining" }
      </div>

    case doc: MethodDocumentation =>
      <div class='javadoc'>{
        doc.documentation ++
          (doc.returnValue match {
            case Some(s) => <p><b>Returns: </b>{ s }</p>
            case _       => Empty
          }) ++ (doc.parameters.map {
            case (k, v) => <p><b>{ k }</b>{ v }</p>
          }) ++ (doc.exceptions.map {
            case e => <p>{ e }</p>
          })
      }</div>

    case r: CodeResult =>
      implicit val env = r.code.makeEnv(Parameters(
        PrintSourceSymbols = ctx.print,
        PrintStrings = ctx.print,
        PrintPrimitives = ctx.print))
      implicit val result = r
      <div class='code-result'>
        <div class='code-note'>{ for ((k, v) <- r.stats) yield <xml:group><b>{ k }</b>: <i>{ v }</i>; </xml:group> }{ render(r.start.map(_.e).toSet) }</div>
        { r.code.declarations.map(renderAST) }
      </div>

    case Concept(os, as) =>
      <xml:group>
        <div class='col-md-6'>{ render(os) }</div>
        <div class='col-md-6'>{ render(as) }</div>
      </xml:group>

    case t: Tree[_] =>
      if (t.numNodes < MaxTree) {
        <div class='tree'>
          <span class='tree-toggle'>+</span>
          { render(t.data) }
          <div class='tree-children'>{ t.children.map(render(_)) }</div>
        </div>
      } else
        Text(t.toString)

    case g: Graph[_, _] =>
      if (g.numNodes < MaxGraph) {
        val i = makeID
        val ns = g.nodes.toList
        val m = ns.zipWithIndex.toMap
        val es = g.map { case Edge(from, l, to) => (m(from), render(l).toString, m(to)) }.toList
        val ls = ns.map {
          case Concept(o, a) => "" + a.size
          case _ => ""
        }
        
        <div class='dagre' id={ "i" + i }>
          <svg><script>{ s"drawGraph(${Unparsed(toJSON(es))}, ${Unparsed(toJSON(ls))}, ${ns.size}, 'i$i');" }</script></svg>
          { for ((n, i) <- m) yield <div class={ s"dagre-node v$i" }>{ render(n) }</div> }
        </div>
      } else
        Text(g.toString)

    case e: Edge[_, _] =>
      render(e.from) ++ render(e.l) ++ render(e.to)

    case l: Label =>
      render(l.field) ++ render(l.interval)

    case i: Interval =>
      Text(i.normalize.toString)

    case p: ClassMask =>
      Text(p.toString)

    case p: Path[_] =>
      render(p.labels)

    case r: SliceRule =>
      Text(r.toString)

    case t: ToolBoxError =>
      <code>{ t.message }</code>

    case t: java.lang.reflect.InvocationTargetException =>
      <code>Invocation { render(t.getCause) }</code>

    case t: Throwable =>
      <code>{ t.getClass.getName } : { t.getMessage }</code>

    case l: Traversable[Any] if l.size == 0 =>
      Empty

    case l: Traversable[Any] if l.size == 1 =>
      render(l.head)

    case l: Traversable[Any] =>
      <table class='table table-condensed table-hover'>{
        for ((elt, i) <- l.toList.zipWithIndex) yield <tr>{
          elt match {
            case (x, y) =>
              <td>{ render(x) }</td><td>{ render(y) }</td>
            case (x, y, z) =>
              <td>{ render(x) }</td><td>{ render(y) }</td><td>{ render(z) }</td>
            case (x, y, z, t) =>
              <td>{ render(x) }</td><td>{ render(y) }</td><td>{ render(z) }</td><td>{ render(t) }</td>
            case (x, y, z, t, u) =>
              <td>{ render(x) }</td><td>{ render(y) }</td><td>{ render(z) }</td><td>{ render(t) }</td><td>{ render(u) }</td>
            case x =>
              <th class='number'>{ i + 1 }</th><td>{ render(x) }</td>
          }
        }</tr>
      }</table>

    case Some(o)   => render(o)

    case (x, y)    => render(x) ++ Text(",") ++ render(y)

    case (x, y, z) => render(x) ++ Text(",") ++ render(y) ++ Text(",") ++ render(z)

    case p: Product => p.productArity match {
      case 0 => keyword(p.productPrefix)
      case 1 => keyword(p.productPrefix) ++ Text(" ") ++ render(p.productElement(0))
      case _ => keyword(p.productPrefix) ++ Text(" ") ++
        <xml:group>{ { for (elt <- p.productIterator) yield render(elt) }.reduce(_ ++ Text(",") ++ _) }</xml:group>

    }

    case s: String if s.contains("\n") =>
      <pre>{ s }</pre>

    case _ => Text(o.toString)
  }

  def variable(v: Val, s: String) =
    <div data-id={ v.o.toString } class='object' title={ v.typ.toString }>{ s }</div>

  /** Textual representation of a syntax element */

  def renderAST(elt: JavaElement)(implicit env: Environment): NodeSeq =
    elt match {
      case t: PrimitiveType => keyword(t.name)
      case _ =>
        <div data-id={ elt.id.toString } class={
          elt match {
            case _: Method => "method"
            case _: Field  => "field"
            case _: Type   => "type"
          }
        }>{ env(elt) }</div>
    }

  def renderAST(s: Declaration)(implicit env: Environment, result: CodeResult): NodeSeq = s match {
    case decl: ClassDecl if decl.isEmpty => Empty

    case decl: ClassDecl =>
      <div class='class-decl' title={ decl.collapsed.map(_.name).mkString(", ") }>
        { keyword("class") ++ " " }{ renderAST(decl.t) ++ " " }{
          if (!decl.abstracts.isEmpty)
            keyword("extends") ++ " " ++ commas(decl.abstracts.sortBy(env).map(renderAST))
        }{
          if (!decl.interfaces.isEmpty)
            keyword("implements") ++ " " ++ commas(decl.interfaces.sortBy(env).map(renderAST)) 
        }{"{"}{
          decl.fields.toList.sortBy(env).map { f =>
            <div class='member-decl'>{
              (if (f.isStatic) keyword("static") ++ " " else Empty) ++ renderAST(f.typ) ++ " " ++ renderAST(f) ++ ";"
            }</div>
          }
        }{
          decl.methods.toList.sortBy(env).map(m => renderAST(decl.declaration(m)))
        }
        }}
      </div>

    case s: MethodDecl =>
      <div class='member-decl'>
        {
          (if (env.b.overrides(s.method).isDefined) keyword("@Override") ++ Text(" ") else Empty) ++
            (if (s.method.isStatic) keyword("static") ++ Text(" ") else Empty) ++
            (if (!s.method.isConstructor)
              renderAST(s.method.returnType) ++ Text(" ") ++ renderAST(s.method)
            else
              renderAST(s.clazz.t)) ++
            Text("(") ++
            commas(
              (0 until s.method.arguments).map {
                case i =>
                  renderAST(s.method.parameterTypes(i)) ++ Text(s" a$i")
              }) ++ Text(") {")
        }{ s.versions.map(renderAST) }
        }}
      </div>

    case m: MethodBody =>
      val local = env.make(m)

      // output edges as attributes
      <div class='stmts'>{
        for (s <- m.stmts)
          yield <div class={
          if (result.initialStmts(s)) "stmt initial" else "stmt"
        }>{
          renderAST(s)(local)
        }; <span class={
          "s" + result.stmts(s) + " stmt-label"
        } data-in={
          result.depends.inbound(s).map {
            case (t, _) => "s" + result.stmts(t)
          }.mkString(" ")
        } data-out={
          result.depends.outbound(s).map {
            case (t, _) => "s" + result.stmts(t)
          }.mkString(" ")
        } data-e={ s.e.counter.toString } data-db={ s.e.c.name }>{
          result.depth.getOrElse(s.e, -1)
        }</span></div>
      }</div>
  }

  def renderAST(s: Statement)(implicit env: Environment): NodeSeq = s match {
    case s: NewArray =>
      <xml:group>{ renderAST(s.result) } = { keyword("new") } { renderAST(s.base) }[??]</xml:group>

    case s: New =>
      <xml:group>{ renderAST(s.result) } = { keyword("new") } { renderAST(s.constructor) }({ commas(s.params.map(renderAST)) })</xml:group>

    case s: Call =>
      <xml:group>{
        (s.result, s.method.returnType) match {
          case (_, VoidType)                  => Empty
          case (v, _) if v.typ == UnknownType => Empty
          case _                              => <xml:group>{ renderAST(s.result) } = </xml:group>
        }
      }{
        s.receiver match {
          case Some(_: This) => Empty
          case Some(v @ Var(_, _, t: ClassType)) if !t.isSubtype(s.method.definition.declarer) =>
            <xml:group>(({ renderAST(s.method.declarer) }) { renderAST(v) }).</xml:group>
          case Some(v) =>
            <xml:group>{ renderAST(v) }.</xml:group>
          case None =>
            <xml:group>{ renderAST(s.method.declarer) }.</xml:group>
        }
      }{ renderAST(s.method) }({ commas(s.params.map(renderAST)) })</xml:group>

    case s: FieldDeref =>
      <xml:group>{ renderAST(s.result) } = {
        s.receiver match {
          case Some(v @ Var(_, _, t: ClassType)) if !t.isSubtype(s.field.declarer) =>
            <xml:group>(({ renderAST(s.field.declarer) }) { renderAST(v) })</xml:group>
          case Some(v) => renderAST(v)
          case None    => renderAST(s.field.declarer)
        }
      }.{ renderAST(s.field) }</xml:group>

    case s: FieldAssignment =>
      <xml:group>{
        s.receiver match {
          case Some(v @ Var(_, _, t: ClassType)) if !t.isSubtype(s.field.declarer) =>
            <xml:group>(({ renderAST(s.field.declarer) }) { renderAST(v) })</xml:group>
          case Some(v) => renderAST(v)
          case None    => renderAST(s.field.declarer)
        }
      }.{ renderAST(s.field) } = { renderAST(s.v) }</xml:group>

    case s: ArrayDeref =>
      <xml:group>{ renderAST(s.result) } = { renderAST(s.receiver) }[{ renderAST(s.index) }]</xml:group>

    case s: ArrayAssignment =>
      <xml:group>{ renderAST(s.receiver) }[{ renderAST(s.index) }] = { renderAST(s.v) }</xml:group>

    case s: Return =>
      <xml:group>{ keyword("return") } { renderAST(s.v) }</xml:group>

    case t: Throw =>
      <xml:group>{ keyword("throw") } { renderAST(t.v) }</xml:group>
  }

  def renderAST(v: Val)(implicit env: Environment): NodeSeq = v match {
    case v: Var =>
      env.local.get(v) match {
        case Some(name) => variable(v, name)

        case None =>
          val prefix = renderAST(v.typ).text.filter(_.isUpper) match {
            case "" => "v"
            case s  => s.toLowerCase
          }

          val name = Var.fresh(prefix, env.local.values.toSet)

          // update local environment with the name
          env.local(v) = name

          <xml:group>{ renderAST(v.typ) } { variable(v, name) }</xml:group>
      }

    case _: This       => keyword("this")
    case _: Super      => keyword("super")
    case NullVal       => keyword("null")
    case _: UnknownVal => keyword("??")
    case Literal(o, t) => t match {
      case _: PrimitiveType if env.p.PrintPrimitives => render(o, t, env.c)
      case env.c.meta.StringClass if env.p.PrintStrings => render(o, t, env.c)
      case _ => keyword("??")
    }
  }
}

trait TextRender extends Render {
  def text(s: Syntax)(implicit env: Environment): String = s match {
    case s: ClassDecl if s.isEmpty => ""

    case s: ClassDecl =>
      "class " + env(s.t) + {
        if (!s.abstracts.isEmpty)
          " extends " + s.abstracts.sortBy(env).map(env).mkString(", ")
        else
          ""
      } + {
        if (!s.interfaces.isEmpty)
          " implements " + s.interfaces.sortBy(env).map(env).mkString(", ")
        else
          ""
      } + " {\n" + {
        s.fields.toList.sortBy(env).map { f =>
          "  " +
            (if (f.isStatic) "static " else "") +
            env(f.typ) + " " + env(f) + ";\n"
        }
      }.mkString("") +
        s.methods.toList.sortBy(env).map { m => text(s.declaration(m)) }.mkString("") +
        "}"

    case s: MethodDecl =>
      "  " + {
        if (s.method.isStaticInitializer)
          ""
        else {
          (if (env.b.overrides(s.method).isDefined) "@Override " else "") +
            (if (s.method.isStatic) "static " else "") +
            (if (s.method.isConstructor)
              env(s.clazz.t)
            else
              env(s.method.returnType) + " " + env(s.method)) +
            "(" +
            (0 until s.method.arguments).map {
              case i =>
                env(s.method.parameterTypes(i)) + s" a$i"
            }.mkString(", ") +
            ")"
        }
      } + " {\n" +
        s.versions.map(text).mkString("    // --\n") +
        "  }\n"

    case s: MethodBody =>
      val vars = env.make(s)
      s.stmts.map(stmt => "    " + text(stmt)(vars) + ";\n").mkString("")

    case s: Statement => renderAST(s).text

    case s: Val       => renderAST(s).text
  }
}
