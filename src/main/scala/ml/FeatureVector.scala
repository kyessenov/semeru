package edu.mit.csail.cap.query
package ml

trait FeatureVector[Feature] {
  def elements: Set[Feature]
  def value(feature: Feature): Double
  def isEmpty: Boolean
}

object BooleanFeatureVector {
  def TanimotoSimilarity[Feature](a: BooleanFeatureVector[Feature], b: BooleanFeatureVector[Feature]): Double = {
    val union = a.elements ++ b.elements
    val intersection = a.elements intersect b.elements
    intersection.size.asInstanceOf[Double] / union.size
  }
  def TanimotoDistance[Feature](a: BooleanFeatureVector[Feature], b: BooleanFeatureVector[Feature]): Double = {
    -math.log(TanimotoSimilarity(a, b))
  }
}

case class BooleanFeatureVector[Feature](features: Set[Feature]) extends FeatureVector[Feature] {
  def elements() = features
  def contains(feature: Feature) = features.contains(feature)
  def value(feature: Feature) = if (features.contains(feature)) 1 else 0
  def isEmpty() = features.isEmpty
  def merge(other: BooleanFeatureVector[Feature]) =
    new BooleanFeatureVector[Feature](features ++ other.elements)
}

case class CountingFeatureVector[Feature](features: Map[Feature, Int]) extends FeatureVector[Feature] {
  def elements() = features.keySet
  def numberOf(feature: Feature) = features.get(feature) match {
    case Some(n) => n
    case None    => 0
  }
  def value(feature: Feature) = numberOf(feature)
  def isEmpty() = features.isEmpty

  private def mergeMap[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] =
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) { (a, kv) =>
      a + (if (a.contains(kv._1)) kv._1 -> f(a(kv._1), kv._2) else kv)
    }

  def merge(other: CountingFeatureVector[Feature]): CountingFeatureVector[Feature] = {
    new CountingFeatureVector[Feature](mergeMap(List(features, other.features))((v1, v2) => v1 + v2))
  }
}

trait FeatureExtraction[Input, Feature] {
  def extractFeatures(input: Input): Iterable[FeatureVector[Feature]]
  def extractFeatures(input: Iterable[Input]): Iterable[FeatureVector[Feature]] =
    input.map({ element: Input => extractFeatures(element) }).flatten
}

trait LabeledFeatureExtraction[Input, Feature, Label <: Enumeration#Value] extends FeatureExtraction[Input, Feature] {
  def extractLabeledFeatures(input: Iterable[(Input, Label)]): Iterable[(FeatureVector[Feature], Label)] = {
    input.map({ element => labelListElements(extractFeatures(element._1), element._2) }).flatten
  }
  private def labelListElements(featureVectors: Iterable[FeatureVector[Feature]], label: Label) = {
    featureVectors.map(element => (element, label))
  }
}

import analysis._
trait MethodsInCallTree extends FeatureExtraction[CallTree, Method] {
  def retrieveMethods(callTree: CallTree): Set[Method] = callTree.methods
  def extractFeatures(callTree: CallTree): Iterable[FeatureVector[Method]] =
    List(new BooleanFeatureVector[Method](retrieveMethods(callTree)))
}

trait PackagesInCallTree extends FeatureExtraction[CallTree, String] {
  def retrievePackages(callTree: CallTree): Set[Package] = callTree.methods.map(_.definition.declarer.packag)
  def extractFeatures(callTree: CallTree): Iterable[FeatureVector[String]] =
    List(new BooleanFeatureVector[String](retrievePackages(callTree)))
}

object IsEnabled extends Enumeration {
  type IsEnabled = Value
  val Disabled, Enabled = Value
}
