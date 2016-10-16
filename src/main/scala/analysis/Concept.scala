package edu.mit.csail.cap.query
package analysis

import scala.collection.JavaConverters._
import colibri.lib._

case class Concept[O <% Ordered[O], A <% Ordered[A]](o: Set[O], a: Set[A])

case class Concepts[O <% Ordered[O], A <% Ordered[A]](map: Map[O, Set[A]]) {
  def attributes: Set[A] = map.values.fold(Set())(_ union _)
  def objects: Set[O] = map.keys.toSet
  
  /** Compute the feature lattice */
  lazy val concepts: Set[Concept[O, A]] = {
    val rel = new TreeRelation
    for ((o, as) <- map; a <- as)
      rel.add(o, a)

    debug("computing lattice")
    val out = new HybridLattice(rel).conceptIterator(Traversal.TOP_ATTRSIZE).asScala.map {
      case concept =>
        Concept(concept.getObjects.asScala.toSet.map((o: Any) => o.asInstanceOf[O]),
          concept.getAttributes.asScala.toSet.map((a: Any) => a.asInstanceOf[A]))
    }.toSet
    debug("done")

    out
  }


  /** Intersection of attributes */
  def intersection: Set[A] =
    if (objects.size == 0)
      Set()
    else
      map.values.reduce(_ intersect _)

/*
  /** Group attributes together: if no concept separates them */
  def attributeGroups: List[Set[A]] =
    union.groupBy(attr => concepts.filter(_.a(attr))).values.toList

  /** Group objects together: if no concept separates them */
  def objectGroups: List[List[O]] =
    objects.groupBy(obj => concepts.filter(_.o(obj))).values.toList
  
  def feature(pos: Traversable[O]): List[(A, Float)] = feature(pos, objects.toSet -- pos.toSet)

  def feature(pos: Traversable[O], neg: Traversable[O]): List[(A, Float)] = {
    assert(pos.size > 0, "must provide at least one positive example")

    // positive attributes
    val apos = pos.toSet.map(f)

    // negative attributes
    val aneg = neg.toSet.flatMap(f)

    // how many share the attribute
    // times -1 if it is shared by negative object
    apos.flatten.toList.map {
      case a => (a, apos.filter(_(a)).size.toFloat / apos.size * (if (aneg(a)) -1 else 1))
    }.sortBy(_._2).reverse
  }
  def features = concepts.collect {
    case Concept(os, as) if os.size > 0 => (os, feature(os))
  }


**/
  def hasse(SkipSingletons: Boolean = true) = {
    // compute sub-concept relationships
    // orient from big to small
    val g = new util.Digraph[Concept[O, A], Unit]
    for (
      a <- concepts; b <- concepts;
      if a.o.subsetOf(b.o) && a.o.size < b.o.size;
      if !SkipSingletons || 1 < a.o.size
    ) g.add(b, (), a)

    g.transitiveReduction
  }
}

