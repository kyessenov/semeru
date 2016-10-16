package edu.mit.csail.cap.query

import scala.collection.mutable

/** Error aggregator */
class Errors {
  /** Something overflows */
  def overflow(start: List[Seed], msg: String) {
    warn(s"overflow: $msg for $start")
  }

  val slicingErrors = new mutable.HashMap[(Type, Option[Method]), List[Seed]]
  
  /** Specify an error for an object in a method body by trace location */
  def slicing(s: Seed) {
    val d = s.e.parent.map(_.method)
    val t = s.o.typ(s.e.c)
    slicingErrors((t,d)) = s :: slicingErrors.getOrElse((t,d), Nil)
    warn(s"can't slice instance of $t inside $d")
  }

  val fieldTypeCollisions = new mutable.HashSet[(Field, Type)]

  /** Encountered field collision */
  def fieldCollision(old: Field, newType: Type) {
    fieldTypeCollisions += ((old, newType))
  }
}