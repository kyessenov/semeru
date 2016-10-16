package edu.mit.csail.cap.query
package ml

import collection.mutable
import weka.clusterers.SimpleKMeans
import weka.core._
import scala.Some

trait ClusteringAlgorithm[Input, Feature] {
  type Cluster = Set[FeatureVector[Feature]]

  def findClusters(input: Iterable[Input], numberOfClusters: Int): Set[Cluster]
}

object SimilarityMatrixGenerator {
  def generateSimilarityMatrix[F <: FeatureVector[Feature], Feature](
    featureVectors: List[F],
    distanceFunction: (BooleanFeatureVector[Feature], BooleanFeatureVector[Feature]) => Double) = {}
}

trait KMeansClustering[Input, Feature] extends ClusteringAlgorithm[Input, Feature]
    with FeatureExtraction[Input, Feature]
    with WekaInstanceBuilder[Feature] {
  def assignInstancesToCluster(featureVectors: List[FeatureVector[Feature]], assignments: Array[Int]): Set[Cluster] = {
    val clusters = new mutable.HashMap[Int, Set[FeatureVector[Feature]]]
    var i = 0
    assignments.foreach {
      c =>
        {
          clusters.get(c) match {
            case Some(cluster) =>
              clusters.put(c, cluster + featureVectors(i))
            case None =>
              clusters.put(c, Set(featureVectors(i)))
          }
        }
        i = i + 1
    }

    clusters.values.toSet
  }

  def findClusters(input: Iterable[Input], numberOfClusters: Int): Set[Cluster] = {
    val clusterer = new SimpleKMeans
    clusterer.setPreserveInstancesOrder(true)
    clusterer.setNumClusters(numberOfClusters)

    val featureVectors = extractFeatures(input)
    val featuresToAttributes = featureToAttributeMap(featureVectors)
    val instances = buildInstances(featuresToAttributes, featureVectors)

    clusterer.buildClusterer(instances)
    assignInstancesToCluster(featureVectors.toList, clusterer.getAssignments)
  }
}

class KMeansMethodsInCallTreeClusterer extends KMeansClustering[CallTree, Method] with MethodsInCallTree
class KMeansPackagesInCallTreeClusterer extends KMeansClustering[CallTree, String] with PackagesInCallTree
