package test

import java.nio.file.Files
import edu.mit.csail.cap.query._
import analysis.CodeResult
import java.nio.file.Paths
import org.scalatest.Assertions.fail
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import analysis._
import experiments._

/** Test driver for comparing synthesized result with the baseline */
trait SynthesisTest extends FunSuite with BeforeAndAfterAll with CodeComparison {
  /** Load metadata */
  def metadata: String

  /** Default parameters */
  def default = Parameters(Matches = 1)

  /** Data provider */
  var data: web.DataProvider = null

  /** Accumulate statistics */
  var stats: List[(String, Map[String, String])] = Nil

  override def beforeAll() {
    data = web.DataProvider(default)
    data.meta.load(data.db, metadata)
  }

  override def afterAll() {
    data.shutdown()

    if (stats.size > 0) {
      val columns = stats.head._2.keys.toList.sorted
      println("CSV data:")
      println("," + columns.mkString(","))
      for ((row, data) <- stats) {
        print(row)
        for (col <- columns)
          print(f",${data.getOrElse(col, "")}%8s")
        println()
      }
    }
  }

  /** Test by comparing against the baseline. Uses just one output */
  def run(trace: String, key: String, p: Parameters = default) {
    val q = Queries.get(key, data.meta)

    data(trace) match {
      case Some(t) =>
        val result :: _ = t.synthesize(q)(p)

        stats ::= s"$trace-$key" ->
          (result.counters.mapValues(_.toString) ++ result.durations.mapValues(_.toString))

        compare(result, p, s"$trace-$key")
      case None =>
        fail(s"missing trace $trace")
    }
  }

  /** Test by comparing against the baseline. Uses just one output */
  def runAll(traces: String, key: String, p: Parameters = default) {
    val q = Queries.get(key, data.meta)
    val ts = data.get(traces).traces
    val codes = ts.flatMap(_.synthesize(q)(p))
    val printed = codes.groupBy(_.code.print(p))

    info(s"${codes.size} snippets, ${printed.keys.size} groups")

    val snippets =
      for ((code, results) <- printed)
        yield results.map(_.c.name).toList.sorted.mkString("// ", ", ", "\n") + code

    for (result <- codes)
      stats ::= s"${result.c.name}-$key-${result.start.map(_.e.counter).toList.sorted.mkString("-")}" ->
        (result.counters.mapValues(_.toString) ++ result.durations.mapValues(_.toString))

    val out = snippets.toList.sorted.mkString("\n====\n")
    val in = Paths.get(s"$prefix/$traces-$key.java")
    val baseline = if (Files.exists(in)) new String(Files.readAllBytes(in)) else ""

    if (baseline !== out) {
      Files.write(Paths.get(s"$prefix/FAIL-$traces-$key.java"), out.getBytes)
      fail(s"$traces $key code changed")
    }
  }
}

trait CodeComparison {
  def prefix = "data/baseline"

  def load(name: String): Set[String] = {
    val in = Paths.get(s"$prefix/$name.java")

    if (Files.exists(in))
      new String(Files.readAllBytes(in)).replaceAll("\\s+", "").split("====").toSet
    else
      Set()
  }

  /** Compare result against a file */
  def compare(r: CodeResult, p: Parameters, name: String) {
    val baselines = load(name)

    val result = r.code.print(p)
    val out = Paths.get(s"$prefix/$name.java.out")

    if (!baselines(result.replaceAll("\\s+", ""))) {
      warn(result)
      Files.write(out, result.getBytes)
      fail(s"$name code changed")
    } else if (Files.exists(out)) {
      Files.delete(out)
    }
  }

  def compareText(r: CodeResult, baseline: String) {
    val result = r.code.print(Parameters(PrintPrimitives = false, PrintSourceSymbols = false, PrintStrings = false))

    if (result.replaceAll("\\s+", "") != baseline.replaceAll("\\s+", "")) {
      warn(result)
      fail("code changed")
    }
  }

}

