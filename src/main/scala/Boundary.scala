package edu.mit.csail.cap.query

import java.util.regex.Pattern
import util.Memo
import scala.collection.mutable

/**
 * Framework-user disjoint code separation. Permits a category of code that is neither user nor framework.
 * Framework code can invoke user code either via exposed interface or via meta-programming (reflection)
 */
trait FrameworkBoundary {
  def isUser(t: ClassType): Boolean
  def isFramework(t: ClassType): Boolean

  def isUser(m: Member): Boolean = isUser(m.declarer)
  def isUser: Query = Predicate {
    case e: Enter => isUser(e.method)
    case e        => e.parent.map(isUser).getOrElse(false)
  }
  def isFramework(m: Member): Boolean = isFramework(m.declarer)
  def isFramework: Query = Predicate {
    case e: Enter => isFramework(e.method)
    case e        => e.parent.map(isFramework).getOrElse(false)
  }

  /** Crosses user-framework boundary */
  def cut(a: Enter, b: Enter) =
    (isUser(a) && isFramework(b)) || (isFramework(a) && isUser(b))

  /** Boundary between user and non-user */
  def _cover(e: Enter): Enter = e.parent match {
    case Some(d) if isUser(e) && isUser(d)   => cover(d)
    case Some(d) if !isUser(e) && !isUser(d) => cover(d)
    case _                                   => e
  }

  /** Top-most call event in user and non-user partitions. Identity on event whose parents are in different partition. */
  def cover: Enter => Enter = Memo(_cover)

  /** Cover for enter or parent enter event */
  def parentCover(e: Event): Option[Enter] = e match {
    case e: Enter => Some(cover(e))
    case e => e.parent match {
      case Some(d) => Some(cover(d))
      case _       => None
    }
  }

  /** Returns top-level framework overridden method */
  def overrides(m: Method): Option[Method] =
    if (m.isStatic)
      None
    else if (isFramework(m))
      Some(m)
    else
      m.overrides match {
        case Some(n) => overrides(n)
        case None    => None
      }

}

case class UserPackages(userPkgs: ClassMask) extends FrameworkBoundary {
  override def isUser(t: ClassType) = userPkgs(t)
  override def isFramework(t: ClassType) = !isUser(t)
}

case class FrameworkPackages(frameworkPkgs: ClassMask) extends FrameworkBoundary {
  override def isUser(t: ClassType) = !isFramework(t)
  override def isFramework(t: ClassType) = frameworkPkgs(t)
}

object AllFramework extends FrameworkBoundary {
  override def isUser(t: ClassType) = false
  override def isFramework(t: ClassType) = true
}

object AllUser extends FrameworkBoundary {
  override def isUser(t: ClassType) = true
  override def isFramework(t: ClassType) = false
}

/** Prioritizes user packages. */
case class Boundary(userPkgs: ClassMask, frameworkPkgs: ClassMask) extends FrameworkBoundary {
  override def isUser(t: ClassType) = userPkgs(t)
  override def isFramework(t: ClassType) = frameworkPkgs(t) && !userPkgs(t)
}

/** Masks is one of "a.b.c","a.b.*", "a.b, a.c.*" */
sealed trait ClassMask extends (ClassType => Boolean) {
  def apply(m: Member): Boolean = apply(m.declarer)
  def unary_! = NegativeClassMask(this)
}

case class NegativeClassMask(mask: ClassMask) extends ClassMask {
  override def apply(m: ClassType) = !mask.apply(m)
  override def toString = "Not(" + mask + ")"
}

case class PrefixMask(mask: String) extends ClassMask {
  override def apply(t: ClassType) = t.name.startsWith(mask)
  override def toString = mask + "*"
}

case class PackageName(name: Package) extends ClassMask {
  override def apply(t: ClassType) = t.packag == name
  override def toString = name
}

case class ClassMasks(masks: List[ClassMask]) extends ClassMask {
  assert(masks.size > 0)
  def apply(t: ClassType) = masks.exists(p => p(t))
  override def toString = masks.mkString(",")
}

object ClassMask {
  def apply(s: String): ClassMasks =
    ClassMasks(s.replaceAll("\\s", "").split(',').toList.filter(!_.isEmpty).map {
      case s if s.endsWith("*") => PrefixMask(s.dropRight(1))
      case s                    => PackageName(s)
    })
}

