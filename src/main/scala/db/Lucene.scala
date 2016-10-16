package edu.mit.csail.cap.query
package db
import util.Serializer
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.RAMDirectory
import org.apache.lucene.store.SimpleFSDirectory
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.index._
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.store.Directory
import org.apache.lucene.util.Version
import org.apache.lucene.search.Query
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.wordnet.SynExpand
import org.apache.lucene.search.BooleanClause
import java.io.File
import scala.language.implicitConversions
import org.apache.lucene.search.ScoreDoc

object Lucene {
  val Contents = "contents"
  val Id = "id"
  val Definition = "definition"

  lazy val analyzer = new EnglishAnalyzer(Version.LUCENE_35)
  lazy val synonymSearcher: IndexSearcher = {
    val synonymIndexDirectory = new SimpleFSDirectory(new File("data/synonyms"))
    val synonymReader = IndexReader.open(synonymIndexDirectory)
    new IndexSearcher(synonymReader)
  }

  def makeConfig = {
    val out = new IndexWriterConfig(Version.LUCENE_35, analyzer)
    //out.getMergePolicy.useCompoundFile(true)
    out
  }

  def synonyms(keyword: String): Query =
    SynExpand.expand(keyword, synonymSearcher, analyzer, Contents, 0)

  def build(methods: Set[Method]): RAMDirectory = {
    val index = new RAMDirectory
    val writer = new IndexWriter(index, makeConfig)

    for (m <- methods) {
      val doc = new Document()
      doc.add(new Field(Id, m.id.toString, Field.Store.YES, Field.Index.NO))
      doc.add(new Field(Definition, m.definition.id.toString, Field.Store.YES, Field.Index.NO))
      doc.add(new Field(Contents, m.words.toString, Field.Store.YES, Field.Index.ANALYZED))
      writer.addDocument(doc)
    }

    writer.commit()
    writer.close()

    index
  }

  def index(meta: Metadata, methods: Set[Method]) =
    MethodIndex(meta, IndexReader.open(build(methods)))
}

case class MethodIndex(meta: Metadata, index: IndexReader) {
  var MaxResults = 10000

  def search(q: Query, maxResultSize: Int = MaxResults): Set[Method] = {
    val searcher = new IndexSearcher(index)
    val collector = TopScoreDocCollector.create(maxResultSize, true)
    searcher.search(q, collector)
    for (hit <- collector.topDocs.scoreDocs.toSet[ScoreDoc])
      yield id(searcher.doc(hit.doc))
  }

  def id(doc: Document) =
    meta.method(doc.get(Lucene.Id).toLong)

  def contains(keywords: Set[String], pred: BooleanClause.Occur): Query = {
    assert(keywords.size > 0)
    val query = new BooleanQuery()
    for (keyword <- keywords)
      query.add(Lucene.synonyms(keyword), pred)
    debug("contains " + keywords + " expanded to " + query)
    query
  }

  def containsAll(q: String): Query =
    contains(q.split("\\s+").toSet.filter(_.size > 0), BooleanClause.Occur.MUST)

  def containsOne(q: String): Query =
    contains(q.split("\\s+").toSet.filter(_.size > 0), BooleanClause.Occur.SHOULD)

  /** Search method based on keyword */
  def contains(q: String, maxResultsSize: Int = MaxResults): Set[Method] =
    search(containsAll(q), maxResultsSize)

  def all: Set[Method] = {
    for (i <- 0 until index.maxDoc if !index.isDeleted(i))
      yield meta.method(index.document(i).get(Lucene.Id).toLong)
  }.toSet

  def shutdown() {
    index.close()
  }
}

trait MethodProvider extends Provider with Trace {
  private val path = new File(cache, "methods")
  private var initialized = false

  lazy val index = {
    if (!path.exists)
      build()

    initialized = true
    MethodIndex(meta, IndexReader.open(new SimpleFSDirectory(path)))
  }

  private def build() {
    val index = Lucene.build(super.methods)

    info("saving index to " + path)
    val to = new SimpleFSDirectory(path)
    for (file <- index.listAll())
      index.copy(to, file, file)

    info("done")
    to.close()
    index.close()
  }

  abstract override lazy val methods = 
    index.all

  abstract override def shutdown() {
    super.shutdown()
    if (initialized)
      index.shutdown()
  }
}