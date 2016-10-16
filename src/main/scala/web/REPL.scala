package edu.mit.csail.cap.query
package web

import org.scalatra.ScalatraServlet
import scala.tools.reflect.ToolBoxError
import scala.tools.reflect.ToolBox
import org.scalatra.NotFound
import analysis._
import org.scalatra.scalate.ScalateSupport

class REPL(val data: DataProvider)
    extends ScalatraServlet with ScalateSupport {
  /** All requests pass name as a parameter */
  def db = data(params("db")).get
  def e = db.at(params("e").toInt)
  implicit def ctx = RenderContext(db.b)
  
  before() {
    contentType = "text/html"
  }

  get("/") { 
    // pass "layout" -> "" to disable the global template
    ssp("repl", "data" -> data)
  }

  get("/run") {
    val q = params("q")
    debug(s"eval: $q")

    data.render(try {
      REPL.eval(db, q)
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        e
    })
  }

  get("/expand") {
    data.render(db.select(e.asInstanceOf[Enter].children))
  }

  get("/parent") {
    data.render(e.parent.get)
  }

  get("/slice") {
    val o = params("o").toInt
    data.render(db.rule.expandAll(Seed(e, o)))
  }

  get("/compile") {
    data.render(db.compile(multiParams("e").toList.flatMap {
      case e => db.at(e.toInt).seeds
    })(data.p))
  }

  override def isScalateErrorPageEnabled = false
}

object REPL {
  private lazy val toolbox = scala.reflect.runtime.currentMirror.mkToolBox()

  val Prelude =
    """|import edu.mit.csail.cap.query._
       |import edu.mit.csail.cap.query.analysis._
       |import edu.mit.csail.cap.query.experiments._
       |import edu.mit.csail.cap.query.web.REPL._
       |val a = edu.mit.csail.cap.query.web.REPL.current
       |import a._
       |""".stripMargin

  var current: TraceConfig = _

  import scala.language.implicitConversions
  implicit def toGroup(traces: List[TraceAnalysis]) = Group(traces)
  
  def eval(a: TraceConfig, q: String) = {
    REPL.current = a
    toolbox.eval(toolbox.parse(Prelude + q))
  }
}

