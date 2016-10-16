package test
import edu.mit.csail.cap.query._
import util._
import org.scalatest.FunSuite
import scala.language.implicitConversions

class GraphSpec extends FunSuite {
  case class Arrow(from: Int) {
    def -->(to: Int) = Edge(from, (), to)
  }
  implicit def int2edge(from: Int) = Arrow(from)
  def basic(g: MutableGraph[Int, Unit]) {
    // create a simple a graph with a self loop, loop, singleton
    g.add(1)
    g.add(1, (), 2)
    g.add(1, (), 1)
    g.add(3)
    g.add(4, (), 2)
    g.add(2, (), 4)

    assert(g.numNodes === 4, "num nodes")
    assert(g.numEdges === 4, "num edges")
    g.foreach { case Edge(from, _, to) => assert(g.has(from, to), s"contains edge $from $to") }
    g.foreach { case Edge(from, _, to) => assert(g.has(from) && g.has(to), "has for nodes") }
    assert(g.outbound(1).toSet === Set((2, ()), (1, ())), "outbound 1")
    assert(g.inbound(1).toSet === Set((1, ())), "inbound 1")
    assert(g.outbound(3).toSet === Set(), "outbound 3")
    assert(g.has(3), "has 3")
    assert(g.inbound(3).toSet === Set(), "inbound 3")
    assert(g.inDegree(4) === 1, "indegree 4")
    assert(g.outDegree(4) === 1, "outdegree 4")

    g.add(4, (), 5)
    assert((g.remove(4, 5, (_: Unit) => true)).toList === 4 --> 5 :: Nil, "remove edge")
  }

  test("digraph") {
    basic(new Digraph[Int, Unit])
  }

  test("multidigraph") {
    basic(new MultiDigraph[Int, Unit])
  }

  test("subgraph") {
    val g = new Digraph[Int, Unit]
    basic(g)
    val g0 = g.subgraph(Set(1, 2, 3))
    assert(g0.toSet === Set(1 --> 2, 1 --> 1), "subgraph")
  }

  test("scc 1") {
    val g = new Digraph[Int, Unit]
    g add 1
    g add 2
    g add 3
    g add 4
    g add 5
    g add 6
    g add 7
    g.add(1, (), 2)
    g.add(2, (), 3)
    g.add(3, (), 1)
    g.add(1, (), 3)
    g.add(3, (), 4)
    g.add(5, (), 6)
    g.add(6, (), 5)
    assert(g.inbound(3).map(_._1).toSet === Set(1, 2))
    assert(g.inbound(5).map(_._1).toSet === Set(6))
    assert(g.inbound(0).map(_._1).toSet === Set())
    assert(g.SCC.toSet ===
      Set(Set(1, 2, 3), Set(4), Set(5, 6), Set(7)))
    assert(g.hasCycles)
    assert(g.condensation.isDAG)
  }
  test("scc 2") {
    val g = new Digraph[Int, Unit]
    assert(g.SCC.toSet ===
      Set())
    g add 1
    g add 2
    g add 3
    g add 4
    g.add(1, (), 2)
    g.add(2, (), 3)
    g.add(4, (), 2)
    g.add(1, (), 1)
    assert(g.inbound(2).map(_._1).toSet === Set(1, 4))
    assert(g.SCC.toSet ===
      Set(Set(1), Set(2), Set(3), Set(4)))
    assert(!g.hasCycles)
    assert(g.condensation.isDAG)
    assert(g.selfloops.toSet === Set(1))
    assert(g.condensation.toSet ===
      Set(Edge(Set(1), (), Set(2)),
        Edge(Set(2), (), Set(3)),
        Edge(Set(4), (), Set(2))), "condensation")
  }

  test("transitive") {
    val g = new Digraph[Int, Unit]
    g.add(1, (), 2)
    g.add(2, (), 3)
    g.add(1, (), 3)
    g.add(4, (), 2)
    g.add(5, (), 6)
    val g0 = g.transitiveReduction()
    val g1 = g.transitiveClosure()

    assert(g1.toSet === g.toSet + (4 --> 3), "closure")
    assert(g.compose(g1).toSet === Set(1 --> 3, 4 --> 3), "composition")
    assert(g0.nodes.toSet === g.nodes.toSet, "reduction nodes")
    assert(g0.toSet === Set(1 --> 2, 2 --> 3,
      4 --> 2, 5 --> 6), "reduction edges")
  }
}
