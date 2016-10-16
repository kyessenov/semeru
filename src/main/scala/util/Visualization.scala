package edu.mit.csail.cap.query
package util

import scala.collection.mutable

object GraphViz {
  import java.io.{ PrintStream, File }

  def write[V, L](g: Graph[V, L], out: PrintStream) {
    out println "digraph tmp {"

    val nodes = new mutable.HashMap[V, Int]

    def get(n: V) = nodes.get(n) match {
      case Some(i) => "n" + i
      case None =>
        val i = nodes.size
        nodes(n) = i
        "n" + i
    }

    def text(n: Any): String = n match {
      case (a, b)            => text(a) + ": " + text(b)
      case n: Traversable[_] => n.map(text).mkString(",\n ")
      case m: Method         => m.declarer.shortName + "." + m.name
      case _                 => n.toString
    }

    for (node <- g.nodes)
      out.println("\"" + get(node) + "\"" +
        "[label=\"" + text(node) + "\" tooltip=\"" + node + "\" fontsize=\"10px\" shape=\"box\" style=\"rounded\"]")

    // Graph edges
    for (edge <- g) {
      out print "\""
      out print get(edge.from)
      out print "\"->\""
      out print get(edge.to)
      out print "\" [label=\""
      out print (edge.l match {
        case () => ""
        case l  => l.toString
      })
      out println "\"]"
    }

    out println "}"
  }

  private[this] def makeTempDot = {
    // write to a file
    val f = File.createTempFile("graph", ".dot")
    f.deleteOnExit
    info("created dot file " + f.getAbsolutePath)
    f
  }

  private[this] def executeDot(in: File) = {
    // write to a png file
    val out = File.createTempFile("graph", ".svg")
    out.deleteOnExit
    val dot = Runtime.getRuntime.exec("dot -Tsvg -o " + out.getAbsolutePath + " " + in.getAbsolutePath)

    if (dot.waitFor != 0)
      info("dot failed to produce: " + out.getAbsolutePath)

    out
  }

  private[this] def showDot(out: File) {
    Runtime.getRuntime.exec("eog " + out.getAbsolutePath)
  }

  def createDot[V, L](g: Graph[V, L]): File = {
    val f = makeTempDot
    val out = new PrintStream(f)
    write(g, out)
    out.flush
    executeDot(f)
  }

  def display[V, L](g: Graph[V, L]) {
    val f = createDot(g)
    showDot(f)
  }
}

object JFreeChart {
  import org.jfree.chart._
  import org.jfree.chart.plot._
  import org.jfree.data.statistics._
  import javax.swing.JFrame
  import javax.swing.WindowConstants

  def histogram(name: String, xlabel: String, ylabel: String,
                data: Traversable[Double], bins: Int) {
    val dataset = new HistogramDataset
    dataset.addSeries("data", data.map(_.toDouble).toArray, bins)

    val chart = ChartFactory.createHistogram(name, xlabel, ylabel, dataset,
      PlotOrientation.VERTICAL,
      false, false, false)

    val panel = new ChartPanel(chart)

    val frame = new JFrame(name)
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    frame.add(panel)
    frame.pack
    frame.setVisible(true)
  }

  def degrees[V, L](g: Graph[V, L]) =
    histogram("Degrees", "degree", "# of nodes", g.nodes.map(g.outbound(_).size.toDouble), 1000)

  /**
   * Show number of ranges in the intervals with the given low threshold.
   */
  def ranges[V](g: Graph[V, NormalForm]) =
    histogram("Ranges", "# of ranges", "# of nodes",
      g.map(_.l.segments.size.toDouble), 1000)
}
