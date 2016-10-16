package edu.mit.csail.cap.query
package util

import org.slf4j.LoggerFactory

trait Logger {
  private val log = LoggerFactory.getLogger("semeru")
  
  def debug(msg: => String) {
    if (log.isDebugEnabled)
      log.debug(msg.toString)
  }

  def debug(cond: => Boolean, msg: => String) {
    if (log.isDebugEnabled)
      if (cond)
        log.debug(msg.toString)
  }

  def info(msg: => String) {
    if (log.isInfoEnabled)
      log.info(msg)
  }

  def warn(msg: => String) {
    if (log.isWarnEnabled)
      log.warn(msg)
  }

  def warn(cond: => Boolean, msg: => String) {
    if (log.isWarnEnabled)
      if (cond)
        log.warn(msg)
  }

  def error(msg: => String) {
    if (log.isErrorEnabled)
      log.error(msg)
  }

  /** Execute and time computation */
  def timed[X](f: => X): (X, Duration) = {
    val current = System.nanoTime
    val res = f
    val duration = System.nanoTime - current
    (res, new Duration(duration))
  }
}

