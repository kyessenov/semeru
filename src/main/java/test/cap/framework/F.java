package test.cap.framework;

public class F {
	// setter/getter for the client
	A a;

	public void set(A a) {
		this.a = a;
	}

	public A get() {
		return a;
	}

	// indirect invocation through framework
	public void x() {
		A a = this.get();
		a.x();
	}

	// indirect invocation through framework with a return value
	public void y() {
		A a = this.get();
		B b = a.y();
		b.x();
	}

	// key method call
	public static Object magic(Object x) {
		return x;
	}
}
