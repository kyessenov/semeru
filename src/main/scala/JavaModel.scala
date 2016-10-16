package edu.mit.csail.cap.query

import scala.collection.mutable
import edu.mit.csail.cap.util.JavaEltHash
import util._

/** Representation of a Java symbol */
sealed trait JavaElement {
  def id: Long
  def name: String
  override def hashCode = id.hashCode

  /** Identify camel code words in a name */
  def words: Set[String] = "([A-Z]+[a-z]*)|([A-Z]*[a-z]+)".r.findAllIn(name).toSet
}

sealed trait OrderByID[T <: OrderByID[T]] extends JavaElement with Ordered[T] {
  override def compare(that: T) = this.id.compare(that.id)
}

object Type {
  val primitives = List(
    ByteType,
    CharType,
    DoubleType,
    FloatType,
    IntType,
    LongType,
    ShortType,
    BooleanType)

  /** Parse types in internal Java representation. See Class.getName()  */
  def parseType(name: String, meta: Metadata): Type =
    if (name.startsWith("["))
      ArrayType(parseType(name.substring(1), meta))
    else if (name.startsWith("L") && name.endsWith(";"))
      ClassType(name.substring(1, name.length - 1), meta)
    else
      primitives.find(_.bytecode == name) match {
        case Some(t) => t
        case None    => throw new RuntimeException("can't parse type " + name)
      }

  /** Parse method signature */
  def parseSignature(sig: String, meta: Metadata) =
    util.SigReader.readSignature(sig)(meta)

  def hash(name: String): Long = JavaEltHash.hashClass(name)

  val camelCodePattern = "([A-Z]+[a-z]*)|([A-Z]*[a-z]+)".r
}

sealed trait Type extends JavaElement {
  /** Full name */
  def name: String

  /** Byte code name */
  def bytecode: String

  /** Name without package qualifiers */
  def shortName: String = name

  /** Pretty printed name */
  def prettyName: String = name

  /** Package qualifier */
  def packag: Package

  /** Base type (non-array) and dimension */
  def baseType: BaseType
  def dimension: Int

  /** Base of the array type or unknown */
  def base: Type

  /** Array type with this as a base */
  def array(i: Int): Type = i match {
    case i if i < 0 => throw new IllegalArgumentException("i < 0")
    case 0          => this
    case i          => ArrayType(array(i - 1))
  }
  def isPrimitive: Boolean
  def isArray: Boolean
  override def toString = name
}

sealed trait BaseType extends Type {
  override def base = UnknownType
  override def baseType = this
  override def dimension = 0
  override def isArray = false
}

sealed trait PrimitiveType extends BaseType {
  val packag = ""
  lazy val id = Type.hash(name)
  def valueOf(o: Object): String = o.toString
  override def isPrimitive = true
}

object VoidType extends PrimitiveType {
  val name = "void"
  val bytecode = "V"
  override def valueOf(o: Object) =
    if (o.isUnknown)
      ""
    else
      o.toString
}

object ByteType extends PrimitiveType {
  val name = "byte"
  val bytecode = "B"
}

object ShortType extends PrimitiveType {
  val name = "short"
  val bytecode = "S"
}

object IntType extends PrimitiveType {
  val name = "int"
  val bytecode = "I"
}

object LongType extends PrimitiveType {
  val name = "long"
  val bytecode = "J"
}

object FloatType extends PrimitiveType {
  val name = "float"
  val bytecode = "F"
}

object DoubleType extends PrimitiveType {
  val name = "double"
  val bytecode = "D"
}

object BooleanType extends PrimitiveType {
  val True = 1
  val False = 0
  val name = "boolean"
  val bytecode = "Z"
  override def valueOf(o: Object) = o match {
    case True  => "true"
    case False => "false"
    case _     => o.toString
  }
}

object CharType extends PrimitiveType {
  val name = "char"
  val bytecode = "C"
  override def valueOf(o: Object) =
    "'" + o.toChar.toString + "'"
}

sealed trait InstanceType extends Type {
  override def isPrimitive = false
}

object NullType extends InstanceType with BaseType {
  val id = 0L
  val name = "Null"
  val bytecode = ""
  val packag = ""
}

object UnknownType extends InstanceType with BaseType {
  val id = -1L
  val name = "??"
  val bytecode = ""
  val packag = ""
}

case class ArrayType(base: Type) extends InstanceType {
  assert(base != NullType && base != VoidType, "incorrect base type")
  lazy val id = Type.hash(bytecode)
  override def name = base.name + "[]"
  override def bytecode = "[" + base.bytecode
  override def shortName = base.shortName + "[]"
  override def prettyName = base.prettyName + "[]"
  override def baseType = base.baseType
  override def dimension = base.dimension + 1
  override def isArray = true
  override def packag = base.packag
}

case class ClassType(name: String, id: Long, meta: Metadata)
    extends InstanceType with BaseType with AccessMask with OrderByID[ClassType] {
  assert(!name.contains('/') && !name.contains('[') && name.size > 0, s"incorrect class name $name")
  override def bytecode = "L" + name + ";"

  /** Class name or inner class name */
  def sourceName =
    name.substring(Math.max(name.lastIndexOf("."), name.lastIndexOf("$")) + 1)

  override def shortName =
    name.substring(name.lastIndexOf(".") + 1)

  override def prettyName =
    if (sourceName.matches("[0-9]*")) shortName.replace("$", "") else sourceName

  override def packag = name.lastIndexOf(".") match {
    case -1 => ""
    case i  => name.substring(0, i)
  }

  def source =
    name.substring(name.lastIndexOf(".") + 1,
      if (name.contains("$")) name.indexOf("$") else name.size)

  /** Class type for the defining class (if it's an inner class) */
  def sourceClass: ClassType =
    name.indexOf("$") match {
      case -1 => this
      case i => meta.clazz(name.substring(0, i)) match {
        case Some(clazz) => clazz
        case None        => this
      }
    }

  /** Direct subtypes */
  def subtypes: Set[ClassType] = meta.subtypes(this)

  /** Direct supertypes */
  def supertypes: Set[ClassType] = meta.supertypes(this)

  def methods: List[Method] = meta.methods(this)

  def fields: List[Field] = meta.fields(this)

  /** All subtypes excluding this */
  def allSubtypes: Set[ClassType] =
    subtypes ++ subtypes.flatMap(_.allSubtypes)

  /** All supertypes excluding this */
  def allSupertypes: Set[ClassType] =
    supertypes ++ supertypes.flatMap(_.allSupertypes)

  def subtypeHierarchy: LinkedTreeImpl[ClassType] = {
    val out = new LinkedTreeImpl[ClassType](this)
    for (s <- subtypes)
      out.add(s.subtypeHierarchy)
    out
  }

  /** Reflexive subtyping */
  def isSubtype(that: ClassType): Boolean =
    this == that || supertypes.exists(_.isSubtype(that))

  /** Resolve field */
  def field(name: String): Option[Field] =
    fields.find(_.name == name) match {
      case Some(f) => Some(f)
      case None =>
        for (
          s <- supertypes;
          f <- s.field(name)
        ) return Some(f)
        return None
    }

  def method(name: String, params: Type*): Method = methods.find {
    case m: Method => m.name == name && m.parameterTypes == params.toList
  } match {
    case Some(m) => m
    case None =>
      warn("missing method " + name + " in " + this)
      meta.dummify
  }

  def constructors: List[Method] = methods.filter(_.isConstructor)

  override def access = meta.mask(this).getOrElse(AccessMask.ACC_PUBLIC)
}

object ClassType {
  def apply(name: String, meta: Metadata): ClassType =
    ClassType(name, Type.hash(name), meta)

  /** Compute minimal subset whose super types contains the set */
  def bound(ts: Set[ClassType]): Set[ClassType] = {
    val supertypes = ts.map(t => (t, t.allSupertypes)).toMap
    ts.filter(s => !ts.exists(t => supertypes(t).contains(s)))
  }
}

sealed trait Member extends JavaElement with AccessMask {
  def member: Query = MemberIs(this)
  def declarer: ClassType
  def meta = declarer.meta
  override def access = meta.mask(this).getOrElse(AccessMask.ACC_PUBLIC)
}

final case class Method(name: String, id: Long, declarer: ClassType, sig: String,
                        returnType: Type, parameterTypes: List[Type], synthetic: Boolean)
    extends Member with OrderByID[Method] {

  override def toString = declarer + "." + name + sig

  def isConstructor = (name == "<init>")
  def isStaticInitializer = (name == "<clinit>")

  def arguments: Int = parameterTypes.size

  /** Check if this can override that assuming subtyping */
  def subsignature(that: Method) = {
    // See 8.4.8.1 Overriding (by Instance Methods)
    !this.isConstructor && !this.isStatic && !that.isStatic && !that.isPrivate &&
      this.name == that.name && this.parameterTypes == that.parameterTypes
  }

  /** Find overridden method in type t or any of t's super-types. */
  def overrides(t: ClassType): Option[Method] = {
    for (
      n <- t.methods if subsignature(n)
    ) return Some(n)
    for (
      u <- t.supertypes;
      n <- overrides(u)
    ) return Some(n)
    return None
  }

  /** Direct overridden method in the typing hierarchy */
  lazy val overrides: Option[Method] =
    declarer.supertypes.collectFirst { overrides(_) match { case Some(n) => n } }

  /** Chain of overridden methods ending with this in the order from abstract to concrete */
  def overridden: List[Method] = {
    var out: List[Method] = Nil
    var cur = this
    while (cur != null) {
      out = cur :: out
      cur.overrides match {
        case Some(n) => cur = n
        case None    => cur = null
      }
    }
    out
  }

  /**
   * All methods overriding this method
   * in the subclasses of the given type (excluding the super-type itself)
   */
  def overriding(mask: ClassType => Boolean = _ => true, supertype: ClassType = declarer): Set[Method] =
    for (
      t <- supertype.allSubtypes;
      if mask(t);
      n <- t.methods;
      if n.subsignature(this)
    ) yield n

  /** Most abstract method in the overrides chain */
  lazy val definition: Method =
    overrides match {
      case Some(m) => m.definition
      case None    => this
    }

  def isUser(implicit b: FrameworkBoundary) = b.isUser(this)

  def isFramework(implicit b: FrameworkBoundary) = b.isFramework(this)
  
  override def words =
    super.words ++ declarer.words
}

object Method {
  def apply(name: String, id: Long, declarer: ClassType, sig: String): Method = {
    val (returnType, parameterTypes) = Type.parseSignature(sig, declarer.meta)
    Method(name, id, declarer, sig, returnType, parameterTypes, false)
  }
}

/**
 * Field type changes across JVMs for the core library.
 *  Default field value is not consistent as well.
 */
final case class Field(name: String, id: Long, declarer: ClassType,
                       synthetic: Boolean = false) extends Member with OrderByID[Field] {
  override def toString = declarer + "." + name
  private var _typ: Type = UnknownType

  /** Field type from signature */
  def typ: Type = _typ
}

object Field {
  def apply(name: String, id: Long, declarer: ClassType, typ: Type): Field = {
    val out = Field(name, id, declarer)
    out._typ = typ
    out
  }
}

object AccessMask {
  /** Access opcodes @see org.objectweb.asm.Opcodes    */
  val ACC_PUBLIC = 1
  val ACC_PRIVATE = 2
  val ACC_PROTECTED = 4
  val ACC_STATIC = 8
  val ACC_FINAL = 16
  val ACC_NATIVE = 256
  val ACC_INTERFACE = 512
  val ACC_ABSTRACT = 1024
  val ACC_SYNTHETIC = 4096
  val ACC_ENUM = 16384
}

trait AccessMask {
  def access: Int
  import AccessMask._
  def isPublic = (access & ACC_PUBLIC) != 0
  def isPrivate = (access & ACC_PRIVATE) != 0
  def isProtected = (access & ACC_PROTECTED) != 0
  def isStatic = (access & ACC_STATIC) != 0
  def isFinal = (access & ACC_FINAL) != 0
  def isInterface = (access & ACC_INTERFACE) != 0
  def isAbstract = (access & ACC_ABSTRACT) != 0
  def isEnum = (access & ACC_ENUM) != 0
  def isNative = (access & ACC_NATIVE) != 0
  def isSynthetic = (access & ACC_SYNTHETIC) != 0
  def prefix: String = {
    val out = new mutable.ArrayBuffer[String]
    if (isSynthetic) out += "synthetic"
    if (isNative) out += "native"
    if (isPublic) out += "public"
    if (isPrivate) out += "private"
    if (isProtected) out += "protected"
    if (isStatic) out += "static"
    if (isFinal) out += "final"
    if (isInterface)
      out += "interface"
    else if (isAbstract)
      out += "abstract"
    out.toList.mkString(" ")
  }
}
