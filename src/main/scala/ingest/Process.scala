package edu.mit.csail.cap.query
package ingest

import db.Database
import util._
import java.io._
import java.net.ServerSocket
import java.net.Socket
import edu.mit.csail.cap.util.JavaEltHash
import edu.mit.csail.cap.wire._
import scala.collection.mutable
import edu.mit.csail.cap.instrument.Configuration

object Processor {
  def dataStream(file: String) =
    new DataInputStream(new BufferedInputStream(new FileInputStream(file)))

  def run(log: String, metadata: String,
          logFile: String = Configuration.LOG,
          metaFile: String = Configuration.METADATA) {
    assert(log != null && log != "", "empty log database")
    assert(metadata != null && metadata != "", "empty metadata database")

    val db = Database()
    val meta = new Metadata(new Errors)

    // read and save meta-data
    info(s"processing $metaFile into $metadata")
    meta.load(db, metadata)
    MetaProcessor(dataStream(metaFile), meta).run()
    meta.save(db, metadata)

    // read and save log
    info(s"processing $logFile")
    LogProcessor(dataStream(logFile), db, meta, log).run()

    // connect
    val c = db.connect(log, meta)
    c.invalidate()
    
    // initializing indexes (could be done lazily) 
    // info(s"creating index")
    // c.makeIndex()
    c.shutdown()
  }

  def run(log: String, metadata: String, port: Int) {
    val socket = new ServerSocket(port)

    info("waiting for connection at " + socket)
    val client = socket.accept()

    info("accepted a connection from: " + client)
    val in = client.getInputStream()

    ???
  }

  def main(args: Array[String]) {
    if (args.length == 2)
      run(args(0), args(1))
    else
      error("Usage: <main> log metadata [port]")
  }
}

case class LogProcessor(in: DataInputStream, db: Database, meta: Metadata, log: String) {
  var out = new SQLLogger(db, meta, log)

  def run() {
    try {
      while (true)
        Serializer.read(in) match {
          case afm: AccessFieldMessage =>
            out.accessField(afm.thread, out.make(afm.thisObj), afm.owner, afm.name, out.make(afm.oldValue), afm.line)
          case afm: AssignFieldMessage =>
            out.assignField(afm.thread, out.make(afm.thisObj), afm.owner, afm.name, out.make(afm.newValue), afm.line)
          case aam: AccessArrayMessage =>
            out.accessArray(aam.thread, out.make(aam.array), aam.index, out.make(aam.oldValue), aam.length, aam.line)
          case aam: AssignArrayMessage =>
            out.assignArray(aam.thread, out.make(aam.array), aam.index, out.make(aam.newValue), aam.length, aam.line)
          case emm: EnterMethodMessage =>
            out.enterMethod(emm.thread, out.make(emm.thisObj), emm.method, emm.params.map(out.make))
          case emm: ExitMethodMessage =>
            out.exitMethod(emm.thread, out.make(emm.returnValue))
          case em: ExceptionMessage =>
            out.exception(em.thread, out.make(em.exception))
          case sm: StringMessage =>
            out.string(sm.id, sm.value)
          case m: CommandMessage =>
            if (m.command == CommandMessage.SET_LOG) {
              out.commit()
              out = new SQLLogger(db, meta, m.param)
            } else if (m.command == CommandMessage.TAG)
              info(s"tag ${m.param} at ${out.last}")
          case m => assert(false, s"incorrect message in log $m")
        }
    } catch { case _: EOFException => }

    out.commit()
  }
}

case class MetaProcessor(in: DataInputStream, out: Metadata) {
  def run() {
    try {
      while (true)
        Serializer.read(in) match {
          case dtm: DeclareClassMessage =>
            declareClass(dtm.name, dtm.access, dtm.supers)
          case dmm: DeclareMethodMessage =>
            declareMethod(dmm.clazz, dmm.access, dmm.method, dmm.desc)
          case dfm: DeclareFieldMessage =>
            declareField(dfm.clazz, dfm.name, dfm.desc, dfm.access, dfm.value.value)
          case m => assert(false, s"incorrect message in meta $m")
        }
    } catch { case _: EOFException => }
  }

  def declareClass(name: String, access: Int, supers: Array[String]) {
    val sub = ClassType(name, out)
    out.declareClass(sub, access)
    for (sup <- supers if sup != "")
      out.declareSubtyping(sub, ClassType(sup, out))
  }

  def declareMethod(clazz: String, access: Int, name: String, sig: String) {
    val ct = ClassType(clazz, out)
    val id = JavaEltHash.hashMethod(clazz, name, sig)
    out.declareMember(Method(name, id, ct, sig), access)
  }

  def declareField(clazz: String, name: String, desc: String, access: Int, value: Object) {
    val ct = ClassType(clazz, out)
    val id = JavaEltHash.hashField(clazz, name)
    val typ = Type.parseType(desc, out)
    assert(typ.bytecode == desc, "validating type parsing")
    out.declareMember(Field(name, id, ct, typ), access)
  }
}
