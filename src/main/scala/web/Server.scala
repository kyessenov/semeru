package edu.mit.csail.cap.query
package web

import org.eclipse.jetty.server.{ Server => jettyServer }
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.ScalatraServlet
import org.scalatra.Ok
import org.scalatra.scalate.ScalateSupport
import org.scalatra.servlet.{ FileUploadSupport, MultipartConfig, SizeConstraintExceededException }
import db.Database

case class Server(data: DataProvider) extends ScalatraServlet with ScalateSupport with FileUploadSupport {
  private val server = {
    val server = new jettyServer(8080)
    val context = new WebAppContext()
    context.setResourceBase("web")
    context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", "")
    context.setAttribute("org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern", "")
    context.addServlet(new ServletHolder(new REPL(data)), "/repl/*")
    context.addServlet(new ServletHolder(new Meta(data.meta)), "/meta/*")

    val holder = new ServletHolder(this)
    holder.getRegistration.setMultipartConfig(
      MultipartConfig(
        maxFileSize = Some(100 * 1024 * 1024),
        fileSizeThreshold = Some(10 * 1024 * 1024),
        location = Some(System.getProperty("user.dir"))).toMultipartConfigElement)
    context.addServlet(holder, "/*")

    server.setHandler(context)
    server
  }

  def start() {
    server.start()
  }

  def stop() {
    data.shutdown()
    server.stop()
  }

  post("/collect") {
    val log = params("log")
    val exp = experiments.Experiments.All.find(_.name == params("app")).get
    data.LogApp = Some(exp)
    data.LogName = Some(log)
    exp.collect(log.startsWith("demo"), log)
    redirect("/experiments")
  }

  def sendAgent(msgs: String*) {
    import java.net._
    val socket = new Socket("localhost", 13338)
    val out = new java.io.PrintStream(socket.getOutputStream)
    for (msg <- msgs)
      out.println(msg)
    out.println("exit")
    out.close()
  }

  post("/start") {
    assert(!data.LogApp.isEmpty, "need to start an app first")
    val f = java.io.File.createTempFile("log", ".bin", null)
    f.deleteOnExit()
    data.LogBin = Some(f.getAbsolutePath)
    data.LogName = Some(params("log"))
    
    // wait 2 seconds for the user to switch focus back to target application
    info("waiting 500ms before sending activation command")
    java.lang.Thread.sleep(500);
    
    info("sending command")
    sendAgent(
      "out " + data.LogBin.get,
      "start")
    redirect("/experiments")
  }

  post("/stop") {
    assert(!data.LogApp.isEmpty, "need to start an app first")
    assert(!data.LogBin.isEmpty, "need to start recording first")
    assert(!data.LogName.isEmpty, "need to name log first")
    sendAgent(
      "stop",
      "flush")

    val app = data.LogApp.get
    val log = data.LogName.get

    data(log).map(_.shutdown)
    ingest.Processor.run(log, app.meta, data.LogBin.get)

    data.LogBin = None
    data.LogName = None
   
    var config = TraceConfig(
      name = log,
      user = app.user,
      framework = app.framework)
      
    experiments.Experiment.parse(log) match {
      case Some(app1) if app1 == app =>
      case _ => config = config.copy(customMeta = Some(app.meta))
    }
  
    data.add(config)

    redirect(s"/demomatch?t=${log}&target=swing_components")
  }
  
  post("/remove") {
    data.remove(params("t"))
  }
  
  post("/drop") {
    data.db.drop(params("t"))
  }

  post("/upload") {
    val LogBin = "log.bin"
    val MetadataBin = "metadata.bin"
    fileParams("logbin").write(LogBin)
    fileParams("metadatabin").write(MetadataBin)

    val log = params("log")
    val metadata = params("metadata")

    // drop connection to trace config
    data(log).map(_.shutdown)

    // process files
    ingest.Processor.run(log, metadata, LogBin, MetadataBin)

    redirect(url("/demomatch", Map("t" -> log, "g" -> "swing_components")))
  }

  /*** Pages */

  get("/") {
    redirect("/repl/")
  }

  get("/experiments") {
    contentType = "text/html"
    ssp("experiments", "data" -> data)
  }
  
  get("/demomatch") {
    contentType = "text/html"
    ssp("demomatch", "data" -> data)
  }

  get("/search") {
    contentType = "text/html"
    ssp("search", "data" -> data)
  }

  get("/upload") {
    contentType = "text/html"
    ssp("upload", "data" -> data)
  }
  
  get("/matchmaker") {
    contentType = "text/html"
    ssp("matchmaker",
      "data" -> data,
      "db" -> params.get("db"),
      "src" -> params.get("src"),
      "dst" -> params.get("dst"))
  }
}

object Server {
  def main(args: Array[String]) {
    val s = Server(DataProvider(Parameters(EagerLoadMetadata = true)))

    sys.addShutdownHook {
      info("terminating server...")
      s.stop()
    }

    info("starting server... [Press ENTER to terminate]")
    s.start()

    if (io.StdIn.readLine() != null) {
      info("terminate on ENTER")
      sys.exit()
    }
  }
}
