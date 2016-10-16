package test

import org.scalatest.FunSuite

import edu.mit.csail.cap.query._
import edu.mit.csail.cap.query.analysis._
import edu.mit.csail.cap.query.db._
import edu.mit.csail.cap.query.ingest._
import edu.mit.csail.cap.query.web._
import edu.mit.csail.cap.query.experiments._

class MeasureHeaps extends FunSuite  {
  implicit val p = Parameters()
  val data = web.DataProvider(p)

  ignore("collect heap sizes") {
    var sizes: List[(String, Int, Int)] = Nil
    for (trace <- data.traces) {
      val h = trace.heaps;
      val n = h.numNodes;
      val m = h.numEdges;
      trace.shutdown();
      sizes ::= (trace.name, n, m);
    }
    
    println("CSV data:")
    println("name, nodes, edges");
    for ((name, nodes, edges) <- sizes)
      println(s"$name, $nodes, $edges")
  }
}
