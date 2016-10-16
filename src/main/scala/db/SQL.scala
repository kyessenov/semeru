package edu.mit.csail.cap.query
package db

import java.sql.{ DriverManager, ResultSet, PreparedStatement }
import scala.collection.mutable.ListBuffer
import org.apache.commons.dbcp2._

/** MySQL database with connection pooling */
case class Database(
    user: String = "root",
    password: String = "",
    server: String = "localhost") {

  private val pool = new BasicDataSource
  pool.setDriverClassName("com.mysql.jdbc.Driver")
  pool.setInitialSize(8)
  pool.setUsername(user)
  pool.setPassword(password)
  pool.setDefaultAutoCommit(false)
  pool.setUrl("jdbc:mysql://" + server + "/" + "?useUnicode=true&characterEncoding=UTF-8")

  def connect(): java.sql.Connection =
    pool.getConnection
    
  def connect(name: String): java.sql.Connection = {
    val out = connect()
    out.setCatalog(name)
    out
  }

  private def execute(cmd: String) {
    val sql = connect()
    try {
      val stmt = sql.createStatement()
      stmt.execute(cmd)
      stmt.close()
    } finally
      sql.close()
  }

  /** Create database */
  def create(name: String) {
    debug("create database " + name)
    execute(s"CREATE DATABASE IF NOT EXISTS `$name` CHARACTER SET utf8")
  }

  /** Drop database */
  def drop(name: String) {
    debug("drop database " + name)
    execute(s"DROP DATABASE IF EXISTS ${name}")
  }

  /** List databases */
  def catalogs: Set[String] = {
    val sql = connect()
    var out: List[String] = Nil
    
    try {
      val rs = sql.getMetaData.getCatalogs
      while (rs.next)
        out ::= rs.getString(1)
    } finally
      sql.close()
      
    out.toSet -- 
      Set("information_schema", "performance_schema", "mysql")
  }

  /** Connect to a trace */
  def connect(name: String, meta: Metadata) =
    new Connection(this, name, meta)
  
  def shutdown() {
    pool.close()
  }

  override def toString = server
}

/** Connection to MySQL database. */
class SQLConnection(val db: Database, val name: String) {
  @inline private[this] def using[B](sql: String)(block: PreparedStatement => B): B = {
    val connection = db.connect(name)
    val stmt = connection.prepareStatement(sql)
    
    try {
      block(stmt)
    } finally {
      stmt.close()
      connection.close()
    }
  }

  /** Execute a statement (using online connection.) */
  def execute(stmt: String) {
    debug(s"SQL update to $name: " + stmt.replace("\n", " "))
    using(stmt)(_.execute())
  }

  /** Retrieve result of a general query (the entire result set is loaded into memory.) */
  def read[T](query: String)(block: ResultSet => T): List[T] =
    using(query) { q =>
      val rs = q.executeQuery
      val ret = new ListBuffer[T]
      while (rs.next) {
        ret += block(rs)
      }
      rs.close()
      ret.toList
    }

  /** Retrieve an integer as a result of a query. */
  def readInt(query: String) = read[Int](query) { _.getInt(1) } match {
    case ret :: Nil => ret
    case _          => throw new RuntimeException("none or multiple results")
  }

  /**
   * Retrieve result set row by row instead of putting everything into memory
   * @see http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-implementation-notes.html
   */
  def apply(query: String)(block: ResultSet => Unit) {
    using(query) { q =>
      // API guarantees that the result set is by default
      // of type TYPE_FORWARD_ONLY and has a concurrency level of CONCUR_READ_ONLY.
      q.setFetchSize(Integer.MIN_VALUE)
      val rs = q.executeQuery()
      var i = 0
      while (rs.next) {
        block(rs)
        i = i + 1
        if (i % 1000000 == 0) {
          info("streaming " + i + " rows from database...")
        }
      }
      rs.close()
    }
  }
  
  def shutdown() {
    // do nothing
  }
}

