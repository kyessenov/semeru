package test.cap.client;

import test.cap.framework.*;

public class B1 implements B {
	// irrelevant field
	Object junk;

	public B1() {
		// irrelevant code
		this.junk = this;
	}

	@Override
	public void x() {
		F.magic(null);
	}

	@Override
	public C junk() {
		// irrelevant code
		return new C1();
	}
}
