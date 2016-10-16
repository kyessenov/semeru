package edu.mit.csail.cap.query
package analysis

import util._

/** Simple viable path of length at least one. */
sealed abstract class Path[V] {
  def from: V
  def to: V
  def life: Interval
  def contains(v: V): Boolean
  def length: Int
  def isSimple: Boolean
  def isViable: Boolean
  def reverseNodes: List[V]
  def reverseLabels: List[Edge[V, Label]]

  def nodes = reverseNodes.reverse
  def labels = reverseLabels.reverse
  def critical = {
    assert(isViable)
    life.normalize match {
      case NormalForm(Segment(x, _) :: _) => x
      case _                              => throw new RuntimeException("path must be viable")
    }
  }

  def criticalEdge = labels.find {
    case e => e.l.interval.contains(critical) && !e.l.interval.contains(critical - 1)
  }.get

  def -->(l: Label, v: V) = Extend(this, l, v)

  def times: List[Int] =
    labels.flatMap(_.l.interval.head)
}

final case class Start[V](v: V) extends Path[V] {
  val from = v
  val to = v
  val life = Forever
  def contains(x: V) = x == v
  val length = 1
  val isSimple = true
  val isViable = true
  val reverseNodes = List(v)
  val reverseLabels = Nil
  override def toString = v.toString
}

final case class Extend[V](p: Path[V], l: Label, v: V) extends Path[V] {
  val from = p.from
  val to = v
  val life = Intersection(p.life, l.interval).normalize
  def contains(x: V) = x == v || p.contains(x)
  val length = p.length + 1
  val isSimple = p.isSimple && !p.contains(v)
  val isViable = !life.isEmpty

  def reverseNodes = v :: p.reverseNodes
  def reverseLabels = Edge(p.to, l, v) :: p.reverseLabels
  override def toString = p.toString + "-->" + to.toString
}

