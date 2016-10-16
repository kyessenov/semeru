package edu.mit.csail.cap.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import edu.mit.csail.cap.instrument.Runtime.CallStack;

public class Transformer implements ClassFileTransformer {
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		final CallStack stack = Runtime.tstack.get();
		assert !stack.self : "cannot invoke transform directly";
		try {
			stack.self = true;
			final String name = className.replace('/', '.');
			return instrument(name, classfileBuffer);
		} finally {
			stack.self = false;
		}
	}

	private byte[] instrument(String name, byte[] source) {
		try {
			final ClassReader reader = new ClassReader(source);
			final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
			final ClassVisitor visitor = new Instrumentor(name, writer);

			// line numbers are not visited when SKIP_DEBUG is enabled
			reader.accept(visitor, ClassReader.SKIP_FRAMES);
			return writer.toByteArray();
		} catch (Throwable t) {
			Runtime.out.println("[agent] error transforming: " + name + ", " + t.getMessage());
			Throwable cause = t;
			while (cause.getCause() != null)
				cause = cause.getCause();
			if (t != cause)
				Runtime.out.println("[agent] cause: " + cause.getMessage());

			return null;
		}
	}

	private File file(String name) {
		return new File("var/bytecode/" + name.replace('.', '/') + ".class");
	}

	@SuppressWarnings("unused")
	private byte[] load(String name) {
		try {
			final File file = file(name);
			if (!file.exists())
				return null;
			final FileInputStream is = new FileInputStream(file(name));
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			final byte[] cache = new byte[1024];
			while (true) {
				int bytesRead = is.read(cache);
				if (bytesRead == -1)
					break;
				bos.write(cache, 0, bytesRead);
			}
			is.close();
			return bos.toByteArray();
		} catch (Throwable t) {
			return null;
		}
	}

	@SuppressWarnings("unused")
	private void save(String name, byte[] code) {
		try {
			final File file = file(name);
			file.mkdirs();
			file.createNewFile();
			final FileOutputStream out = new FileOutputStream(file);
			out.write(code);
			out.close();
		} catch (Throwable t) {
		}
	}
}
