package edu.mit.csail.cap.query
package util

import collection.mutable.ListBuffer
import java.io._
import java.io.{ DataOutputStream => Out, DataInputStream => In }
import db._
import analysis._
import java.util.zip.GZIPOutputStream
import java.util.zip.GZIPInputStream

/** JSON serialization */
trait JSON {
  import org.json4s._
  import org.json4s.Extraction.decompose
  import org.json4s.native._
  implicit def formats: Formats =
    Serialization.formats(NoTypeHints) 

  def fromJSON[T](json: String)(implicit mf: Manifest[T]): T =
    JsonMethods.parse(json).extract[T]

  def toJSON(x: Any): String =
    JsonMethods.compact(JsonMethods.render(decompose(x)))

  def prettyJSON(x: Any): String =
    JsonMethods.pretty(JsonMethods.render(decompose(x)))

  def loadJSON[T](file: String)(implicit mf: Manifest[T]): T =
    fromJSON[T](FileReader.load(file))

  def saveJSON(file: String, data: Any) =
    FileReader.save(file, prettyJSON(data))
}

/** Simple file reader */
object FileReader {
  import java.nio.file.{ Files, Paths }

  def load(file: String) =
    new String(Files.readAllBytes(Paths.get(file)))

  def save(file: String, data: String) =
    Files.write(Paths.get(file), data.getBytes)

}

/** Binary serialization (for classes in our domain.) */
trait Serializer[V] {
  def write(v: V, out: Out)
  def read(in: In): V

  def writeFile(v: V, path: String) {
    writeFile(v, new Out(new BufferedOutputStream(new FileOutputStream(new File(path)))))
  }
  
  def writeZIP(v: V, path: String) {
    val zip = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(path))))
    writeFile(v, new Out(zip))
  }

  def writeFile(v: V, out: Out) {
    write(v, out)
    out.close()
  }

  def readFile(path: String): V = 
		readFile(new In(new BufferedInputStream(new FileInputStream(path))))

	def readZIP(path: String): V = 
	  readFile(new In(new GZIPInputStream(new BufferedInputStream(new FileInputStream(path)))))
		
  def readFile(in: In): V = {
    val out = read(in)
    in.close()
    out
  }
}

object SegmentSerializer extends Serializer[Segment] {
  def write(v: Segment, out: Out) {
    out.writeInt(v.low)
    out.writeInt(v.high)
  }

  def read(in: In) = {
    val low = in.readInt
    val high = in.readInt
    Segment(low, high)
  }
}

object IntervalSerializer extends Serializer[NormalForm] {
  def write(v: NormalForm, out: Out) {
    val l = v.segments
    out.writeInt(l.size)
    for (r <- l)
      SegmentSerializer.write(r, out)
  }

  def read(in: In) = {
    val out = new ListBuffer[Segment]
    val count = in.readInt
    for (_ <- 1 to count) {
      val r = SegmentSerializer.read(in)
      out += r
    }
    NormalForm(out.toList)
  }
}

case class FieldSerializer(meta: Metadata) extends Serializer[Field] {
  def write(v: Field, out: Out) = out.writeLong(v.id)
  def read(in: In) = meta.field(in.readLong)
}

case class TypeSerializer(meta: Metadata) extends Serializer[Type] {
  def write(v: Type, out: Out) = {
    out.writeLong(v.baseType.id)
    out.writeInt(v.dimension)
  }
  def read(in: In) = {
    val t = meta.typ(in.readLong)
    val d = in.readInt
    t.array(d)
  }
}

case class MethodSerializer(meta: Metadata) extends Serializer[Method] {
  def write(v: Method, out: Out) = out.writeLong(v.id)
  def read(in: In) = meta.method(in.readLong)
}

object ObjectSerializer extends Serializer[Object] {
  def write(v: Object, out: Out) = out.writeInt(v)
  def read(in: In) = in.readInt
}

case class CallTreeSerializer(c: Connection) extends Serializer[CallTree] {
  val sub = MethodSerializer(c.meta)

  def write(t: CallTree, out: Out) = {
    sub.write(t.method, out)
    out.writeInt(t.counter)

    val children = t.children
    out.writeInt(children.size)
    for (child <- children)
      write(child, out)
  }

  def read(in: In) = {
    val m = sub.read(in)
    val counter = in.readInt
    val out = new CallTree(m, c, counter)

    val children = in.readInt
    for (i <- 0 until children)
      out.add(read(in))

    out
  }
}

case class CallTraceSerializer(c: Connection) extends Serializer[List[CallTree]] {
  val sub = CallTreeSerializer(c)

  def write(roots: List[CallTree], out: Out) = {
    out.writeInt(roots.size)
    for (root <- roots)
      sub.write(root, out)
  }

  def read(in: In) = {
    val count = in.readInt
    val roots =
      for (i <- 0 until count)
        yield sub.read(in)

    roots.toList
  }
}

case class ClusterSerializer(c: Connection) extends Serializer[Cluster] {
  val sub = TypeSerializer(c.meta)
  def write(c: Cluster, out: Out) = c match {
    case InstanceCluster(o, _) =>
      out.writeBoolean(true)
      ObjectSerializer.write(o, out)
    case TypeCluster(t) =>
      out.writeBoolean(false)
      sub.write(t, out)
  }
  def read(in: In) = in.readBoolean match {
    case true  => InstanceCluster(ObjectSerializer.read(in), c)
    case false => TypeCluster(sub.read(in))
  }
}

case class HeapAbstractionSerializer(c: Connection) extends Serializer[HeapAbstraction] {
  val cluster = ClusterSerializer(c)
  def write(h: HeapAbstraction, out: Out) {
    out.writeInt(h.size)
    for (Edge(from, l, to) <- h) {
      cluster.write(from, out)
      cluster.write(to, out)
      IntervalSerializer.write(l, out)
    }
  }
  def read(in: In) = {
    val n = in.readInt
    val g = new HeapAbstractionImpl(c)
    for (i <- 0 until n) {
      val from = cluster.read(in)
      val to = cluster.read(in)
      val l = IntervalSerializer.read(in)
      g.add(from, l, to)
    }
    g
  }
}

