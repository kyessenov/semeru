package edu.mit.csail.cap.query
package util
import scala.collection.mutable
/**
 * Function memoization.
 */
class Memo[-T, +R](f: T => R) extends (T => R) {
  private[this] val cache = mutable.Map.empty[T, R]
  def apply(x: T): R = cache.get(x) match {
    case Some(y) => y
    case None =>
      val y = f(x)
      cache(x) = y
      y
  }
}
object Memo {
  def apply[T, R](f: T => R) = new Memo(f)
}
