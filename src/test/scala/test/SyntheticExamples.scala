package test

import org.scalatest.FunSuite

import edu.mit.csail.cap.query._
import edu.mit.csail.cap.query.analysis._
import edu.mit.csail.cap.query.db._
import edu.mit.csail.cap.query.ingest._
import edu.mit.csail.cap.query.web._
import edu.mit.csail.cap.query.experiments._

class SyntheticExamples extends FunSuite with CodeComparison {
  implicit val p = Parameters()
  val data = web.DataProvider(p)

  def magic = data.meta.method("test.cap.framework.F.magic(Ljava/lang/Object;)Ljava/lang/Object;").get

  test("collection") {
    Experiments.Test.run()
  }

  test("framework usage collection") {
    Experiments.FrameworkUsage.run()
  }

  def load(s: String)(f: TraceConfig => Unit) =
    data(s) match {
      case None => fail(s"expected to have trace $s")
      case Some(t) =>
        // manually load metadata
        data.meta.load(data.db, t.metadata)

        try {
          f(t)
        } finally {
          t.shutdown
        }
    }

  test("construction and storage") {
    load("test") { db =>
      val c = db.t
      val h1 = HeapSeries.build(c)
      val h2 = c.heaps
      assert(h1.toSet === h2.toSet, "heap series persistence")

      val habs1 = HeapAbstraction.build(h1)
      val habs2 = h2.abstraction
      assert(habs1.toSet === habs2.toSet, "heap abstraction persistence")
      assert(c.enters.events === c.trees.events, "call trees must match by events")
    }
  }

  test("synthesis: basic") {
    load("FrameworkUsage") { db =>
      val c = db.t
      val t = c.member(Set(magic)).events(0)
      val out = db.compile(t.seeds)
      compare(out, data.p, "FrameworkUsage")
    }
  }

  test("synthesis: container elimination") {
    load("FrameworkUsage") { db =>
      val c = db.t
      val t = c.member(Set(magic)).events(1)
      val out = db.compile(t.seeds)
      compareText(out, """
class U1 {
  static void test1() {
    Object o = new Object();
    F.magic(o);
  }
}
""")
    }
  }

  test("synthesis: type hierarchy collapse") {
    load("FrameworkUsage") { db =>
      val c = db.t
      val t = c.member(Set(magic)).events(2)
      val out = db.compile(t.seeds)
      compareText(out, """
class U1 {
  static void test2() {
    UA ua = new UA();
    F f = new F();
    UA ua0 = new UA();
    ua.f1 = ua0;
    f.set(ua);
    f.x();
  }
}
class UA implements A {
  Object f1;
  @Override void x() {
    Object o = this.f1;
    F.magic(o);
  }
}
""")
    }
  }
}
