package edu.mit.csail.cap.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import edu.mit.csail.cap.util.JavaEltHash;

/**
 * Instruments a class using ASM method body adaptors.
 * 
 * <p>
 * The following events are traced:
 * <ol>
 * <li>method enter -- argument list and signature information
 * <li>method exit -- return value and signature information
 * <li>exception -- thrown exception (program counter jumps to the first catch
 * block)
 * <li>field access
 * <li>field assign
 * <li>array read
 * <li>array write
 * </ol>
 * 
 * <p>
 * Primitive values are ignored at instrumentation level.
 * 
 * <p>
 * The instrumentation maintains the property that every entry event is paired
 * with one of the exit event. This is done by wrapping the code for every
 * method into a try-finally block, and reporting uncaught exceptions.
 * 
 * <p>
 * Due to the way constructors and super-constructors appear in the byte-code,
 * we skip events before the super-constructor call. For future references: if
 * the property above appears to be broken, it's most likely a problem with
 * field/array instrumentation combined with constructor calls.
 * 
 * <p>
 * Field resolution:
 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-5.html#jvms-5.4.3.2
 * Field initialization:
 * http://docs.oracle.com/javase/specs/jvms/se5.0/html/ClassFile.doc.html#9801
 * 
 * @author kuat
 * 
 */
final class Instrumentor extends ClassVisitor {
	private final static String RUNTIME = Runtime.class.getName().replace('.', '/');
	private final String className;

	public Instrumentor(String className, ClassWriter writer) {
		super(Opcodes.ASM5, writer);
		assert !className.contains("/") : "class name format";
		this.className = className;
	}

	private String externalize(String name) {
		return name == null ? null : name.replace('/', '.');
	}

	/** Visit the header of a class: register type */
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		if (Runtime.policy.listenDeclarations) {
			final String[] externalInterfaces = new String[interfaces.length];
			for (int i = 0; i < interfaces.length; i++) {
				externalInterfaces[i] = externalize(interfaces[i]);
			}
			// superName for Object class is null
			Runtime.declareType(externalize(name), access, superName == null ? "" : externalize(superName),
					externalInterfaces);
		}
	}

	/** Visit a field */
	@Override
	public FieldVisitor visitField(int access, String fieldName, String desc, String signature, Object value) {
		if (Runtime.policy.listenDeclarations)
			Runtime.declareField(className, fieldName, access, externalize(desc), value);
		return super.visitField(access, fieldName, desc, signature, value);
	}

	/** Visit a method */
	@Override
	public MethodVisitor visitMethod(final int access, final String methodName, final String desc, String generics,
			String[] exceptions) {
		final long mhash = JavaEltHash.hashMethod(className, methodName, desc);
		if (Runtime.policy.listenDeclarations)
			Runtime.declareMethod(className, access, methodName, desc);

		final MethodVisitor visitor = super.visitMethod(access, methodName, desc, generics, exceptions);

		// skip abstract and native methods
		if ((access & Opcodes.ACC_NATIVE) != 0)
			return visitor;
		if ((access & Opcodes.ACC_ABSTRACT) != 0)
			return visitor;

		// skip excluded methods
		final boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
		final int domain = Runtime.policy.methodDomain(className, methodName, desc);
		if (domain == Configuration.EXCLUSION)
			return visitor;

		return new AdviceAdapter(Opcodes.ASM5, visitor, access, methodName, desc) {
			private boolean initialized = false;
			private int line = -1;
			private final Label start = new Label();

			@Override
			protected void onMethodEnter() {
				initialized = true;

				if (!Runtime.policy.listenCfg)
					return;

				mv.visitLabel(start);

				int idx = generateArgumentArray(isStatic ? 0 : 1);
				mv.visitLdcInsn(domain);
				mv.visitLdcInsn(mhash);
				if (isStatic)
					mv.visitInsn(ACONST_NULL);
				else
					mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, idx);

				mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "enterMethod", "(IJLjava/lang/Object;[Ljava/lang/Object;)V",
						false);
			}

			private int generateArgumentArray(int idx) {
				Type[] argumentTypes = Type.getArgumentTypes(desc);

				mv.visitIntInsn(BIPUSH, argumentTypes.length);
				mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

				for (int i = 0; i < argumentTypes.length; i++) {
					Type argumentType = argumentTypes[i];

					mv.visitInsn(DUP);
					mv.visitIntInsn(BIPUSH, i);
					mv.visitVarInsn(argumentType.getOpcode(ILOAD), idx);

					box(argumentType);

					mv.visitInsn(AASTORE);
					idx += argumentType.getSize();
				}

				mv.visitVarInsn(ASTORE, idx);
				return idx;
			}

			/** Last event in the code */
			@Override
			public void visitMaxs(int maxStack, int maxLocals) {
				try {
					if (!Runtime.policy.listenCfg)
						return;

					// not creating locals
					Label end = new Label();
					mv.visitTryCatchBlock(start, end, end, null);
					// mark the very last instruction with a label
					// must be called after visit try catch to conform ASM spec
					mv.visitLabel(end);
					onFinally(ATHROW);
					mv.visitInsn(ATHROW);
				} finally {
					mv.visitMaxs(maxStack, maxLocals);
				}
			}

			@Override
			protected void onMethodExit(int opcode) {
				if (!Runtime.policy.listenCfg)
					return;

				if (opcode != ATHROW)
					onFinally(opcode);
			}

			private void onFinally(int opcode) {
				if (opcode == ARETURN) {
					mv.visitInsn(DUP);
					mv.visitLdcInsn(mhash);
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "exitMethod", "(Ljava/lang/Object;J)V", false);
				} else if (opcode == ATHROW) {
					mv.visitInsn(DUP);
					mv.visitLdcInsn(mhash);
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "exception", "(Ljava/lang/Throwable;J)V", false);
				} else if (opcode == IRETURN) {
					mv.visitInsn(DUP);
					mv.visitLdcInsn(mhash);
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "exitMethod", "(IJ)V", false);
				} else if (opcode == LRETURN) {
					mv.visitInsn(DUP2);
					mv.visitLdcInsn(mhash);
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "exitMethod", "(JJ)V", false);
				} else if (opcode == DRETURN) {
					mv.visitInsn(DUP2);
					mv.visitLdcInsn(mhash);
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "exitMethod", "(DJ)V", false);
				} else {
					mv.visitLdcInsn(mhash);
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "exitMethod", "(J)V", false);
				}
			}

			@Override
			public void visitLineNumber(int line, Label start) {
				this.line = line;
				super.visitLineNumber(line, start);
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				try {
					if (!Runtime.policy.listenFields)
						return;

					// instrument only application classes
					if (!Runtime.policy.isApplication(className))
						return;

					final String ownerName = externalize(owner);

					// only fields of the application classes
					if (!Runtime.policy.isApplication(ownerName))
						return;

					final int sort = Type.getType(desc).getSort();

					// only fields of object and array sorts
					if (sort != Type.OBJECT && sort != Type.ARRAY)
						return;

					final Long hash = Runtime.typeID(ownerName);
					final long ownerHash = (hash == null) ? Runtime.declareType(ownerName) : hash;

					// perform instrumentation
					if (opcode == GETSTATIC) {
						mv.visitInsn(ACONST_NULL);
						mv.visitFieldInsn(opcode, owner, name, desc);
						mv.visitLdcInsn(ownerHash);
						mv.visitLdcInsn(name);
						mv.visitLdcInsn(line);
						mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "accessField",
								"(Ljava/lang/Object;Ljava/lang/Object;JLjava/lang/String;I)V", false);
					} else if (opcode == GETFIELD) {
						mv.visitInsn(DUP);
						mv.visitInsn(DUP);
						mv.visitFieldInsn(opcode, owner, name, desc);
						mv.visitLdcInsn(ownerHash);
						mv.visitLdcInsn(name);
						mv.visitLdcInsn(line);
						mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "accessField",
								"(Ljava/lang/Object;Ljava/lang/Object;JLjava/lang/String;I)V", false);
					} else if (opcode == PUTSTATIC) {
						mv.visitInsn(ACONST_NULL);
						mv.visitInsn(DUP2);
						mv.visitInsn(POP);
						mv.visitLdcInsn(ownerHash);
						mv.visitLdcInsn(name);
						mv.visitLdcInsn(line);
						mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "assignField",
								"(Ljava/lang/Object;Ljava/lang/Object;JLjava/lang/String;I)V", false);
					} else if (opcode == PUTFIELD) {
						if (initialized) {
							mv.visitInsn(DUP2);
							mv.visitLdcInsn(ownerHash);
							mv.visitLdcInsn(name);
							mv.visitLdcInsn(line);
							mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "assignField",
									"(Ljava/lang/Object;Ljava/lang/Object;JLjava/lang/String;I)V", false);
						} else if (className.equals(ownerName)) {
							mv.visitInsn(DUP);
							mv.visitLdcInsn(mhash);
							mv.visitLdcInsn(ownerHash);
							mv.visitLdcInsn(name);
							mv.visitLdcInsn(line);
							mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "delayedAssignField",
									"(Ljava/lang/Object;JJLjava/lang/String;I)V", false);
						} else {
							Runtime.out.println("uninitialized owner field " + owner + " in " + className);
						}
					}
				} finally {
					super.visitFieldInsn(opcode, owner, name, desc);
				}
			}

			@Override
			public void visitInsn(int opcode) {
				// instrument only application classes
				if (!Runtime.policy.isApplication(className) || !initialized || !Runtime.policy.listenArrays) {
					super.visitInsn(opcode);
					return;
				}

				// array instructions
				if (opcode == AALOAD) {
					mv.visitInsn(DUP2);
					mv.visitLdcInsn(line);
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "accessArray", "([Ljava/lang/Object;II)V", false);
					super.visitInsn(opcode);
				} else if (opcode == AASTORE) {
					// actual assignment is done inside the method
					mv.visitLdcInsn(line);
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "assignArray",
							"([Ljava/lang/Object;ILjava/lang/Object;I)V", false);
				} else {
					super.visitInsn(opcode);
				}
			}

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				boolean test = Runtime.policy.trapSystemCalls;
				test = test && Runtime.policy.listenArrays;
				test = test && Runtime.policy.listenCfg;
				test = test && Runtime.policy.isApplication(className);
				test = test && initialized;

				if (test && opcode == INVOKESTATIC && owner.equals("java/lang/System") && name.equals("arraycopy")) {
					mv.visitMethodInsn(INVOKESTATIC, RUNTIME, name, desc, itf);
				} else {
					super.visitMethodInsn(opcode, owner, name, desc, itf);
				}
			}
		};
	}
}
