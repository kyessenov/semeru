package edu.mit.csail.cap.query
package util

import scala.collection.mutable

/**
 * Time interval.
 * Decision tree form is good for testing while normal form is good for storing and analyzing.
 * Representation is only unique for normal form.
 *
 * No interval contains Integer.MAX_VALUE. All intervals are subsets of Forever.
 */
sealed abstract class Interval {
  def contains(t: Int): Boolean
  def normalize: NormalForm
  def complement: Interval
  def isEmpty = (normalize == Empty.normalize)
  def head: Option[Int] = normalize.head
  def intersect(that: Interval) = Intersection(this, that)
  def union(that: Interval) = Union(this, that)
}
case class AfterInt(x: Int) extends Interval { // close on left
  override def contains(t: Int) = Integer.MAX_VALUE > t && t >= x
  override def normalize =
    if (x == Integer.MAX_VALUE)
      Empty.normalize
    else
      Segment(x, Integer.MAX_VALUE).normalize
  override def complement = BeforeInt(x)
}
case class BeforeInt(x: Int) extends Interval { // open on right
  override def contains(t: Int) = t < x
  override def normalize =
    if (x == Integer.MIN_VALUE)
      Empty.normalize
    else
      Segment(Integer.MIN_VALUE, x).normalize
  override def complement = AfterInt(x)
}
case class Intersection(a: Interval, b: Interval) extends Interval {
  override def contains(t: Int) = a.contains(t) && b.contains(t)
  override lazy val normalize = {
    val NormalForm(an) = a.normalize
    val NormalForm(bn) = b.normalize
    def helper(an: List[Segment], bn: List[Segment]): List[Segment] = {
      (an, bn) match {
        case (Nil, _) => Nil
        case (_, Nil) => Nil
        case (Segment(ax, ay) :: al, Segment(bx, by) :: bl) =>
          if (ay <= bx)
            helper(al, bn)
          else if (by <= ax)
            helper(an, bl)
          else if (ax <= bx && by <= ay) // ax bx < by ay
            Segment(bx, by) :: helper(an, bl)
          else if (ax <= bx && ay < by) // ax bx < ay by
            Segment(bx, ay) :: helper(al, bn)
          else if (bx < ax && ay <= by) // bx < ax ay by
            Segment(ax, ay) :: helper(al, bn)
          else if (bx < ax && by < ay) // bx < ax < by < ay
            Segment(ax, by) :: helper(an, bl)
          else {
            assert(false)
            Nil
          }
      }
    }
    NormalForm(helper(an, bn))
  }
  override def complement = Union(a.complement, b.complement)
}
case class Union(a: Interval, b: Interval) extends Interval {
  override def contains(t: Int) = a.contains(t) || b.contains(t)
  override def normalize = complement.normalize.complement
  override def complement = Intersection(a.complement, b.complement)
}
object Empty extends Interval {
  override def contains(t: Int) = false
  override def normalize = NormalForm(Nil)
  override def complement = Forever
}
object Forever extends Interval {
  override def contains(t: Int) = t < Integer.MAX_VALUE
  override def normalize = Segment(Integer.MIN_VALUE, Integer.MAX_VALUE).normalize
  override def complement = Empty
}
case class Segment(low: Int, high: Int) extends Interval {
  /** non-empty interval of the form [low, high) */
  assert(low < high, s"$low < $high")

  override def contains(t: Int) = low <= t && t < high
  override def normalize = NormalForm(this :: Nil)
  override def complement = normalize.complement
  override def isEmpty = false
  override def toString = "[" + low + ", " + high + ")"

}
case class NormalForm(segments: List[Segment]) extends Interval {
  /* this is the canonical form of intervals */
  assert((0 until (segments.size - 1)).forall(i => segments(i).high < segments(i + 1).low))
  override def contains(t: Int) = segments.exists(_.contains(t))
  override def normalize = this
  override def complement: NormalForm =
    if (segments.size == 0)
      Forever.normalize
    else {
      val out = new mutable.ListBuffer[Segment]

      if (segments.head.low > Integer.MIN_VALUE)
        out += Segment(Integer.MIN_VALUE, segments.head.low)

      if (segments.size > 1) {
        var i = 0
        while (i < segments.size - 1) {
          out += Segment(segments(i).high, segments(i + 1).low)
          i = i + 1
        }
      }

      if (segments.last.high < Integer.MAX_VALUE)
        out += Segment(segments.last.high, Integer.MAX_VALUE)

      NormalForm(out.toList)
    }
  override def head = segments match {
    case Nil        => None
    case first :: _ => Some(first.low)
  }
  override def toString = segments.foldLeft("{")(_ + _) + "}"
}

