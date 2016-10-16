package edu.mit.csail.cap.instrument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import edu.mit.csail.cap.util.Pattern;

/**
 * Tracing configuration is loaded and processed before transformation of
 * classes.
 * 
 * <b>Explanation of exclusions and library</b>:
 * 
 * Classes in the observed application can be split into three disjoint
 * categories:
 * <ul>
 * <li>excluded classes (as specified by exclusion black list and not falling in
 * other categories) -- not monitored
 * <li>library classes (as specified by library without exclusions) -- lightly
 * monitored
 * <li>application classes (as specified by application, library takes
 * precedence) -- heavily monitored; by default it is everything
 * </ul>
 * 
 * Library classes are monitored only one level deep. We only record the top
 * most calls to the library code. We do not record internal calls in the
 * library to the library but calls from the library to the application are
 * recorded.
 * 
 * Field events are only registered for instances of application classes. We
 * record only accesses within application classes. We do not record fields that
 * have values of the excluded types.
 * 
 * Array events are only registered within application classes. We only record
 * arrays that have application base type.
 * 
 * Thread-safe.
 * 
 * @author kuat
 * 
 */
public final class Configuration {
	/**
	 * Agent server receives events from clients and sends them to a database
	 */
	public final String agentServer;
	public final int agentPort;

	public static final String LOG = "/tmp/log.bin";
	public static final String METADATA = "/tmp/metadata.bin";

	/** Use files instead of sockets */
	private final boolean useFiles;

	/** If true, control flow events will be generated */
	public final boolean listenCfg;
	/** If true, field events will be generated */
	public final boolean listenFields;
	/** If true, array events will be generated */
	public final boolean listenArrays;
	/**
	 * If true, only events for arrays of application classes will be generated
	 */
	public final boolean listenApplicationArrays;
	/** If true, meta-data messages will be generated */
	public final boolean listenDeclarations;

	/** If set, starts the agent management. */
	public final boolean management;
	public final int managementPort;

	/**
	 * If true, record string values (maps from hash code to actual value) for
	 * bounded length strings
	 */
	public final boolean recordStrings;
	public final int recordMaxStringSize;

	/** Method domains */
	public static final int EXCLUSION = 0;
	public static final int APPLICATION = 1;
	public static final int LIBRARY = 2;

	/** Excluded classes */
	private final Pattern[] blacklist;

	/** Library classes */
	private final Pattern[] library;

	/** Application classes */
	private final Pattern[] application;

	/** Primitive classes */
	private static final String[] PRIMITIVES = new String[] { "boolean", "byte", "char", "double", "float", "int",
			"long", "short" };

	/** Work-around System.arraycopy */
	public final boolean trapSystemCalls = true;

	private static final String BLACKLIST = "org.objectweb.asm.*, edu.mit.csail.cap.*, " + "java.lang.*, " + "sun.*, "
			+ "com.sun.*, " + "java.util.concurrent.locks.*, " + "java.security.*, " + "java.io.*, " + "java.nio.*, "
			+ "java.net.*";

	/**
	 * Library overrides application settings; therefore, these should be
	 * conservative
	 */
	private static final String DEFAULT_LIBRARY = "java.util.*, " + "java.math.*, " + "java.sql.*, " + "java.text.*, "
			+ "javax.sql.*, " + "javax.xml.*, " + "org.xml.*, " + "org.w3c.dom.*";

	/** Methods are marked specially ignoring rules for classes */
	private static final MethodSig[] OVERRIDE = new MethodSig[] {
			new MethodSig("java.lang.reflect.Method", "invoke", null, APPLICATION),
			new MethodSig("java.util.Arrays", "copyOf", "([BI)[B", EXCLUSION),
			new MethodSig("java.util.Arrays", "copyOf", "([CI)[C", EXCLUSION),
			new MethodSig("java.util.Arrays", "copyOf", "([II)[I", EXCLUSION),
			new MethodSig("java.util.Arrays", "copyOfRange", "([BII)[B", EXCLUSION),
			new MethodSig("java.util.Arrays", "copyOfRange", "([CII)[C", EXCLUSION),
			new MethodSig("java.util.Arrays", "copyOfRange", "([III)[I", EXCLUSION),
			new MethodSig(null, "hashCode", "()I", EXCLUSION),
			new MethodSig(null, "equals", "(Ljava/lang/Object;)Z", EXCLUSION) };

	/** Initialize by storing constants. Avoid dependencies on system code */
	public Configuration(String fileName) throws FileNotFoundException, IOException {
		System.out.println("[agent] config file " + fileName);
		final Properties p = new Properties();
		FileInputStream fis = new FileInputStream(new File(fileName));
		p.load(fis);
		fis.close();

		this.agentServer = p.getProperty("agent.server", "localhost");
		this.agentPort = Integer.valueOf(p.getProperty("agent.port", "13337"));
		this.useFiles = Boolean.valueOf(p.getProperty("agent.files", "true"));

		this.listenCfg = Boolean.valueOf(p.getProperty("listen.cfg", "true"));
		this.listenFields = Boolean.valueOf(p.getProperty("listen.fields", "true"));
		this.listenArrays = Boolean.valueOf(p.getProperty("listen.arrays", "true"));
		this.listenApplicationArrays = Boolean.valueOf(p.getProperty("listen.arrays.application", "false"));
		this.listenDeclarations = Boolean.valueOf(p.getProperty("listen.declarations", "true"));
		this.recordStrings = Boolean.valueOf(p.getProperty("listen.strings", "true"));
		this.recordMaxStringSize = Integer.valueOf(p.getProperty("listen.stringMax", "64"));

		System.out.print("[agent] recording:");
		if (listenCfg)
			System.out.print(" cfg");
		if (listenFields)
			System.out.print(" fields");
		if (listenArrays)
			System.out.print(" arrays");
		if (listenApplicationArrays)
			System.out.print(" only_application_arrays");
		if (listenDeclarations)
			System.out.print(" declarations");
		if (recordStrings)
			System.out.print(" strings");
		System.out.println();

		this.blacklist = Pattern.makePatterns(p.getProperty("listen.exclude", "") + "," + BLACKLIST);
		System.out.print("[agent] blacklist:");
		for (Pattern pat : blacklist)
			System.out.print(" " + pat);
		System.out.println();

		this.library = Pattern.makePatterns(p.getProperty("listen.library", "") + "," + DEFAULT_LIBRARY);
		System.out.print("[agent] library:");
		for (Pattern pat : library)
			System.out.print(" " + pat);
		System.out.println();

		this.application = Pattern.makePatterns(p.getProperty("listen.application", "*"));
		System.out.print("[agent] app:");
		for (Pattern pat : application)
			System.out.print(" " + pat);
		System.out.println();

		System.out.print("[agent] override:");
		for (MethodSig sig : OVERRIDE)
			System.out.print(" " + sig);
		System.out.println();

		this.management = Boolean.valueOf(p.getProperty("agent.management", "false"));
		this.managementPort = Integer.valueOf(p.getProperty("agent.management.port", "13338"));
		if (management)
			System.out.println("[agent] management port: " + managementPort);
	}

	/** Return the domain of the method */
	public int methodDomain(String className, String methodName, String sig) {
		for (MethodSig m : OVERRIDE)
			if (m.matches(className, methodName, sig)) {
				// System.out.println("method rule " + className + "." +
				// methodName +
				// sig);
				return m.domain;
			}

		if (isLibrary(className))
			return LIBRARY;
		else if (isApplication(className))
			return APPLICATION;
		else
			return EXCLUSION;
	}

	public boolean primitive(String name) {
		for (String primitive : PRIMITIVES)
			if (primitive.equals(name))
				return true;
		return false;
	}

	public boolean array(String name) {
		return name.startsWith("[");
	}

	private boolean blacklisted(String name) {
		assert name.indexOf('/') == -1;
		if (array(name) || primitive(name))
			return true;

		// exclusion patterns
		for (Pattern p : blacklist)
			if (p.accept(name))
				return true;

		return false;
	}

	/** Class is a library */
	private boolean isLibrary(String name) {
		assert name.indexOf('/') == -1;

		// no black list
		if (blacklisted(name))
			return false;

		// library patterns
		for (Pattern p : library)
			if (p.accept(name))
				return true;

		return false;
	}

	/** Class is application. */
	public boolean isApplication(String name) {
		assert name.indexOf('/') == -1;

		// no black list
		if (blacklisted(name))
			return false;

		// no library
		if (isLibrary(name))
			return false;

		// application pattern
		for (Pattern p : application)
			if (p.accept(name))
				return true;

		return false;
	}

	/**
	 * Determine if we record array event for an element reference in the array
	 */
	public boolean recordArray(Object ref) {
		if (listenApplicationArrays)
			return ref != null && isApplication(ref.getClass().getName());
		else
			return true;
	}

	/** True if using a file dump instead of a server connection. */
	public boolean useFiles() {
		return useFiles;
	}

	static class MethodSig {
		final String className;
		final String methodName;
		final String sig;
		final int domain;

		MethodSig(String className, String method, String sig, int domain) {
			this.className = className;
			this.methodName = method;
			this.sig = sig;
			this.domain = domain;
		}

		public boolean matches(String c, String m, String s) {
			return (className == null ? true : className.equals(c)) && methodName.equals(m)
					&& (sig == null ? true : sig.equals(s));
		}

		@Override
		public String toString() {
			return (className == null ? className : "*") + "." + methodName + (sig == null ? "*" : sig) + ":" + domain;
		}
	}
}
