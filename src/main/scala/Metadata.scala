package edu.mit.csail.cap.query

import collection.{ mutable, immutable }
import db.{ Database, Schema, CSVTable, SQLConnection }

private case class ClassDecl(ct: ClassType) {
  // undeclared class types might be missing relationship to object
  var supers: List[ClassType] = Nil
  var subs: List[ClassType] = Nil
  var fields: List[Field] = Nil
  var methods: List[Method] = Nil
}

/** Incrementally populated meta-data cache */
class Metadata(val errors: Errors) {
  // mutable sorted maps
  private val masks = new mutable.LongMap[Int]
  private val fields = new mutable.LongMap[Field]
  private val methods = new mutable.LongMap[Method]
  private val decls = new mutable.LongMap[ClassDecl]
  private val primitives = new mutable.LongMap[Type] ++=
    (NullType :: UnknownType :: VoidType :: Type.primitives).map(t => t.id -> t)

  // initialize common classes
  val CollectionClass = make(classOf[java.util.Collection[_]])
  val ListClass = make(classOf[java.util.List[_]])
  val ArrayListClass = make(classOf[java.util.ArrayList[_]])
  val HashSetClass = make(classOf[java.util.HashSet[_]])
  val LinkedHashSetClass = make(classOf[java.util.LinkedHashSet[_]])
  val TreeSetClass = make(classOf[java.util.TreeSet[_]])
  val MapClass = make(classOf[java.util.Map[_, _]])
  val HashMapClass = make(classOf[java.util.HashMap[_, _]])
  val TreeMapClass = make(classOf[java.util.TreeMap[_, _]])
  val IteratorClass = make(classOf[java.util.Iterator[_]])
  val ObjectClass = make(classOf[java.lang.Object])
  val StringClass = make(classOf[java.lang.String])

  // initialize dummy field
  val dummy = Field("$dummy", 16, ObjectClass, synthetic = true)
  register(dummy)

  // initialize dummy method
  val dummify = Method("$dummify", 17, ObjectClass, "()V", VoidType, Nil, true)
  register(dummify)

  // initialize special container fields
  val containers = analysis.Containers(this)
  for (abs <- containers.all)
    register(abs.field)

  private def make(c: java.lang.Class[_]) = {
    val out = ClassType(c.getName, this)
    register(out)
    out
  }

  /** Register an element by its id */
  private[this] def register(j: JavaElement) {
    element(j.id) match {
      case Some(elt) =>
        assert(elt == j, s"id collision detected: old $elt and new $j both map to ${j.id}")
        (elt, j) match {
          case (elt: Field, j: Field) if elt.typ != j.typ =>
            // keep first but remember (old, new) pair
            errors.fieldCollision(elt, j.typ)
          case _ =>
        }
      case None => j match {
        case t: ClassType =>
          decls += t.id -> ClassDecl(t)
        case m: Method =>
          methods += m.id -> m
          decls(m.declarer.id).methods ::= m
        case f: Field =>
          fields += f.id -> f
          decls(f.declarer.id).fields ::= f
        case t: Type =>
          warn("adding non-class type " + t)
      }
    }
  }

  def declareClass(ct: ClassType, access: Int) {
    register(ct)
    if (access >= 0)
      masks += ct.id -> access
  }

  def declareSubtyping(sub: ClassType, sup: ClassType) {
    register(sub)
    register(sup)
    decls(sub.id).supers ::= sup
    decls(sup.id).subs ::= sub
  }

  def declareMember(m: Member, access: Int) {
    register(m)
    masks += m.id -> access
  }

  /** Retrieve element by ID. */
  def field(id: Long) = fields(id)
  def method(id: Long) = methods(id)
  def hasMethod(id: Long) = methods.contains(id)
  def typ(id: Long): Type = primitives.get(id) match {
    case Some(t) => t
    case None    => clazz(id)
  }
  def clazz(id: Long): ClassType = decls(id).ct

  def allMethods = methods.values
  def allFields = fields.values
  def allClasses = decls.values.view.map(_.ct)

  def member(id: Long) = (fields.get(id), methods.get(id)) match {
    case (Some(f), None) => f
    case (None, Some(m)) => m
    case (None, None)    => throw new NoSuchElementException
    case _               => throw new RuntimeException("id collision")
  }

  def element(id: Long): Option[JavaElement] =
    primitives.get(id) match {
      case Some(t) => Some(t)
      case None => decls.get(id) match {
        case Some(decl) => Some(decl.ct)
        case None => methods.get(id) match {
          case Some(m) => Some(m)
          case None => fields.get(id) match {
            case Some(f) => Some(f)
            case None    => None
          }
        }
      }
    }

  def mask(t: JavaElement) = masks.get(t.id)

  def methods(t: ClassType): List[Method] = decls(t.id).methods
  def fields(t: ClassType): List[Field] = decls(t.id).fields
  def subtypes(t: ClassType): Set[ClassType] = decls(t.id).subs.toSet
  def supertypes(t: ClassType): Set[ClassType] = decls(t.id).supers.toSet

  /** Retrieve element by name. */
  def clazz(name: String) = decls.values.find(_.ct.toString == name).map(_.ct)
  def field(name: String) = fields.values.find(_.toString == name)
  def method(name: String) = methods.values.find(_.toString == name)
  def classes(pattern: String): Traversable[ClassType] = decls.values.filter { _.ct.toString matches pattern }.map(_.ct)
  def fields(pattern: String): Traversable[Field] = fields.values.filter { _.toString matches pattern }
  def methods(pattern: String): Traversable[Method] = methods.values.filter { _.toString matches pattern }

  /** Load meta data */
  def load(db: Database, name: String) {
    if (db.catalogs.contains(name)) {
      info("loading metadata " + name + " into " + this)
      load(new SQLConnection(db, name))
    }
  }

  private def load(c: SQLConnection) {
    c("select name, id, access from TYPE") { t =>
      val ct = ClassType(t.getString(1), t.getLong(2), this)
      declareClass(ct, t.getInt(3))
    }

    c("select id, super from SUBTYPE") { st =>
      declareSubtyping(decls(st.getLong(1)).ct, decls(st.getLong(2)).ct)
    }

    c("select name, id, type, signature, access, value from FIELD") { f =>
      val name = f.getString(1)
      val id = f.getLong(2)
      val declarer = decls(f.getLong(3)).ct
      val typ = Type.parseType(f.getString(4), this)
      val access = f.getInt(5)
      // val value = fields.getInt(6)
      declareMember(Field(name, id, declarer, typ), access)
    }

    c("select name, id, signature, type, access from METHOD") { m =>
      val name = m.getString(1)
      val id = m.getLong(2)
      val sig = m.getString(3)
      val declarer = decls(m.getLong(4)).ct
      val access = m.getInt(5)
      declareMember(Method(name, id, declarer, sig), access)
    }
  }

  /** Save meta data to MySQL */
  def save(db: Database, name: String) {
    debug("saving metadata " + this)

    db.create(name)
    val DECLARED = CSVTable(Schema.TYPE)
    val UNDECLARED = CSVTable(Schema.TYPE, true)
    val SUBTYPE = CSVTable(Schema.SUBTYPE)
    val METHOD = CSVTable(Schema.METHOD)
    val FIELD = CSVTable(Schema.FIELD)

    for ((id, decl) <- decls) {
      masks.get(id) match {
        case Some(mask) =>
          DECLARED.write(id, decl.ct.name, mask)
        case None =>
          UNDECLARED.write(id, decl.ct.name, 0)
      }
      for (sup <- decl.supers)
        SUBTYPE.write(id, sup.id)
    }

    for (m <- methods.values if !m.synthetic)
      METHOD.write(m.id, m.name, m.declarer.id, m.access, m.sig)

    for (f <- fields.values if !f.synthetic)
      FIELD.write(f.id, f.name, f.declarer.id, f.access, f.typ.bytecode, 0)

    val c = db.connect(name)
    val stmt = c.createStatement()
    DECLARED.load(stmt)
    UNDECLARED.load(stmt)
    SUBTYPE.load(stmt)
    METHOD.load(stmt)
    FIELD.load(stmt)

    debug("committing metadata")
    c.commit()
    c.close()
  }

  override def toString =
    fields.size + " fields, " +
      methods.size + " methods, " +
      decls.size + " classes"

}

/** Metadata cache spanning multiple databases */
class MultiMetadata(errors: Errors) extends Metadata(errors) {
  private val metas = new mutable.HashSet[String]

  override def load(db: Database, name: String) {
    if (!metas(name)) {
      super.load(db, name)
      metas += name
    }
  }

  def forget(name: String) {
    metas -= name
  }
}
