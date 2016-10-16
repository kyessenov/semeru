package edu.mit.csail.cap.query

import db._
import com.google.common.cache._
import java.io.File
import java.sql.ResultSet
import edu.mit.csail.cap.util.trove3.gnu.trove.map.hash.TIntObjectHashMap

trait Provider {
  def cache: File
  def meta: Metadata
  def shutdown()

  def invalidate() {
    def delete(file: File) {
      if (file.isDirectory)
        for (f <- file.listFiles())
          delete(f)
      if (file.exists)
        file.delete()
    }

    debug("delete cache " + cache)
    delete(cache)
  }
}

trait TypeProvider extends SQLConnection with Provider with Trace {
  private val _types = new TIntObjectHashMap[Type]

  /** Retrieve object type */
  override def typeOf(o: Object): Type = {
    if (o.isNull)
      NullType
    else if (o.isUnknown)
      UnknownType
    else if (_types.containsKey(o))
      _types.get(o)
    else
      read(s"select type, dims from OBJECTS where id=$o") { rs =>
        meta.typ(rs.getLong(1)).array(rs.getInt(2))
      } match {
        case t :: Nil =>
          _types.put(o, t)
          t
        case _ =>
          UnknownType
      }
  }

  /** All objects of the exact type */
  override def objects(t: Type): List[Object] =
    read(s"select id from OBJECTS where type=${t.baseType.id} and dims=${t.dimension}") { rs =>
      val o = rs.getInt("id")
      _types.put(o, t)
      o
    }

  /** All objects declared of type t */
  def instances(t: ClassType): Set[Object] = {
    for (sub <- t.allSubtypes + t)
      yield objects(sub)
  }.flatten

  /** Caches a map from an object to its type in a weak map */
  def forceLoadTypes() {
    apply("select id, type, dims from OBJECTS") { rs =>
      _types.put(rs.getInt(1), meta.typ(rs.getLong(2)).array(rs.getInt(3)))
    }
    debug("loaded types for all objects: " + _types.size)
  }

  /** Clear type cache */
  def forceClearTypes() {
    _types.clear()
  }

  abstract override def shutdown() {
    super.shutdown()
    _types.clear()
  }
}

trait EventProvider extends Provider with Trace {
  private val _events = {
    CacheBuilder.newBuilder
      .weakValues
      .maximumSize(Parameters.EventCacheSize)
      .build[java.lang.Integer, Event]
  }

  /** Event schema */
  def event(rs: ResultSet): Event = {
    def nullable(name: String): Option[Int] = {
      val out = rs.getInt(name)
      if (rs.wasNull) None else Some(out)
    }

    val i = rs.getInt("counter")
    Option(_events.getIfPresent(i)) match {
      case Some(e) => e
      case None =>
        val receiver = rs.getInt("receiver")
        val depth = rs.getInt("stack_depth")
        val thread = rs.getLong("thread")
        val id = rs.getLong("id")
        val value = nullable("value")
        val caller = nullable("caller")
        val e = rs.getInt("event_type") match {
          case Enter.key =>
            val params = (nullable("param0") :: nullable("param1") :: Nil).flatten
            val t = if (receiver.isNull) None else Some(receiver)
            new Enter(t, meta.method(id), value, params, nullable("succ"), thread, c, depth, caller, i)
          case Exit.key =>
            new Exit(meta.method(id), value.get, thread, c, depth, caller, i)
          case Exception.key =>
            new Exception(receiver, thread, c, depth, caller, i)
          case Read.key =>
            val t = if (receiver.isNull) None else Some(receiver)
            new Read(t, meta.field(id), value.get, thread, c, depth, caller, i)
          case Write.key =>
            val t = if (receiver.isNull) None else Some(receiver)
            new Write(t, meta.field(id), value.get, thread, c, depth, caller, i)
          case ArrayRead.key =>
            new ArrayRead(receiver, id.toInt, value.get, thread, c, depth, caller, i)
          case ArrayWrite.key =>
            new ArrayWrite(receiver, id.toInt, value.get, thread, c, depth, caller, i)
        }

        _events.put(i, e)

        e
    }
  }

  override def at(i: Int) =
    Option(_events.getIfPresent(i)) match {
      case Some(e) => e
      case None    => c.t.at(i)
    }

  abstract override def shutdown() {
    super.shutdown()
    _events.invalidateAll()
  }
}

trait HeapProvider extends Provider with Trace {
  /** Heaps database */
  private var heapsInitialized = false
  private val heapFiles = new File(cache, "neo")
  private val abstractionFiles = new File(cache, "abstraction")

  override lazy val heaps = {
    heapsInitialized = true
    HeapGraph(c, heapFiles, abstractionFiles)
  }

  abstract override def shutdown() {
    super.shutdown()
    if (heapsInitialized) heaps.shutdown()
  }
}

/** Full trace with connections to database sources */
class Connection(db: Database, name: String, override val meta: Metadata)
    extends SQLConnection(db, name)
    with TraceProxy
    with EventProvider
    with TypeProvider
    with MethodProvider
    with HeapProvider {
  debug("connected to " + this)
  cache.mkdirs()

  override def cache = new File(s"var/${c.db.server}/${c.name}")
  override val t = DeclTrace(this)
  override def c = this
  override lazy val size = t.size

  def makeIndex() {
    debug(s"heaps: $heaps")
    debug(s"methods: ${methods.size}")
  }

  /** String value. */
  def stringValue(o: Object): Option[String] =
    read(s"select value from STRINGS where id=$o") { _.getString(1) }.headOption

  override def shutdown() {
    super.shutdown()
  }

  override def toString = name + "@" + db
}
