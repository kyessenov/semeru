package edu.mit.csail.cap.query
package db

import org.neo4j.graphdb._
import org.neo4j.graphdb.index.Index
import org.neo4j.unsafe.batchinsert._
import org.neo4j.index.lucene.unsafe.batchinsert._
import org.neo4j.tooling._
import org.neo4j.graphdb.factory._
import scala.collection.mutable.HashMap
import scala.collection.JavaConverters._
import java.io.File
import util._
import analysis._
import edu.mit.csail.cap.util.trove3.gnu.trove.map.hash.TObjectLongHashMap
import java.lang.{ Integer => JInt, Long => JLong }
import org.neo4j.helpers.collection.IteratorUtil

trait Neo4j[V, L] extends Graph[V, L] {
  type Prop = java.util.HashMap[java.lang.String, java.lang.Object]
  import Neo4j._

  private var initialized = false
  private lazy val db = {
    initialized = true
    debug("connected to neo4j " + file)
    (new GraphDatabaseFactory).newEmbeddedDatabaseBuilder(file.getPath)
      .setConfig(GraphDatabaseSettings.read_only, "true")
      .setConfig(GraphDatabaseSettings.allow_store_upgrade, "true")
      .newGraphDatabase
  }
  private lazy val index = db.index.forNodes(OBJECT_INDEX)

  def file: File
  def keys: List[String]
  def key(v: V): (String, Any)
  def object2node(o: V): Option[Node] = {
    val (k, v) = key(o)
    Option(index.get(k, v).getSingle)
  }

  def node2object(node: Node): V
  def getLabel(r: Relationship): L

  def shutdown() {
    if (initialized)
      db.shutdown()
  }

  private def transaction[T](f: => T): T = {
    val tx = db.beginTx()
    try {
      val out = f
      out
    } finally {
      tx.success()
      tx.close()
    }
  }

  override def has(o: V) = transaction {
    object2node(o).isDefined
  }

  override def nodes = transaction {
    GlobalGraphOperations.at(db).getAllNodes.asScala.map(node2object(_))
  }

  override def numNodes = transaction {
    IteratorUtil.count(GlobalGraphOperations.at(db).getAllNodes)
  }

  override def numEdges = transaction {
    IteratorUtil.count(GlobalGraphOperations.at(db).getAllRelationships)
  }

  override def outbound(o: V) = transaction {
    object2node(o) match {
      case None => Nil
      case Some(n) =>
        n.getRelationships(Direction.OUTGOING).asScala.map { r =>
          (node2object(r.getEndNode), getLabel(r))
        }
    }
  }

  override def foreach[U](f: Edge[V, L] => U) = transaction {
    for (r <- GlobalGraphOperations.at(db).getAllRelationships.asScala)
      f(Edge(node2object(r.getStartNode), getLabel(r), node2object(r.getEndNode)))
  }

  override def inbound(o: V) = transaction {
    object2node(o) match {
      case None => Nil
      case Some(n) =>
        n.getRelationships(Direction.INCOMING).asScala.map { r =>
          (node2object(r.getStartNode), getLabel(r))
        }
    }
  }

  def putObject(o: V, p: Prop)
  def putLabel(l: L, p: Prop)

  def write(g: Graph[V, L]) {
    debug("batch insert " + g + " into " + this)

    // create the batch inserter
    val inserter = BatchInserters.inserter(file.getPath, BULK_CONFIG)
    val p = new Prop

    // create the batch index inserter
    val indexProvider = new LuceneBatchInserterIndexProvider(inserter)
    val index = indexProvider.nodeIndex(OBJECT_INDEX, INDEX_CONFIG)
    val map = new TObjectLongHashMap[V]

    debug("creating nodes")
    for (o <- g.nodes) {
      p.clear
      putObject(o, p)
      val n = inserter.createNode(p)
      map.put(o, n)
      index.add(n, p)
    }

    debug("creating edges")
    for (Edge(from, e, to) <- g) {
      p.clear
      putLabel(e, p)
      val src = map.get(from)
      val dest = map.get(to)
      inserter.createRelationship(src, dest, FIELD_EDGE, p)
    }

    debug("finished adding edges")
    indexProvider.shutdown()
    inserter.shutdown
    debug("done")
  }

  override def toString = "neo4j at " + file
}

object Neo4j {
  var BULK_CONFIG = Map(
    "neostore.nodestore.db.mapped_memory" -> "90M",
    "neostore.relationshipstore.db.mapped_memory" -> "3G",
    "neostore.relationshipgroupstore.db.mapped_memory" -> "10M",
    "neostore.propertystore.db.mapped_memory" -> "50M",
    "neostore.propertystore.db.strings.mapped_memory" -> "100M",
    "neostore.propertystore.db.arrays.mapped_memory" -> "0M").asJava

  var INDEX_CONFIG = Map(
    "type" -> "exact").asJava

  val FIELD_EDGE = new RelationshipType { val name = "field" }
  val OBJECT_INDEX = "objects"
}

case class HeapGraph(c: Connection, file: File, abs: File) extends HeapSeries with Neo4j[Object, HeapLabel] {
  val OBJECT_ID = "oid"
  val LOW = "low"
  val HIGH = "high"
  val EDGE_ID = "eid"

  if (!file.exists) {
    val hs = HeapSeries.build(c)
    write(hs)
    HeapAbstractionSerializer(c).writeFile(hs.abstraction, abs.getPath)
  }

  override lazy val abstraction =
    HeapAbstractionSerializer(c).readFile(abs.getPath)
  override def keys = OBJECT_ID :: Nil
  override def key(o: Object) = (OBJECT_ID, o)
  override def node2object(n: Node) =
    n.getProperty(OBJECT_ID).asInstanceOf[Object]
  override def getLabel(r: Relationship) = {
    val l = r.getProperty(LOW).asInstanceOf[JInt]
    val h = r.getProperty(HIGH).asInstanceOf[JInt]
    val id = r.getProperty(EDGE_ID).asInstanceOf[JLong]
    new HeapLabel(c.meta.field(id), Segment(l, h))
  }
  override def putObject(o: Object, p: Prop) {
    p.put(OBJECT_ID, o.asInstanceOf[JInt])
  }
  override def putLabel(l: HeapLabel, p: Prop) {
    p.put(LOW, l.interval.low.asInstanceOf[JInt])
    p.put(HIGH, l.interval.high.asInstanceOf[JInt])
    p.put(EDGE_ID, l.field.id.asInstanceOf[JLong])
  }
}

case class HeapAbstractionGraph(c: Connection, file: File) extends HeapAbstraction with Neo4j[Cluster, NormalForm] {
  val OBJECT_ID = "instance"

  val TYPE = "t"
  val DIM = "d"
  val TYPE_ID = "type"

  val LOW = "l"
  val HIGH = "h"
  lazy val interval = Segment(1, c.size + 1)

  override def keys = OBJECT_ID :: TYPE_ID :: Nil
  override def key(c: Cluster) = c match {
    case InstanceCluster(o, _) => (OBJECT_ID, o)
    case TypeCluster(t)        => (TYPE_ID, t.id)
  }
  override def node2object(n: Node) =
    if (n.hasProperty(OBJECT_ID))
      InstanceCluster(n.getProperty(OBJECT_ID).asInstanceOf[Int], c)
    else {
      val t = c.meta.typ(n.getProperty(TYPE).asInstanceOf[JLong])
      val d = n.getProperty(DIM).asInstanceOf[JInt]
      TypeCluster(t.array(d))
    }
  override def getLabel(r: Relationship) = {
    val ls = r.getProperty(LOW).asInstanceOf[Array[Int]]
    val hs = r.getProperty(HIGH).asInstanceOf[Array[Int]]
    println(ls.size)
    NormalForm((for ((l, h) <- ls zip hs) yield Segment(l, h)).toList)
  }
  override def putObject(c: Cluster, p: Prop) {
    c match {
      case InstanceCluster(o, _) =>
        p.put(OBJECT_ID, o.asInstanceOf[JInt])
      case TypeCluster(t) =>
        p.put(TYPE, t.baseType.id.asInstanceOf[JLong])
        p.put(DIM, t.dimension.asInstanceOf[JInt])
        p.put(TYPE_ID, t.id.asInstanceOf[JLong])
    }
  }
  override def putLabel(i: NormalForm, p: Prop) {
    p.put(LOW, i.segments.map(_.low.asInstanceOf[JInt]).toArray)
    p.put(HIGH, i.segments.map(_.high.asInstanceOf[JInt]).toArray)
  }
}
