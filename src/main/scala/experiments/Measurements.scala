package edu.mit.csail.cap.query
package experiments
import util._
import analysis._
import db._

case class MatchMakerMeasure(meta: Metadata) {
  import meta._

  CriticalChain.TIME_BOUND = 30 * 1000
  CriticalChain.PATH_BOUND = Integer.MAX_VALUE
  CriticalChain.QUEUE_CLEANUP = true

  type Data = (TraceAnalysis, Type, Type, Option[Path[Object]], Duration)

  def measure(databases: List[TraceAnalysis], a: ClassType, b: ClassType): List[Data] =
    for (
      db <- databases;
      as = a.allSubtypes;
      bs = b.allSubtypes;
      x = (as union bs).toSet[Type];
      s <- as;
      t <- bs;
      (chain, dur) = timed(db.chain(s, t, skip = x diff Set(t)))
    ) yield (db, s, t, chain, dur)

  def measureCritical(data: List[Data], sameEnd: Boolean): List[Data] = {
    for (
      (db, s, t, chain, dur) <- data;
      if (chain.isDefined);
      (cchain, cdur) = timed(db.criticalChain(chain.get, keepEnd = sameEnd))
    ) yield (db, s, t, Some(cchain), cdur)
  }

  def sum(l: List[Long]) = l.foldLeft(0L)(_ + _)
  def average(l: List[Long]) = sum(l) / l.size

  def writeCSV(data: List[Data]) {
    import java.io._
    val f = new java.io.PrintWriter("out.csv")
    for ((db, s, t, chain, dur) <- data) {
      f.print(db)
      f.print(",")
      f.print(s)
      f.print(",")
      f.print(t)
      f.print(",")
      chain match {
        case Some(c) => f.print(c.length)
        case None    =>
      }
      f.print(",")
      f.print(dur)
      f.println
    }
    f.flush
    f.close
  }

  def lprint(s: Any) = Console.print(s.toString + " & ")

  def writeLatex(data: List[Data]) {
    val found = data.filter(_._4.isDefined)
    val notfound = data.filter(!_._4.isDefined)
    val timeout = notfound.filter(_._5.duration > CriticalChain.TIME_BOUND)
    val timeok = notfound.filter(_._5.duration <= CriticalChain.TIME_BOUND)
    assert(notfound.size == timeout.size + timeok.size)
    assert(data.size == found.size + notfound.size)
    lprint(found.size)
    lprint(average(found.map(_._5.duration)) + " (" + found.map(_._5.duration).max + ")")
    lprint(timeout.size + " (" + timeout.size.toDouble / notfound.size.toDouble + " \\%)")
    lprint(average(timeok.map(_._5.duration)))
  }
}
