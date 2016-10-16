package test.cap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HelloHeap {

	static boolean value;

	static class A {
		Object f;

		void update(A a) {
			a.f = a;
		}

		void call(int x) {
			return;
		}
	}

	static class SpecialException extends Exception {
		private static final long serialVersionUID = 1L;
		Map<String, String> x;

		SpecialException(String msg) {
			super(msg);
			System.out.println("something");
			x = new HashMap<String, String>();
			x.put("a", "b");
		}
	}

	/**
	 * Creates an instance and updates its only field to point to itself.
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws SpecialException, IOException {
		// System.out.print("Please enter:");
		// byte [] buf = new byte [200];
		// System.in.read(buf);

		A a = new A();
		a.call(3);
		a.update(a);

		A b = new A();
		b.update(a);

		try {
			throwing();
		} catch (RuntimeException e) {
			new A();
		}

		try {
			beforeThrowing();
		} catch (RuntimeException e) {

		}

		System.out.println("HelloHeap!");
	}

	static void throwing() {
		throw new RuntimeException();
	}

	static void beforeThrowing() {
		throwing();
	}

}
