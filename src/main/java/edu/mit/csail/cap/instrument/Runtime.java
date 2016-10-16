package edu.mit.csail.cap.instrument;

import java.io.PrintStream;

import edu.mit.csail.cap.util.JavaEltHash;
import edu.mit.csail.cap.util.Stack;
import edu.mit.csail.cap.util.trove3.gnu.trove.TCollections;
import edu.mit.csail.cap.util.trove3.gnu.trove.map.TObjectLongMap;
import edu.mit.csail.cap.util.trove3.gnu.trove.map.hash.TObjectLongHashMap;
import edu.mit.csail.cap.wire.AccessArrayMessage;
import edu.mit.csail.cap.wire.AccessFieldMessage;
import edu.mit.csail.cap.wire.AssignArrayMessage;
import edu.mit.csail.cap.wire.AssignFieldMessage;
import edu.mit.csail.cap.wire.DeclareClassMessage;
import edu.mit.csail.cap.wire.DeclareFieldMessage;
import edu.mit.csail.cap.wire.DeclareMethodMessage;
import edu.mit.csail.cap.wire.EnterMethodMessage;
import edu.mit.csail.cap.wire.ExceptionMessage;
import edu.mit.csail.cap.wire.ExitMethodMessage;
import edu.mit.csail.cap.wire.StringMessage;
import edu.mit.csail.cap.wire.VMValue;

/**
 * Instrumentation runtime.
 * 
 * This must be thread-safe since methods are called concurrently from
 * application threads.
 * 
 * Requires initialization.
 * 
 * Initial exit events without corresponding enter events are skipped.
 * 
 * Method enter/exit events are guarded by self variable. State events are not
 * since only instrumented classes can generate state events.
 * 
 * @author kuat
 */
public final class Runtime {
	// owned by Client
	static volatile boolean DISABLE = false;

	public static Configuration policy;
	public static PrintStream out;
	private static Client client;

	private static TObjectLongMap<String> CLASS_CACHE;
	private static long BOOLEAN_TYPE;
	private static long SHORT_TYPE;
	private static long BYTE_TYPE;
	private static long CHARACTER_TYPE;
	private static long INT_TYPE;
	private static long FLOAT_TYPE;
	private static long LONG_TYPE;
	@SuppressWarnings("unused")
	private static long VOID_TYPE;
	private static long DOUBLE_TYPE;

	static final ThreadLocal<CallStack> tstack = new ThreadLocal<CallStack>() {
		@Override
		protected CallStack initialValue() {
			return new CallStack();
		}
	};

	static class CallStack {
		/* True while in the instrumentation code. */
		boolean self = false;

		Stack<Integer> domains = new Stack<Integer>();
		Stack<Long> hashes = new Stack<Long>();
		Stack<Boolean> records = new Stack<Boolean>();

		Stack<AssignFieldMessage> delayed = new Stack<AssignFieldMessage>();
		Stack<Long> delayedHashes = new Stack<Long>();

		void push(int domain, long hash) {
			// Always record application calls
			// Record library calls if it is:
			// * top of the stack
			// * after an application call
			// * force included and previous is not force included

			final boolean record;
			if (domain == Configuration.APPLICATION)
				record = true;
			else {
				assert domain == Configuration.LIBRARY : "domain is either application or library";
				final Integer prev = domains.peek();
				if (prev == null) {
					record = true;
				} else if (prev == Configuration.APPLICATION)
					record = true;
				else
					record = false;
			}

			domains.push(domain);
			hashes.push(hash);
			records.push(record);
		}

		void pop(long hash) {
			domains.pop();
			records.pop();
			final Long old = hashes.pop();
			assert old == null || old.equals(hash) : "unexpected pop of " + old + " instead of " + hash + ": " + this;
		}

		/** True if the current stack frame should be recorded. */
		boolean record() {
			final Boolean cur = records.peek();
			if (cur == null)
				return false;
			else
				return cur;
		}

		@Override
		public String toString() {
			return hashes.toString();
		}
	}

	static void initialize(Configuration conf, Client cl, PrintStream out) {
		Runtime.policy = conf;
		Runtime.client = cl;
		Runtime.out = out;

		Runtime.CLASS_CACHE = TCollections.synchronizedMap(new TObjectLongHashMap<String>());

		Runtime.BOOLEAN_TYPE = JavaEltHash.hashClass(Boolean.TYPE.getName());
		Runtime.SHORT_TYPE = JavaEltHash.hashClass(Short.TYPE.getName());
		Runtime.CHARACTER_TYPE = JavaEltHash.hashClass(Character.TYPE.getName());
		Runtime.BYTE_TYPE = JavaEltHash.hashClass(Byte.TYPE.getName());
		Runtime.INT_TYPE = JavaEltHash.hashClass(Integer.TYPE.getName());
		Runtime.LONG_TYPE = JavaEltHash.hashClass(Long.TYPE.getName());
		Runtime.FLOAT_TYPE = JavaEltHash.hashClass(Float.TYPE.getName());
		Runtime.VOID_TYPE = JavaEltHash.hashClass(Void.TYPE.getName());
		Runtime.DOUBLE_TYPE = JavaEltHash.hashClass(Double.TYPE.getName());
	}

	public static void declareType(String clazz, int access, String superClass, String[] interfaces) {
		DeclareClassMessage m = new DeclareClassMessage();
		m.name = clazz;
		m.access = access;
		m.supers = new String[interfaces.length + 1];
		m.supers[0] = superClass;
		for (int i = 0; i < interfaces.length; i++)
			m.supers[i + 1] = interfaces[i];
		client.send(m);

		long hash = JavaEltHash.hashClass(clazz);
		Runtime.CLASS_CACHE.put(clazz, hash);
	}

	public static long declareType(Class<?> c) {
		assert !c.isPrimitive() && !c.isArray() : "must be true class " + c;
		final String name = c.getName().replace('/', '.');
		if (policy.listenDeclarations) {
			DeclareClassMessage m = new DeclareClassMessage();
			m.name = name;
			m.access = c.getModifiers();
			m.supers = new String[0];
			client.send(m);
		}

		final long hash = JavaEltHash.hashClass(name);
		Runtime.CLASS_CACHE.put(name, hash);
		return hash;
	}

	public static long declareType(String name) {
		if (policy.listenDeclarations) {
			DeclareClassMessage m = new DeclareClassMessage();
			m.name = name;
			m.access = -1;
			m.supers = new String[0];
			client.send(m);
		}

		final long hash = JavaEltHash.hashClass(name);
		Runtime.CLASS_CACHE.put(name, hash);
		return hash;
	}

	public static Long typeID(Class<?> c) {
		if (c.equals(Boolean.TYPE))
			return Runtime.BOOLEAN_TYPE;
		else if (c.equals(Short.TYPE))
			return Runtime.SHORT_TYPE;
		else if (c.equals(Byte.TYPE))
			return Runtime.BYTE_TYPE;
		else if (c.equals(Integer.TYPE))
			return Runtime.INT_TYPE;
		else if (c.equals(Character.TYPE))
			return Runtime.CHARACTER_TYPE;
		else if (c.equals(Long.TYPE))
			return Runtime.LONG_TYPE;
		else if (c.equals(Float.TYPE))
			return Runtime.FLOAT_TYPE;
		else if (c.equals(Double.TYPE))
			return Runtime.DOUBLE_TYPE;
		else
			return typeID(c.getName());
	}

	public static Long typeID(String name) {
		if (Runtime.CLASS_CACHE.containsKey(name))
			return Runtime.CLASS_CACHE.get(name);
		else
			return null;
	}

	public static void declareMethod(String clazz, int access, String method, String desc) {
		DeclareMethodMessage m = new DeclareMethodMessage();
		m.clazz = clazz;
		m.method = method;
		m.desc = desc;
		m.access = access;
		client.send(m);
	}

	public static void declareField(String clazz, String name, int access, String desc, Object value) {
		DeclareFieldMessage m = new DeclareFieldMessage();
		m.clazz = clazz;
		m.name = name;
		m.access = access;
		m.desc = desc;
		m.value = VMValue.make(value);
		client.send(m);
	}

	public static void string(int id, String value) {
		if (DISABLE)
			return;

		StringMessage m = new StringMessage();
		m.id = id;
		m.value = value;
		client.send(m);
	}

	public static void enterMethod(int domain, long hash, Object thisObj, Object[] params) {
		if (DISABLE) {
			tstack.remove();
			return;
		}

		final CallStack stack = tstack.get();

		if (!stack.self) {
			stack.self = true;

			try {
				stack.push(domain, hash);
				if (stack.record()) {
					EnterMethodMessage m = new EnterMethodMessage();
					m.thread = Thread.currentThread().getId();
					m.method = hash;
					m.thisObj = VMValue.make(thisObj);
					m.params = VMValue.makeAll(params);
					client.send(m);
				}

				// handle field assignments preceding <init>
				while (stack.delayedHashes.peek() != null && stack.delayedHashes.peek() == hash) {
					stack.delayedHashes.pop();
					AssignFieldMessage m = stack.delayed.pop();
					m.thisObj = VMValue.make(thisObj);
					client.send(m);
				}
			} finally {
				stack.self = false;
			}

		}
	}

	public static void exitMethod(Object o, long hash) {
		if (DISABLE) {
			tstack.remove();
			return;
		}

		final CallStack stack = tstack.get();
		if (!stack.self) {
			stack.self = true;

			try {
				if (stack.record()) {
					ExitMethodMessage m = new ExitMethodMessage();
					m.thread = Thread.currentThread().getId();
					m.returnValue = VMValue.make(o);
					client.send(m);
				}
				stack.pop(hash);
			} finally {
				stack.self = false;
			}
		}
	}

	public static void exitMethod(long hash) {
		exitMethod(VMValue.UNKNOWN, hash);
	}

	public static void exitMethod(int i, long hash) {
		exitMethod((Integer) i, hash);
	}

	public static void exitMethod(long l, long hash) {
		exitMethod((Long) l, hash);
	}

	public static void exitMethod(double d, long hash) {
		exitMethod((Double) d, hash);
	}

	public static void exception(Throwable e, long hash) {
		if (DISABLE) {
			tstack.remove();
			return;
		}

		final CallStack stack = tstack.get();
		if (!stack.self) {
			stack.self = true;

			if (stack.record()) {
				ExceptionMessage m = new ExceptionMessage();
				m.thread = Thread.currentThread().getId();
				m.exception = VMValue.make(e);
				client.send(m);
			}
			stack.pop(hash);

			stack.self = false;
		}
	}

	public static void accessField(Object thisObj, Object oldValue, long owner, String name, int line) {
		if (DISABLE)
			return;

		AccessFieldMessage m = new AccessFieldMessage();
		m.thread = Thread.currentThread().getId();
		m.thisObj = VMValue.make(thisObj);
		m.oldValue = VMValue.make(oldValue);
		m.owner = owner;
		m.name = name;
		m.line = line;
		client.send(m);
	}

	public static void assignField(Object thisObj, Object newValue, long owner, String name, int line) {
		if (DISABLE)
			return;

		AssignFieldMessage m = new AssignFieldMessage();
		m.thread = Thread.currentThread().getId();
		m.thisObj = VMValue.make(thisObj);
		m.newValue = VMValue.make(newValue);
		m.owner = owner;
		m.name = name;
		m.line = line;
		client.send(m);
	}

	public static void delayedAssignField(Object newValue, long mid, long owner, String name, int line) {
		if (DISABLE)
			return;

		AssignFieldMessage m = new AssignFieldMessage();
		m.thread = Thread.currentThread().getId();
		m.newValue = VMValue.make(newValue);
		m.owner = owner;
		m.name = name;
		m.line = line;

		tstack.get().delayed.push(m);
		tstack.get().delayedHashes.push(mid);
	}

	public static void accessArray(Object[] aref, int index, int line) {
		if (DISABLE)
			return;

		// record only successful reads
		if (aref != null && 0 <= index && index < aref.length) {
			final Object ref = aref[index];
			if (policy.recordArray(ref)) {
				AccessArrayMessage m = new AccessArrayMessage();
				m.thread = Thread.currentThread().getId();
				m.array = VMValue.make(aref);
				m.oldValue = VMValue.make(ref);
				m.index = index;
				m.length = aref.length;
				m.line = line;
				client.send(m);
			}
		}
	}

	public static void assignArray(Object[] aref, int index, Object ref, int line) {
		try {
			if (DISABLE)
				return;

			// record only successful writes
			if (aref != null && 0 <= index && index < aref.length && policy.recordArray(ref)) {
				AssignArrayMessage m = new AssignArrayMessage();
				m.thread = Thread.currentThread().getId();
				m.array = VMValue.make(aref);
				m.newValue = VMValue.make(ref);
				m.index = index;
				m.length = aref.length;
				m.line = line;
				client.send(m);
			}
		} finally {
			// perform actual assignment (not done in the original class
			// anymore)
			aref[index] = ref;
		}
	}

	/** Declared because of LIBRARY_WHITELIST */
	private static final long SYSTEM_ARRAYCOPY = -5829065086251241999L;

	public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
		final Object[] params = new Object[] { src, srcPos, dest, destPos, length };
		enterMethod(Configuration.LIBRARY, SYSTEM_ARRAYCOPY, null, params);
		try {
			System.arraycopy(src, srcPos, dest, destPos, length);
			exitMethod(SYSTEM_ARRAYCOPY);
		} catch (Throwable e) {
			exception(e, SYSTEM_ARRAYCOPY);
		}
	}
}
