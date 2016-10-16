package test.cap.client;

import test.cap.framework.*;

public class A1 implements A {
	// cross call initialization
	java.util.List<B> b;

	public A1(F f) {
		f.set(this);
	}

	@Override
	public void x() {
		this.b = new java.util.ArrayList<B>();
		this.b.add(new B1());
		this.b.add(new B1());
	}

	@Override
	public B y() {
		// return this.b
		B b = this.y1();
		return b;
	}

	private B y1() {
		return this.b.get(0);
	}

	@Override
	public C junk() {
		// irrelevant code
		return new C1();
	}
}
