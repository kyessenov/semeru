package edu.mit.csail.cap.query
package db

import scala.reflect.ClassTag

/** Relational table schema */
case class Schema(name: String, fields: Column[_]*) {
  require(primary.size > 0, "requires primary key(s)")

  /** Primary key */
  def primary = fields.filter(_.primary)

  /** Creation script */
  def sql = {
    val entries = fields.toList.map(_.sql) ++ {
      if (!primary.isEmpty)
        List("PRIMARY KEY (" + primary.map(_.quotedName).mkString(", ") + ")")
      else
        Nil
    }
    val out = new StringBuilder
    out ++= s"CREATE TABLE IF NOT EXISTS `$name` (\n"
    out ++= entries.mkString(",\n")
    out ++= ") ENGINE=MyISAM"
    out.toString
  }
}

/** Table column parameterized by data type */
case class Column[T](name: String, nullable: Boolean = false,
                     primary: Boolean = false, size: Int = 255)(implicit val tag: ClassTag[T]) {
  /** Mapping to SQL data types */
  def sort: String = tag match {
    case ClassTag.Int  => "int"
    case ClassTag.Byte => "tinyint"
    case ClassTag.Long => "bigint"
    case ClassTag.Char => s"varchar($size)"
    case _             => throw new RuntimeException(s"unsupported schema type${tag}")
  }

  def quotedName = "`" + name + "`"

  /** Column creation line */
  def sql = quotedName + " " + sort + " " + (if (nullable) "DEFAULT NULL" else "NOT NULL")

  /** Escape to SQL */
  def escape(a: Any): String =
    (a, tag) match {
      case (a: Byte, ClassTag.Byte)   => a.toString
      case (a: Int, ClassTag.Int)     => a.toString
      case (a: Long, ClassTag.Long)   => a.toString
      case (a: String, ClassTag.Char) => a.replace("\t", "\\t").replace("\n", "\\n")
      case _                          => throw new RuntimeException(s"wrong entry $a for column $name")
    }

  /** Extract value according to the data type*/
  def extract(a: Any): String = (a, nullable) match {
    case (None, true)    => "\\N"
    case (Some(a), true) => escape(a)
    case (a, false)      => escape(a)
    case _               => throw new RuntimeException(s"not nullable $a")
  }
}

case class CSVTable(schema: Schema, ignore: Boolean = false) {
  val root = new java.io.File("csv")
  val file = new java.io.File(root, s"${schema.name}-${duplicate}.csv")
  val writer = new java.io.FileWriter(file)
  private[this] var counter = 0

  file.createNewFile()
  root.mkdirs()

  /** Specify how to deal with duplicates */
  def duplicate = if (ignore) "IGNORE" else "REPLACE"

  def load(stmt: java.sql.Statement) {
    writer.close()
    debug(s"create and load $counter rows into ${schema.name} from ${file}")
    stmt.execute(schema.sql)
    try {
      stmt.execute(s"LOAD DATA LOCAL INFILE '${file.getAbsolutePath()}' ${duplicate} INTO TABLE `${schema.name}`")
    } finally {
      file.delete()
    }
  }

  def write(row: Any*) {
    val n = row.size
    val m = schema.fields.size
    assert(n == m, "dynamic schema cast error: row size for " + schema.name)
    for (i <- 0 until n) {
      if (i > 0) writer.write('\t')
      writer.write(schema.fields(i).extract(row(i)))
    }
    writer.write('\n')
    counter = counter + 1
    if ((counter % 1000000) == 0)
      info(s"${schema.name} counter = ${counter / 1000000}M")
  }
}

object Schema {
  val LOG = Schema("LOG",
    Column[Int]("counter", primary = true),
    Column[Byte]("event_type"),
    Column[Long]("id"),
    Column[Int]("stack_depth"),
    Column[Int]("receiver", nullable = true),
    Column[Int]("value", nullable = true),
    Column[Long]("thread"),
    Column[Int]("caller", nullable = true),
    Column[Int]("line", nullable = true),
    Column[Int]("param0", nullable = true),
    Column[Int]("param1", nullable = true),
    Column[Int]("succ", nullable = true))

  val PARAMS = Schema("PARAMS",
    Column[Int]("counter", primary = true),
    Column[Int]("id"),
    Column[Byte]("arg", primary = true))

  val OBJECTS = Schema("OBJECTS",
    Column[Int]("id", primary = true),
    Column[Long]("type"),
    Column[Int]("dims"))

  val STRINGS = Schema("STRINGS",
    Column[Int]("id", primary = true),
    Column[Char]("value", size = 255))

  val FIELD = Schema("FIELD",
    Column[Long]("id", primary = true),
    Column[Char]("name", size = 255),
    Column[Long]("type"),
    Column[Int]("access"),
    Column[Char]("signature", size = 512),
    Column[Int]("value"))

  val METHOD = Schema("METHOD",
    Column[Long]("id", primary = true),
    Column[Char]("name", size = 255),
    Column[Long]("type"),
    Column[Int]("access"),
    Column[Char]("signature", size = 2048))

  val TYPE = Schema("TYPE",
    Column[Long]("id", primary = true),
    Column[Char]("name", size = 512),
    Column[Int]("access"))

  val SUBTYPE = Schema("SUBTYPE",
    Column[Long]("id", primary = true),
    Column[Long]("super", primary = true))

  val POST_PROCESS = List(
    """|ALTER TABLE `LOG` 
          |ADD INDEX caller (caller),
          |ADD INDEX elt (id, event_type)""".stripMargin,
    """|UPDATE `LOG` L1, `LOG` L2 
          |SET L1.succ = L2.counter, L1.value = L2.value
          |WHERE L1.event_type = 1 AND L2.caller = L1.counter AND (L2.event_type = 2 OR L2.event_type = 4)""".stripMargin,
    "ALTER TABLE `OBJECTS` ADD INDEX types (type)",
    "ANALYZE TABLE `LOG`")

  val HEAP = Schema("HEAP",
    Column[Int]("from", primary = true),
    Column[Int]("to", primary = true),
    Column[Long]("field", primary = true),
    Column[Int]("start"),
    Column[Int]("end"))
}

