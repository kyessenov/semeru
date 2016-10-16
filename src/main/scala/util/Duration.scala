package edu.mit.csail.cap.query
package util

class Duration(val duration: Long) extends AnyVal with Ordered[Duration] {
  override def toString = {
    (duration, duration / 1000L, duration / (1000L * 1000L), duration / (1000 * 1000 * 1000L)) match {
      case (ns, 0, 0, 0)                             => "%d ns".format(ns)
      case (ns, us, 0, 0) if (ns == us * 1000)       => "%d us".format(us)
      case (ns, us, 0, 0)                            => "%3.3f us".format(ns / 1000d)
      case (_, us, ms, 0) if us == ms * 1000         => "%d ms".format(ms)
      case (_, us, ms, 0)                            => "%3.3f ms".format(us / 1000d)
      case (_, _, ms, s) if s < 60 && ms == s * 1000 => "%d s".format(s)
      case (_, _, ms, s) if s < 60                   => "%3.3f s".format(ms / 1000d)
      case (_, _, ms, _) =>
        val MillisPerSecond = 1000L
        val MillisPerMinute = 60L * MillisPerSecond
        val MillisPerHour = 60L * MillisPerMinute
        val MillisPerDay = 24L * MillisPerHour
        List(MillisPerDay, MillisPerHour, MillisPerMinute, MillisPerSecond).foldLeft((ms, List.empty[Long])) {
          case ((ms: Long, bits: List[_]), millisPer: Long) =>
            (ms % millisPer, (ms / millisPer) :: bits.asInstanceOf[List[Long]])
        }._2.reverse match {
          case List(0, h, m, s) =>
            "%02d:%02d:%02d".format(h, m, s)
          case List(d, h, m, s) =>
            "%d days %02d:%02d:%02d".format(d, h, m, s)
          case _ => "%d ms".format(ms)
        }
    }
  }
  def +(that: Duration) = new Duration(this.duration + that.duration)
  override def compare(that: Duration) = this.duration.compare(that.duration)
}
