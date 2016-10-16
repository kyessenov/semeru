package test
import edu.mit.csail.cap.query._

import db._
import scala.util.Random

object ObjectTypePerf {
  def main(args: Array[String]) {
    val metadata = args(0)
    val db = args(1)
    val localhost = Database()
    val meta = new Metadata(new Errors)
    meta.load(localhost, metadata)
    val c = localhost.connect(db, meta)

    info("reading all objects")
    val objects = c.read("select DISTINCT(id) from OBJECTS")(_.getInt(1)).toVector
    info("number: " + objects.size)

    info("getting heap DB")
    val heap = c.heaps

    val SAMPLE = 10000
    var s = List[Object]()

    info(s"sampling $SAMPLE objects")
    for (i <- 0 until SAMPLE)
      s ::= objects(Random.nextInt(objects.size))

    info("measuring Neo4J")
    timed {
      for (o <- s)
        heap.has(o)
    }

  }
}
