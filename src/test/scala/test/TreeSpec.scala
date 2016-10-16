package test
import edu.mit.csail.cap.query._
import util._
import org.scalatest.FunSuite

class TreeSpec extends FunSuite {
  test("linked tree") {
    val a = new LinkedTreeImpl("a")
    assert(a.nodes.toList == "a" :: Nil)
    assert(a.numNodes == 1)
    assert(a.numEdges == 0)
    assert(a.data == "a")
    assert(a.outbound("a").isEmpty)
    assert(a.has("a"))

    val b = a + new LinkedTreeImpl("b")
    val c = a + new LinkedTreeImpl("c")
    val d = b + new LinkedTreeImpl("d")

    //
    // a
    // 1 2
    // b   c
    // 3
    // d
    //

    assert(a.numNodes === 4)
    assert(a.numEdges === 3)
    assert(a.data === "a")
    assert(c.data === "c")
    assert(a.nodes.toList === "a" :: "b" :: "d" :: "c" :: Nil)
    assert(a.outbound("b").toList === ("d", ()) :: Nil)
    assert(a.has("d"))
    assert(!c.has("d"))
    assert(b.has("d"))

    assert(d.outbound("d").isEmpty)
    assert(a.outbound("d").isEmpty)

    assert(a.search("a") === Some(a))
    assert(b.search("d") === Some(d))
    assert(a.search("d") === Some(d))
    assert(b.search("c") === None)

    // Select 
    assert(a.select(x => x.data == "b" | x.data == "c").forall(_.size === 0))
    assert(a.select(x => x.data == "b" | x.data == "c").map(_.data).toSet === Set("b", "c"))
    assert(a.select(x => x.data === "d").map(_.toSet).toSet === Set(Set()))
    
    // Select with roots
    assert(a.selectWithParents(_.data == "d").map(_.toSet) === Some(Set(
      Edge("a", (), "b"), Edge("b", (), "d")
    )))
    assert(a.selectWithParents(_.data == "a").map(_.toSet) === Some(Set()))
    assert(a.selectWithParents(_.data == "e").map(_.toSet) === None)
    
    // Enumeration
    assert(a.toList === Edge("a", (), "b") ::
      Edge("b", (), "d") ::
      Edge("a", (), "c") :: Nil)

    // Remove "b" node
    a.remove("b")
    assert(a.nodes.toList === "a" :: "c" :: Nil)  
  }
}
