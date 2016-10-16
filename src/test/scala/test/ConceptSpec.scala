package test

import edu.mit.csail.cap.query.analysis._
import org.scalatest._

@Ignore
class ConceptSpec extends FunSuite {
  // encode matrix for composite, even, odd, prime, square
  val composite = 'c'
  val even = 'e'
  val odd = 'o'
  val prime = 'p'
  val square = 's'

  def map(x: java.lang.Integer): Set[Char] = {
    if (x == 1) return Set(odd, square)
    if (x == 2) return Set(even, prime)
    if (x == 3) return Set(odd, prime)
    if (x == 4) return Set(composite, even, square)
    if (x == 5) return Set(odd, prime)
    if (x == 6) return Set(composite, even)
    if (x == 7) return Set(odd, prime)
    if (x == 8) return Set(composite, even)
    if (x == 9) return Set(composite, odd, square)
    if (x == 10) return Set(composite, even)
    return Set()
  }

  test("FCA library") {

    val keys = List[java.lang.Integer](1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    import Ordering.Implicits._
    val out = Concepts[java.lang.Integer, Char](keys.map(key => key -> map(key)).toMap)

    val expected = Set(
      Concept[java.lang.Integer, Char](Set(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), Set[Char]()),
      Concept[java.lang.Integer, Char](Set(4, 6, 8, 9, 10), Set(composite)),
      Concept[java.lang.Integer, Char](Set(1, 4, 9), Set(square)),
      Concept[java.lang.Integer, Char](Set(2, 4, 6, 8, 10), Set(even)),
      Concept[java.lang.Integer, Char](Set(1, 3, 5, 7, 9), Set(odd)),
      Concept[java.lang.Integer, Char](Set(2, 3, 5, 7), Set(prime)),
      Concept[java.lang.Integer, Char](Set(4, 6, 8, 10), Set(composite, even)),
      Concept[java.lang.Integer, Char](Set(4, 9), Set(composite, square)),
      Concept[java.lang.Integer, Char](Set(4), Set(composite, even, square)),
      Concept[java.lang.Integer, Char](Set(1, 9), Set(odd, square)),
      Concept[java.lang.Integer, Char](Set(9), Set(composite, odd, square)),
      Concept[java.lang.Integer, Char](Set(2), Set(even, prime)),
      Concept[java.lang.Integer, Char](Set(3, 5, 7), Set(odd, prime)),
      Concept[java.lang.Integer, Char](Set(), Set(composite, even, odd, prime, square)))

    assert(out.concepts === expected)
  }
}
