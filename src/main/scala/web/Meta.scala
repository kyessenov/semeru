package edu.mit.csail.cap.query
package web

import org.scalatra.ScalatraServlet
import xml.{ NodeSeq, Group, Text }

/** Provides access to meta-data. */
class Meta(val meta: Metadata)
    extends ScalatraServlet with Render {

  before() {
    contentType = "text/html"
  }

  implicit def ctx = RenderContext(AllFramework)

  get("/method/:id") {
    val id = params("id").toLong
    val m = meta.method(id)
    Group {
      prefix(m) ++ Text(" ") ++
        render(m.returnType) ++ Text(" ") ++
        render(m.declarer) ++
        Text("." + m.name + "(") ++
        commas(m.parameterTypes.map(render)) ++
        Text(s"): $id") ++ {
          m.overrides match {
            case Some(n) => <br/> ++ keyword(" overrides ") ++ render(n)
            case None    => NodeSeq.Empty
          }
        }
    }
  }

  get("/field/:id") {
    val f = meta.field(params("id").toLong)
    Group {
      prefix(f) ++ Text(" ") ++
        render(f.typ) ++ Text(" ") ++
        render(f.declarer) ++
        Text("." + f.name)
    }
  }

  get("/type/:id") {
    val t = meta.typ(params("id").toLong)
    Group {
      t match {
        case t: ClassType =>
          prefix(t) ++ Text(" ") ++
            Text(t.name) ++
            {
              val supers = t.supertypes
              if (supers.size > 0)
                Text(" ") ++ keyword("extends") ++ Text(" ") ++ supers.flatMap(render(_))
              else
                NodeSeq.Empty
            }
        case _ => Text(t.name)
      }
    }
  }
}

