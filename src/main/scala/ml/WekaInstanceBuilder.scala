package edu.mit.csail.cap.query
package ml

import weka.core._
import collection.mutable
import scala.Some

trait WekaInstanceBuilder[Feature] {
  def featureToAttributeMap(featureVectors: Iterable[FeatureVector[Feature]]) = {
    var featuresToAttribute = new mutable.HashMap[Feature, Attribute]()
    featureVectors.foreach {
      featureVector =>
        featureVector.elements.foreach {
          feature =>
            if (!featuresToAttribute.contains(feature)) featuresToAttribute.put(feature, new Attribute(feature.toString))
        }
    }
    featuresToAttribute.toMap
  }

  def buildAttributeVector(attributes: Iterable[Attribute]): FastVector = {
    val fastVector = new FastVector(attributes.size)
    attributes.foreach { attribute => fastVector.addElement(attribute) }
    fastVector
  }

  def featureVectorToInstance(featureVector: FeatureVector[Feature],
                              dataSet: Instances,
                              featuresToAttribute: Map[Feature, Attribute]): Instance = {
    val instance = new SparseInstance(0)
    instance.setDataset(dataSet)
    featureVector.elements.foreach {
      feature: Feature =>
        featuresToAttribute.get(feature) match {
          case Some(attribute) =>
            {
              instance.setValue(attribute, featureVector.value(feature))
            }
          case None => ???
        }
    }
    instance
  }

  def buildInstances(featuresToAttributes: Map[Feature, Attribute],
                     featureVectors: Iterable[FeatureVector[Feature]]) = {
    val instances = new Instances("training-instances",
      buildAttributeVector(featuresToAttributes.values),
      featuresToAttributes.size)
    featureVectors.foreach { featureVector =>
      instances.add(featureVectorToInstance(featureVector, instances, featuresToAttributes))
    }
    instances
  }
}

trait WekaLabeledInstanceBuilder[Feature, Label <: Enumeration#Value] extends WekaInstanceBuilder[Feature] {

  def labeledFeatureToAttributeMap(featureVectors: Iterable[(FeatureVector[Feature], Label)]): Map[Feature, Attribute] = {
    super.featureToAttributeMap(featureVectors.unzip._1)
  }

  def computeClassValue(label: Label): String = label.toString

  def buildAttributeVector(attributes: Iterable[Attribute], classAttribute: Attribute): FastVector = {
    val attributeVector = super.buildAttributeVector(attributes)
    attributeVector.addElement(classAttribute)
    attributeVector
  }

  def buildClassAttribute(labels: Iterable[Label]): Attribute = {
    val values = new FastVector
    labels.toList.distinct.foreach { label => values.addElement(computeClassValue(label)) }
    new Attribute("class-attribute", values)
  }

  def labeledFeatureVectorToInstance(featureVector: (FeatureVector[Feature], Label),
                                     dataSet: Instances,
                                     featuresToAttribute: Map[Feature, Attribute]): Instance = {
    val instance = featureVectorToInstance(featureVector._1, dataSet, featuresToAttribute)
    instance.setClassValue(computeClassValue(featureVector._2))
    instance
  }

  def buildLabeledInstances(featuresToAttributes: Map[Feature, Attribute],
                            featureVectors: Iterable[(FeatureVector[Feature], Label)]) = {
    val classAttribute = buildClassAttribute(featureVectors.unzip._2)
    val instances = new Instances("training-instances",
      buildAttributeVector(featuresToAttributes.values, classAttribute),
      featuresToAttributes.size)
    instances.setClass(classAttribute)
    featureVectors.foreach { featureVector =>
      instances.add(labeledFeatureVectorToInstance(featureVector, instances, featuresToAttributes))
    }
    instances
  }
}
