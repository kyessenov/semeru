package edu.mit.csail.cap.query
package analysis

/** Query a trace for a feature from a demo trace */
sealed trait CallQuery extends Ordered[CallQuery] {
  /** Identifying, descriptive methods */
  def methods: Set[Method]

  /**
   * Returns a stream of seed events for matches.
   * The first event in a match is the ancestor of all events in a match.
   */
  def search(t: BoundaryAnalysis): Traversable[List[Event]]

  /** Quick check before search (optimization) */
  def has(t: BoundaryAnalysis): Boolean

  /** Find number of matches */
  def count(t: BoundaryAnalysis): Int

  override def compare(that: CallQuery) =
    this.toString.compare(that.toString)

  /** Select events happening under this query (for extends and invokes) */
  def under(t: BoundaryAnalysis) =
    t.select(Or({
      for (e :: _ <- search(t)) yield e.asInstanceOf[Enter].contains
    }.toList))

  /** Select stack traces for this query (not including the element itself) */
  def over(t: BoundaryAnalysis) =
    t.select(Or({
      for (e :: _ <- search(t)) yield StackTrace(e)
    }.toList))
}

object EmptyQuery extends CallQuery {
  override def methods = Set()
  override def search(t: BoundaryAnalysis) = Nil
  override def has(t: BoundaryAnalysis) = false
  override def count(t: BoundaryAnalysis) = 0
  override def toString = ""
}

object CallQuery {
  def fromString(meta: Metadata, s: String): CallQuery =
    if (s.isEmpty)
      EmptyQuery
    else s(0) match {
      case '+' => Extends(meta.method(s.substring(1).toLong))
      case '=' => Invokes(meta.method(s.substring(1).toLong))
      case '[' =>
        val comma = s.indexOf(',')
        NestedQuery(
          fromString(meta, s.substring(1, comma)),
          fromString(meta, s.substring(comma + 1, s.size - 1)))
      case '{' =>
        val comma = s.indexOf(',')
        Extends(
          meta.method(s.substring(1, comma).toLong),
          Some(meta.clazz(s.substring(comma + 1, s.size - 1).toLong)))
      case 'r' =>
         Invokes(meta.method(s.substring(1).toLong), Not(ValueIs(Null)))
      case _ => ???
    }
}

/** Framework definition user extensions in the sub-types of the class */
case class Extends(definition: Method, declaration: Option[ClassType] = None) extends CallQuery {
  override def methods = Set(definition)
  
  def supertype = declaration.getOrElse(definition.declarer)

  def extensions(b: FrameworkBoundary = AllUser): Set[Method] =
    definition.overriding(b.isUser, supertype)

  override def search(t: BoundaryAnalysis) =
    t.member(extensions(t.b)).view.map(List(_))

  override def count(t: BoundaryAnalysis) =
    t.member(extensions(t.b)).size

  override def has(t: BoundaryAnalysis) =
    this match {
      case Extends(m, None) if m.toString == "java.lang.Object.toString()Ljava/lang/String;" => true
      case _ => (t.methods intersect extensions(t.b)).size > 0
    }

  override def toString =
    declaration match {
      case None    => "+" + definition.id
      case Some(t) => "{" + definition.id + "," + t.id + "}"
    }
}

/** Simple call to a framework (or its extension) */
case class Invokes(m: Method, filter: Query = True) extends CallQuery {
  override def methods = Set(m)

  val q = Set(m) ++ m.overriding()

  override def search(t: BoundaryAnalysis) =
    t.member(q).select(filter).view.collect {
      // add return seed as well
      case e: Enter => e :: (e.exit match {
        case Some(d) => d :: Nil
        case None    => Nil
      })
    }

  override def count(t: BoundaryAnalysis) =
    t.member(q).size

  /** TODO Very slow! Not clear why since it should be Lucene-based. Make an under approximation now  */
  override def has(t: BoundaryAnalysis) =
    t.methods(m)
  //(t.methods intersect q).size > 0

  override def toString = "=" + m.id
}

/** Group call queries by the declaring classes */
case class ClassQuery(clazz: ClassType, queries: Set[CallQuery]) extends CallQuery {
  override def methods =
    queries.flatMap(_.methods)

  override def search(t: BoundaryAnalysis) =
    queries.flatMap(_.search(t))

  override def count(t: BoundaryAnalysis) =
    queries.map(_.count(t)).sum

  /** TODO: performance optimization, over approximation */
  override def has(t: BoundaryAnalysis) =
    true
  //queries.exists(_.has(t))

  override def toString =
    queries.map(_.toString).mkString("(", ",", ")")
}

case class NestedQuery(parent: CallQuery, child: CallQuery) extends CallQuery {
  override def methods =
    parent.methods ++ child.methods

  override def search(t: BoundaryAnalysis) =
    for (
      r1 <- parent.search(t);
      r2 <- child.search(t.select(r1(0).asInstanceOf[Enter].contains))
    ) yield r1 ::: r2

  override def count(t: BoundaryAnalysis) =
    search(t).size

  /** XXX: over approximation ! */
  override def has(t: BoundaryAnalysis) =
    parent.has(t) && child.has(t)
  //!search(t).isEmpty

  override def toString =
    "[" + parent + "," + child + "]"
}

case class Score(
  // original query
  q: CallQuery,
  // maximum heuristics; higher -> more important
  heuristics: Double = 0,
  // minimum depth; lower -> more important
  depth: Int = 0,
  // # documents / # containing documents or 0; higher -> more specific
  IDF: Double = 0,
  // boolean for keyword match; higher -> more important
  keyword: Int = 0)

/** Distinct score queries */
case class Scores(scores: Set[Score]) {
  def idf(target: Group) =
    if (target.isEmpty)
      this
    else {
      debug("computing idf")
      val n = target.traces.size
      val out = Scores(scores.map {
        case score =>
          val count = target.traces.filter(score.q.has).size;
          score.copy(IDF = if (count == 0) 0 else Math.log(n.toDouble / count.toDouble))
      })
      debug("done")
      out
    }

  def keyword(meta: Metadata, q: String) =
    if (q.trim() == "")
      this
    else {
      val index = db.Lucene.index(meta, scores.flatMap(_.q.methods))
      val matches = index.search(index.containsOne(q))

      Scores(scores.map {
        case score =>
          score.copy(keyword = (matches intersect score.q.methods).size)
      })
    }

  def declarationClass(q: CallQuery): ClassType = q match {
    case q: Invokes                 => q.m.declarer.sourceClass
    case q: Extends                 => q.supertype.sourceClass
    case ClassQuery(clazz, _)       => clazz
    case EmptyQuery                 => ???
    case NestedQuery(parent, child) => declarationClass(parent)
  }

  /** Group queries by the defining class */
  def groupByClass = {
    // extract defining class for each call query
    val groups = scores.groupBy(score => declarationClass(score.q))

    Scores(groups.map {
      case (clazz, scores) => Score(
        q = ClassQuery(clazz, scores.map(_.q)),
        keyword = scores.map(_.keyword).max,
        depth = scores.map(_.depth).min,
        IDF = scores.map(_.IDF).max,
        heuristics = scores.map(_.heuristics).max)
    }.toSet)
  }

  /** Top-down sorted list */
  def sorted =
    scores.toList.sortBy(Ranking.lexicographic).reverse

  def size = scores.size
}

object Ranking {
  // - high is good
  def lexicographic(a: Score) = (
    // keyword matching first
    a.keyword,
    // extends first
    a.q match { case _: Extends => 2 case _: Invokes => 1 case _ => 0 },
    // specific first (absent or common get rank 0)
    a.IDF,
    // closer to the top get higher rank
    -a.depth,
    // name heuristic
    a.heuristics)
}

object Heuristics {
  /** [0,1] priority. Threshold .5 for getters/util, 1 for interesting stuff */
  def apply(m: Method): Double = m match {
    case m if m.isSynthetic => 0
    case m if m.isConstructor || m.isStaticInitializer => .25
    case m if m.name == "equals" && m.sig == "(Ljava/lang/Object;)Z" => .25
    case m if m.name == "hashCode" && m.sig == "()I" => .25
    case m if PrefixMask("java.lang.")(m.declarer) => .4
    case m if PrefixMask("java.util.")(m.declarer) => .4
    case m if m.name == "getAdapter" && m.sig == "(Ljava/lang/Class;)Ljava/lang/Object;" => .5
    case m if m.name.startsWith("get") => .75
    case m if m.name.startsWith("has") && m.returnType == BooleanType => .75
    case m if m.name.startsWith("is") && m.returnType == BooleanType => .75
    case m if m.name.startsWith("can") && m.returnType == BooleanType => .75
    case m if m.declarer.name.endsWith("Listener") => .75
    case _ => 1.0
  }
}
