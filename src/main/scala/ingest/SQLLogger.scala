package edu.mit.csail.cap.query
package ingest

import java.io.{ File, FileWriter }
import java.sql.Statement
import scala.collection.mutable
import db._
import edu.mit.csail.cap.wire.VMValue

/** Loads trace data into MySQL */
class SQLLogger(val db: Database, val meta: Metadata, val log: String) {
  info(s"create log $log")

  // Schema files
  val LOG = CSVTable(Schema.LOG)
  val PARAMS = CSVTable(Schema.PARAMS)
  val OBJECTS = CSVTable(Schema.OBJECTS)
  val STRINGS = CSVTable(Schema.STRINGS)

  // Execution state
  val threads = new mutable.HashMap[Long, ThreadStack]
  def thread(uid: Long): ThreadStack =
    threads.getOrElseUpdate(uid, new ThreadStack)

  /** Commit data to MySQL */
  def commit() {
    // Check for stack matching property
    val active = threads.toList.filter(_._2.depth > 0)
    if (active.size > 0)
      info(s"detected missing closing event for ${active.size} threads")

    // Delete stale data
    db.drop(log)
    db.create(log)

    val c = db.connect(log)
    val stmt = c.createStatement()

    LOG.load(stmt)
    PARAMS.load(stmt)
    OBJECTS.load(stmt)
    STRINGS.load(stmt)

    debug("committing log")
    c.commit()
    c.close()

    postProcess()
  }

  def postProcess() {
    info("postprocessing")
    val c = db.connect(log, meta)
    for (cmd <- Schema.POST_PROCESS)
      c.execute(cmd)
    c.shutdown()
  }

  /** Logs an entry into the database and returns the entry counter */
  var last = 0
  def log(threadId: Long,
          eltId: Long,
          eventType: Kind,
          receiver: Option[Object],
          value: Option[Object],
          line: Option[Int] = None,
          param0: Option[Object] = None,
          param1: Option[Object] = None): Int = {
    val info = thread(threadId)
    last = last + 1
    LOG.write(last, eventType.key, eltId, info.depth, receiver, value, threadId, info.parent, line, param0, param1, None)
    last
  }

  def accessField(t: Long, thisObj: Object, owner: Long, name: String, oldValue: Object, line: Int) =
    meta.clazz(owner).field(name) match {
      case Some(f) => log(t, f.id, Read, Some(thisObj), Some(oldValue), Some(line))
      case None    => error(s"cannot resolve $name in ${meta.clazz(owner)}, skipping access field")
    }

  def assignField(t: Long, thisObj: Object, owner: Long, name: String, newValue: Object, line: Int) =
    meta.clazz(owner).field(name) match {
      case Some(f) => log(t, f.id, Write, Some(thisObj), Some(newValue), Some(line))
      case None    => error(s"cannot resolve $name in ${meta.clazz(owner)}, skipping assign field")
    }

  def accessArray(t: Long, array: Object, index: Int, oldValue: Object, length: Int, line: Int) =
    log(t, index, ArrayRead, Some(array), Some(oldValue), Some(line))

  def assignArray(t: Long, array: Object, index: Int, newValue: Object, length: Int, line: Int) =
    log(t, index, ArrayWrite, Some(array), Some(newValue), Some(line))

  def enterMethod(t: Long, thisObj: Object, method: Long, args: Array[Object]) {
    val param0 = if (args.size > 0) Some(args(0)) else None
    val param1 = if (args.size > 1) Some(args(1)) else None
    val counter = log(t, method, Enter, Some(thisObj), None, None, param0, param1)
    thread(t).push(new Frame(method, counter))

    for ((arg, i) <- args.zipWithIndex)
      PARAMS.write(counter, arg, i.toByte)
  }

  def exitMethod(t: Long, returnValue: Object) {
    val info = thread(t)
    if (info.empty) return

    log(t, info.peek.method, Exit, None, Some(returnValue))
    info.pop()
  }

  def exception(t: Long, exception: Object) {
    val info = thread(t)
    if (info.empty) return

    // do not log exception as value to distinguish between exception and exit events
    log(t, info.peek.method, Exception, Some(exception), None)
    info.pop()
  }

  val objects = new mutable.HashSet[Integer]
  def make(o: VMValue): Object =
    if (o.typeHash == VMValue.PRIMITIVE_TYPE)
      o.value
    else if (objects.contains(o.value))
      o.value
    else {
      OBJECTS.write(o.value, o.typeHash, o.dim)
      objects.add(o.value)
      o.value
    }

  val strings = new mutable.HashSet[Integer]
  def string(id: Int, value: String) {
    if (!strings.contains(id)) {
      STRINGS.write(id, value)
      strings.add(id)
    }
  }
}

/** Call frame */
case class Frame(method: Long, counter: Int)

/** Call stack */
class ThreadStack {
  private[this] var rep: List[Frame] = Nil
  def depth = rep.size
  def push(info: Frame) {
    rep = info :: rep
  }
  def pop() {
    rep = rep.tail
  }
  def peek = rep.head
  def parent: Option[Int] = rep.headOption match {
    case None      => None
    case Some(top) => Some(top.counter)
  }
  def empty = rep.isEmpty
}
