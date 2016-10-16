package edu.mit.csail.cap.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;

import com.sun.tools.attach.VirtualMachine;

/**
 * This is the main class for the instrumentation agent (to be used when
 * invoking JVM with agent parameters.)
 * <p>
 * <p>
 * See {@link http
 * ://java.sun.com/j2se/1.5.0/docs/api/java/lang/instrument/package
 * -summary.html}
 * <p>
 * 
 * @author kuat
 * 
 */
public final class Agent {
	/** Invoked when VM is launched with the agent */
	public static void premain(String arg, Instrumentation inst) {
		try {
			Configuration prop = new Configuration(arg);
			setup(prop, inst);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/** Invoked when the agent is attached to the target VM */
	public static void agentmain(String args, Instrumentation inst) {
		try {
			System.out.println("agent started");
			Configuration prop = new Configuration(args);
			dumpHeap(prop);
			setup(prop, inst);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Invoked as a stand-alone tool. The agent attaches to a working VM.
	 **/
	public static void main(String[] args) throws Exception {
		if (args.length != 3)
			throw new RuntimeException("Usage: <main> properties-file pid agent-jar");

		System.out.println("agent initiating attachment");
		VirtualMachine vm = VirtualMachine.attach(args[1]);
		System.out.println("loading agent");
		vm.loadAgent(args[2], args[0]);
		System.out.println("detaching");
		vm.detach();
	}

	/**
	 * Dump the heap.
	 **/
	private static void dumpHeap(Configuration prop) throws Exception {
		if (!prop.listenFields)
			return;
		else
			System.out.println("heap dump not implemented");
	}

	private static void checkAssertions() {
		// test for assertions
		boolean enabled = false;
		assert enabled = true;
		if (!enabled)
			System.out.println("[agent] assertions are disabled for agent");
	}

	private static void setup(Configuration prop, final Instrumentation inst) throws Exception {
		checkAssertions();
		assert inst.isRetransformClassesSupported() : "VM must support retransformation of classes";

		final Client client;
		if (prop.useFiles())
			client = new Client(Configuration.LOG, Configuration.METADATA);
		else
			client = new Client(prop.agentServer, prop.agentPort);

		Runtime.initialize(prop, client, System.out);

		// start management thread
		if (prop.management) {
			System.out.println("[agent] starting management server");
			client.init();
			new Management(prop.managementPort, client).start();
		}

		// add shutdown hook to finish client safely
		java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				client.close();
			}
		});

		// add transformer
		final ClassFileTransformer transformer = new Transformer();
		inst.addTransformer(transformer, true);

		// transform existing classes
		final Class<?>[] classes = inst.getAllLoadedClasses();
		final Class<?>[] todo = new Class[classes.length];

		int num = 0;
		for (Class<?> c : classes) {
			if (c.isArray())
				continue;

			if (!inst.isModifiableClass(c))
				throw new RuntimeException("cannot modify " + c);

			todo[num++] = c;
		}

		if (num > 0) {
			inst.retransformClasses(Arrays.copyOf(todo, num));
			System.out.println("[agent] finished transformation of " + num + " classes");
		}
	}
}
