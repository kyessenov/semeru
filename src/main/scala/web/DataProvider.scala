package edu.mit.csail.cap.query

import scala.collection.mutable
import scala.io.Source
import db._
import experiments._
import analysis._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.File
import util.FileReader

package object web {
  case class DataProvider(p: Parameters = Parameters()) extends Render {
    val errors = new Errors
    var db: Database = null
    var meta: MultiMetadata = null
    var config: Configuration = null

    reload()

    def reload() {
      shutdown()

      db = Database()
      meta = new MultiMetadata(errors)
      config = loadJSON[Configuration](p.ConfigFile)
      for (t <- config.databases)
        t.init(this)

      info(s"configured ${traces.size} traces, missing: ${missing.mkString(", ")}, extra: ${extra.mkString(", ")}")
    }

    def add(db: TraceConfig) {
      // force reload metadata
      meta.forget(db.metadata)
      db.init(this)
      config = config.copy(databases = db :: config.databases.filter(_.name != db.name))
      saveJSON(p.ConfigFile, config)
    }

    def remove(name: String) {
      config = config.copy(databases = config.databases.filter(_.name != name))
      saveJSON(p.ConfigFile, config)
    }

    /** Active traces */
    def traces: List[TraceConfig] =
      config.databases.filterNot(_.isMissing)

    /** Missing traces */
    def missing: List[TraceConfig] =
      config.databases.filter(_.isMissing)

    /** Extra catalogs */
    def extra =
      db.catalogs.toSet --
        traces.map(_.name) --
        traces.map(_.metadata)

    /** Get a trace by name */
    def apply(name: String): Option[TraceConfig] =
      traces.find(_.name == name)

    /** Get a group by name */
    def get(name: String): Group =
      apply(name) match {
        case Some(t) =>
          Group(t :: Nil)
        case None =>
          config.groups.find(_.name == name) match {
            case Some(TracePattern(_, pat)) =>
              Group(traces.filter(_.name.matches(pat)))
            case None =>
              Group(Nil)
          }
      }

    /** Package masks for known components (groups of packages) */
    def modules: List[ClassMasks] =
      config.modules.map(ClassMask(_))

    def shutdown() {
      if (config != null) {
        for (trace <- config.databases)
          trace.shutdown()
        db.shutdown()
      }
    }

    /** File output for ongoing trace recording */
    var LogApp: Option[Experiment] = None
    var LogBin: Option[String] = None
    var LogName: Option[String] = None

    override def toString =
      s"${traces.size} traces, ${missing.size} missing, " + meta
  }

  case class TraceConfig(
    name: String,
    customMeta: Option[String] = None,
    desc: Option[String] = None,
    user: Option[String] = None,
    framework: Option[String] = None)
      extends TraceAnalysis with CachingSynthesizer {
    assert(name.size > 0)

    /** Needs to be initialized */
    var data: DataProvider = null
    
    def init(data: DataProvider) {
      this.data = data
      if (p.EagerLoadMetadata)
        meta.load(data.db, metadata)
    }

    implicit def p = data.p

    override def toString = name

    private[this] var _t: Option[Connection] = None

    override def meta = data.meta

    /** Connection */
    override implicit def t: Connection = _t match {
      case Some(t) =>
        t
      case None if isMissing =>
        throw new RuntimeException(s"missing trace $name")
      case None =>
        meta.load(data.db, metadata)
        _t = Some(data.db.connect(name, meta));
        t
    }

    lazy val isMissing =
      !data.db.catalogs.contains(name)

    /** Framework boundary */
    implicit lazy override val b: FrameworkBoundary = (user, framework) match {
      case (Some(u), Some(f)) => Boundary(ClassMask(u), ClassMask(f))
      case (Some(u), None)    => UserPackages(ClassMask(u))
      case (None, Some(f))    => FrameworkPackages(ClassMask(f))
      case (None, None)       => AllUser
    }

    def description =
      name.split("_").map(_.trim).mkString(" ") + {
        desc match {
          case Some(desc) => ": " + desc
          case None       => ""
        }
      }

    /** Experiment that created this trace */
    def experiment = Experiment.parse(name) match {
      case Some(e) => e
      case _       => Experiments.Test
    }

    def metadata = customMeta match {
      case Some(meta) => meta
      case None       => experiment.meta
    }

    def isDemo = name.startsWith("demo")

    override lazy val trees = {
      val file = new File(c.cache, "trees.zip")
      if (file.exists) {
        debug(s"loading trees for $name")
        val roots = util.CallTraceSerializer(c).readZIP(file.getPath)
        debug("done")
        new BoundaryCallAnalysis(this, roots)
      } else {
        val trees = super.trees
        debug(s"caching trees for $name")
        util.CallTraceSerializer(c).writeZIP(trees.roots, file.getPath)
        debug("done")
        trees
      }
    }

    def shutdown() {
      _t match {
        case Some(t) =>
          t.shutdown()
          _t = None
        case None =>
      }
    }
  }

  case class Configuration(
      modules: List[String],
      databases: List[TraceConfig],
      groups: List[TracePattern]) {
    val names = databases.map(_.name) ++ groups.map(_.name)
    val dups = names.groupBy(identity).collect { case (x, _ :: _ :: _) => x }
    assert(dups.size == 0, s"duplicate names: $dups")
  }

  case class TracePattern(
    name: String,
    pat: String)
}
