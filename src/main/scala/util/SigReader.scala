package edu.mit.csail.cap.query
package util

import collection.mutable.{ ListBuffer, Stack }
import org.objectweb.asm.signature.{ SignatureVisitor, SignatureReader }
import org.objectweb.asm.Opcodes
import java.util.regex.Pattern

/** Java bytecode signature reader. */
object SigReader {
  // Outputs a return type and list of parameters as a string and type representation.    
  def readSignature(sig: String)(implicit meta: Metadata) = {
    val reader = new SignatureReader(sig)
    val visitor = new MethodInfoVisitor

    reader.accept(visitor)

    (visitor.typ, visitor.parameters.toList)
  }
}

private class BytecodeTypeSignature(f: (Type) => Unit)(implicit meta: Metadata) extends SignatureVisitor(Opcodes.ASM5) {
  private var typ: Type = null
  private var arrLevel = 0

  private def finish() {
    assert(typ != null)
    for (i <- 0 until arrLevel) typ = ArrayType(typ)
    f(typ)
  }
  override def visitClassType(t: String) = {
    var name = t.replace("/", ".")
    typ = ClassType(name, meta)
    finish()
  }
  override def visitArrayType = {
    arrLevel = arrLevel + 1
    this
  }
  override def visitBaseType(t: Char) {
    typ = t match {
      case 'V' => VoidType
      case _   => Type.primitives.find(_.bytecode == t.toString).get
    }
    finish()
  }
  override def visitTypeVariable(p1: String) = {}
  override def visitExceptionType = null
  override def visitReturnType = null
  override def visitParameterType = null
  override def visitInterface = null
  override def visitSuperclass = null
  override def visitInterfaceBound = null
  override def visitClassBound = null
  override def visitFormalTypeParameter(p1: String) = {}
  override def visitEnd {}
  override def visitTypeArgument(p1: Char) = null
  override def visitTypeArgument = {}
  override def visitInnerClassType(p1: String) = {}

}
private class MethodInfoVisitor[T](implicit meta: Metadata) extends SignatureVisitor(Opcodes.ASM5) {
  var typ: Type = null
  val parameters = new ListBuffer[Type]

  override def visitReturnType = new BytecodeTypeSignature(typ = _)
  override def visitParameterType = new BytecodeTypeSignature(parameters.append(_))
  override def visitEnd {}
  override def visitTypeArgument(p1: Char) = null
  override def visitTypeArgument {}
  override def visitInnerClassType(p1: String) {}
  override def visitClassType(p1: String) {}
  override def visitArrayType = null
  override def visitTypeVariable(p1: String) {}
  override def visitBaseType(p1: Char) {}
  override def visitExceptionType = null
  override def visitInterface = null
  override def visitSuperclass = null
  override def visitInterfaceBound = null
  override def visitClassBound = null
  override def visitFormalTypeParameter(p1: String) {}
}

