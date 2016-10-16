package test

import edu.mit.csail.cap.query._
import util._
import org.scalatest._
import java.util.concurrent.Executors

class IntervalSpec extends FunSuite {
  val POOL_SIZE = 4
  // Increase N for a larger scope of model checking
  val N = 2

  val intervals: Seq[Interval] =
    (for (i <- 0 to N; j <- i + 1 to N) yield Segment(i, j)) ++
      (for (i <- 0 to N) yield AfterInt(i)) ++
      (for (i <- 0 to N) yield BeforeInt(i)) ++
      Seq(AfterInt(Integer.MAX_VALUE), BeforeInt(Integer.MAX_VALUE),
        AfterInt(Integer.MIN_VALUE), BeforeInt(Integer.MIN_VALUE),
        Empty, Forever)

  val inputs: Seq[Int] =
    Integer.MIN_VALUE +:
      (Integer.MIN_VALUE + 1) +:
      Integer.MAX_VALUE +:
      (Integer.MAX_VALUE - 1) +:
      (-1 to N + 1)

  private val service = Executors.newFixedThreadPool(POOL_SIZE)
  private def schedule(f: => Unit) = service.submit(new Runnable {
    override def run = f
  })

  private def compare(a: Interval, b: Interval) {
    for (i <- inputs)
      assert(a.contains(i) == b.contains(i), "" + a + " and " + b + " don't match at " + i)
    assert(a.isEmpty == b.isEmpty)
  }

  private def compare(a: Interval, oracle: Int => Boolean) {
    for (i <- inputs)
      assert(a.contains(i) == oracle(i), "" + a + " disagrees with oracle at " + i)
  }

  test("normal form") {
    for (a <- intervals)
      compare(a, a.normalize)
  }

  test("empty, forever") {
    compare(Empty, _ => false)
    compare(Forever, t => t < Integer.MAX_VALUE)
    assert(Empty.isEmpty)
    assert(!Forever.isEmpty)

    compare(Forever.complement, Empty)
    compare(Empty.complement, Forever)
    compare(Intersection(Forever, Empty), Empty)
    compare(Union(Empty, Empty), Empty)
    compare(Intersection(Forever, Forever), Forever)
    compare(Intersection(Empty, Empty), Empty)
    compare(Union(Forever, Empty), Forever)
    compare(Union(Forever, Forever), Forever)
  }

  test("union, union normal form") {
    (for (a <- intervals; b <- intervals; c <- intervals) yield schedule {
      val ab = Union(a, b)
      compare(ab, ab.normalize)
      compare(ab, i => a.contains(i) || b.contains(i))

      val abc = Union(c, ab)
      compare(abc, abc.normalize)
      compare(abc, i => a.contains(i) || b.contains(i) || c.contains(i))
    }).toList.map(_.get)
  }

  test("intersection, intersection normal form") {
    (for (a <- intervals; b <- intervals; c <- intervals) yield schedule {
      val ab = Intersection(a, b)
      compare(ab, ab.normalize)
      compare(ab, i => a.contains(i) && b.contains(i))

      val abc = Intersection(c, ab)
      compare(abc, abc.normalize)
      compare(abc, i => a.contains(i) && b.contains(i) && c.contains(i))
    }).toList.map(_.get)
  }

  private def complementTest(a: Interval) {
    val b = a.complement
    compare(b, i => i < Integer.MAX_VALUE && !a.contains(i))
    compare(a, i => i < Integer.MAX_VALUE && !b.contains(i))
    compare(b, b.normalize)
    compare(b.complement, a)
  }

  test("complement, complement normal form") {
    for (a <- intervals) complementTest(a)
    for (a <- intervals; b <- intervals) {
      complementTest(a union b)
      complementTest(a intersect b)
    }
  }
}
