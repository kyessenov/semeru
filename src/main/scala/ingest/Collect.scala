package edu.mit.csail.cap.query
package ingest
import collection.JavaConversions._
import java.io.File
import java.io.FileOutputStream
import java.nio.file.{ Files, Paths }
import java.util.jar._

/** Collect data. Expects to be run in the project root directory  */
object Collector {
  /** Input .class files */
  def CompilerOutput = "target/scala-2.11/classes/"
  def AgentClasses = List(
    "edu/mit/csail/cap/instrument",
    "edu/mit/csail/cap/wire",
    "edu/mit/csail/cap/util",
    "test")

  /** Output JAR file */
  def Agent = System.getProperty("user.dir") + "/agent.jar"

  /** JAR class path; resolved relative to agent JAR */
  def AgentClassPath = List(
    "/lib/tools.jar",
    "/lib/asm-all-5.0.4.jar",
    "/lib/internal-trove3.jar").map(System.getProperty("user.dir") + _)

  /** VM parameters to enable Java agent */
  def javaOptions(config: String) = List(
    "-ea:edu.mit.csail.cap...",
    //  Java 8 removed the option below
    //    "-XX:-UseSplitVerifier",
    "-Xverify:none",
    s"-javaagent:$Agent=$config")

  /** List agent .class files */
  def agentClasses: List[String] = AgentClasses.flatMap(classes)
  def classes(root: String): List[String] = {
    for (
      rel <- new File(CompilerOutput + root).list.toList;
      name = root + "/" + rel;
      f = new File(CompilerOutput + name)
    ) yield if (f.isDirectory)
      classes(name)
    else
      List(name)
  }.flatten

  /** Create JAR with the agent binaries */
  def makeJAR {
    // create manifest
    val main = classOf[edu.mit.csail.cap.instrument.Agent].getName
    val manifest = new Manifest
    val attrs = manifest.getMainAttributes
    attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0.0")
    attrs.put(new Attributes.Name("Can-Retransform-Classes"), "true")
    attrs.put(new Attributes.Name("Boot-Class-Path"), (Agent :: AgentClassPath).mkString(" "))
    attrs.put(new Attributes.Name("Premain-Class"), main)
    attrs.put(new Attributes.Name("Agent-Class"), main)
    attrs.put(new Attributes.Name("Main-Class"), main)

    debug("writing agent JAR file")

    // write file
    val out = new JarOutputStream(new FileOutputStream(new File(Agent)), manifest)
    for (s <- agentClasses) {
      val entry = new JarEntry(s)
      out.putNextEntry(entry)
      out.write(Files.readAllBytes(Paths.get(CompilerOutput + s)))
      out.closeEntry()
    }

    out.close()
  }

  def launch(config: String, exe: List[String], cmd: String*): Process = {
    makeJAR
    val path = new File(config).getAbsolutePath
    val p = new ProcessBuilder
    p.inheritIO
    p.directory(null)
    p.command(exe ++ javaOptions(path) ++ cmd)
    info(p.command.mkString(" "))
    p.start
  }

  def runMain(config: String, main: String, cp: String) =
    launch(config, List("java"), "-cp", cp, main)

  def runJAR(config: String, jar: String) =
    launch(config, List("java"), "-jar", jar)

  def attach(config: String, pid: Int) =
    launch(config, List("java"), "-jar", Agent, config, pid.toString, Agent)
}
