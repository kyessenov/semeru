package edu.mit.csail.cap.query
package analysis

import java.io.File
import xml.{ NodeSeq, Elem, XML }

class MethodDocumentation(val documentation: String,
                          val returnValue: Option[String],
                          val parameters: List[(String, String)],
                          val exceptions: List[String]) {
  override def toString: String = {
    documentation +
      returnValue +
      parameters.foldLeft("") { (x, y) => x + " " + y._1 + ": " + y._2 } +
      exceptions.foldRight("") { (x, y) => x + " " + y }
  }
}

class XMLDocumentation private (val c: Connection, xmlDocuments: List[Elem]) extends Documentation {
  override def get(method: Method): Option[MethodDocumentation] = {
    for (xmlDocument <- xmlDocuments) {
      val documentation = get(xmlDocument, method)
      if (documentation.isDefined)
        return documentation
      else {
        method.overrides match {
          case Some(m) => return get(m)
          case None    =>
        }
      }
    }
    None
  }

  def get(xml: Elem, method: Method): Option[MethodDocumentation] = {
    val className = method.declarer.name
    val methodName = method.name
    val methodNode = ((xml \ "jelclass").
      filter { x => (x \ "@fulltype").text == className } \ "methods" \ "method").
      filter { x => (x \ "@name").text == methodName }
    if (methodNode.isEmpty)
      None
    else {
      val commentNode = (methodNode \ "comment")
      val methodDescription = (commentNode \ "description").text
      if (methodDescription.isEmpty) {
        None
      } else {
        val methodAttributes = (commentNode \ "attribute")
        Some(new MethodDocumentation(methodDescription,
          returnValueDoc(methodAttributes),
          argumentsDoc(methodNode),
          exceptionsDoc(methodAttributes)))
      }
    }
  }

  def returnValueDoc(methodAttributes: NodeSeq): Option[String] = {
    val returnNodeSeq = methodAttributes.filter(x => (x \ "@name").text == "@return")
    if (returnNodeSeq.isEmpty)
      None
    else
      Some((returnNodeSeq \ "description").text)
  }

  def argumentsDoc(methodNode: NodeSeq): List[(String, String)] =
    (methodNode \ "params" \ "param").map { x => ((x \ "@name").text -> (x \ "@comment").text) }.toList

  def exceptionsDoc(methodAttributes: NodeSeq): List[String] =
    methodAttributes.filter(x => (x \ "@name").text == "@throws").map { x => (x \ "description").text }.toList
}

object XMLDocumentation {
  /**
   * Loads the documentation from a JELDoclet-generated XML file.
   *  http://jeldoclet.sourceforge.net/
   *
   *  To generate the file run this command
   *  find PATH_TO_SOURCE_FILES -type f -name "*.java" |
   *  xargs javadoc -doclet com.jeldoclet.JELDoclet -docletpath PATH_TO/jeldoclet.jar
   *
   *  @param file the XML file containing the javadoc representation generated
   *  @return a Documentation object on which users can perform queries
   */
  def load(db: Connection, files: List[File]): Documentation = {
    for (file <- files if !file.exists) warn("file " + file + " doesn't exist")
    val xmlDocuments = files.filter(_.exists).map(XML.loadFile(_))
    new XMLDocumentation(db, xmlDocuments)
  }
}

trait Documentation {
  /** Return documentation for m or its first overridden method */
  def get(m: Method): Option[MethodDocumentation]
  def c: Connection
}

