package edu.mit.csail.cap.query
package ml

import jnisvmlight._
import collection.mutable
import scala.Some
import weka.classifiers.bayes.NaiveBayes
import weka.core.{ Attribute, Instances }

trait ClassificationAlgorithm[Input, Feature, Label <: Enumeration#Value] {

  def train(input: Iterable[(Input, Label)])

  def classifierIsReady: Boolean

  def classifyFeatureVector(featureVector: FeatureVector[Feature]): Double

  def extractFeatures(input: Iterable[Input]): Iterable[FeatureVector[Feature]]
  // Provided by a feature extractor

  def classify(input: Iterable[Input]): Iterable[(FeatureVector[Feature], Double)] = {
    if (classifierIsReady) {
      val features = extractFeatures(input)
      features.filter(feature => !feature.isEmpty).map(x => (x, classifyFeatureVector(x)))
    } else {
      throw new RuntimeException("Model not trained")
    }
  }

  def mostRelevantFeatures(n: Int): List[(Feature, Double)]
}

trait NaiveBayesianClassifier[Input, Feature, Label <: Enumeration#Value]
    extends ClassificationAlgorithm[Input, Feature, Label]
    with LabeledFeatureExtraction[Input, Feature, Label]
    with WekaLabeledInstanceBuilder[Feature, Label] {
  private var classifier: NaiveBayes = null
  private var instances: Instances = null
  private var featuresToAttributes: Map[Feature, Attribute] = null

  def classifierIsReady = classifier != null

  def train(input: Iterable[(Input, Label)]) = {
    classifier = new NaiveBayes

    val featureVectors = extractLabeledFeatures(input)
    featuresToAttributes = labeledFeatureToAttributeMap(featureVectors)
    instances = buildLabeledInstances(featuresToAttributes, featureVectors)

    classifier.buildClassifier(instances)
  }

  def classifyFeatureVector(featureVector: FeatureVector[Feature]): Double =
    classifier.classifyInstance(featureVectorToInstance(featureVector, instances, featuresToAttributes))

  def mostRelevantFeatures(n: Int): List[(Feature, Double)] = {
    println(classifier)
    List()
  }
}

/*
 * Implementation of SVM relying on SVM-light
 */
trait SupportVectorMachine[Input, Feature, Label <: Enumeration#Value] extends ClassificationAlgorithm[Input, Feature, Label]
    with LabeledFeatureExtraction[Input, Feature, Label] {
  private val SVMLight = new SVMLightInterface()
  private val featureMapping = new mutable.LinkedHashMap[Feature, Int]()
  private var model: SVMLightModel = null

  def classifierIsReady = model != null

  private def labelToDouble(label: Label): Double = if (label.id == 0) -1 else +1

  private def generateIdValuePairs(featureVector: FeatureVector[Feature]): List[(Int, Double)] = {
    // Feature/value pairs MUST be ordered by increasing feature number. Features with value zero can be skipped
    featureVector.elements.map {
      feature: Feature =>
        val id = featureMapping(feature)
        (id, featureVector.value(feature))
    }.toList.sortWith((a: (Int, Double), b: (Int, Double)) => a._1 < b._1)
  }

  private def buildSVMLightFeatureVector(featureVector: FeatureVector[Feature]) = {
    val (ids, values) = generateIdValuePairs(featureVector).unzip
    new jnisvmlight.FeatureVector(ids.toArray, values.toArray)
  }

  private def buildSVMLightLabeledFeatureVector(featureVector: FeatureVector[Feature], label: Label) = {
    val (ids, values) = generateIdValuePairs(featureVector).unzip
    new jnisvmlight.LabeledFeatureVector(labelToDouble(label), ids.toArray, values.toArray)
  }

  private def buildFeatureMapping(input: Iterable[(FeatureVector[Feature], Label)]) = {
    var i = 1
    input.foreach { element =>
      element._1.elements.foreach { feature =>
        if (!featureMapping.contains(feature)) {
          featureMapping.put(feature, i)
          i = i + 1
        }
      }
    }
  }

  def trainingParameters = {
    SVMLightInterface.SORT_INPUT_VECTORS = true
    val learningParameters = new LearnParam
    learningParameters.`type` = LearnParam.CLASSIFICATION
    //learningParameters.verbosity = 10
    val trainingParameters = new TrainingParameters()
    trainingParameters.setLearningParameters(learningParameters)
    trainingParameters
  }

  def train(input: Iterable[(Input, Label)]) = {
    val labeledFeatures = extractLabeledFeatures(input)
    buildFeatureMapping(labeledFeatures)
    val labeledSVMlightFeatures = labeledFeatures.
      filter(labeledFeature => !labeledFeature._1.isEmpty).
      map { labeledFeature => buildSVMLightLabeledFeatureVector(labeledFeature._1, labeledFeature._2) }
    model = SVMLight.trainModel(labeledSVMlightFeatures.toArray, trainingParameters)
  }

  def classifyFeatureVector(featureVector: FeatureVector[Feature]): Double =
    model.classify(buildSVMLightFeatureVector(featureVector))

  def mostRelevantFeatures(n: Int): List[(Feature, Double)] = {
    if (classifierIsReady) {
      val topFeatures = model.getLinearWeights
        .tail // Dropping the first element as the first feature is not used
        .zipWithIndex
        .sortWith((a: (Double, Int), b: (Double, Int)) => a._1 > b._1)
        .take(n)
      val (weights, indices) = topFeatures.unzip
      val features = featureMapping.keys.toList
      indices.map(i => features(i)).toList.zip(weights)
    } else {
      throw new RuntimeException("Model not trained")
    }
  }
}

class SVMPacakgesInCallTree extends SupportVectorMachine[CallTree, String, IsEnabled.IsEnabled] with PackagesInCallTree
class NaiveBayesianPacakgesInCallTree extends NaiveBayesianClassifier[CallTree, String, IsEnabled.IsEnabled] with PackagesInCallTree
