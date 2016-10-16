package test.cap;

abstract class A {
	abstract String getA();

	String i;
	String k;

	void print() {
		System.out.println(i);
		hidden();
	}

	private void hidden() {
	}
}

interface B {
	String getB();

	String j = "3";
}

public class AbstractTest extends A implements B {
	// Hiding field
	A i;

	public static void main(String[] args) {
		AbstractTest t = new AbstractTest();
		t.k = "A";
		t.setI();
		System.out.println(t.getA() + " " + t.getB());
		t.print();
		t.hidden();
	}

	@Override
	public String getB() {
		return "BB";
	}

	@Override
	String getA() {
		return "a";
	}

	void hidden() {
	}

	public void setI() {
		this.i = this;
		super.i = "A";
	}
}
