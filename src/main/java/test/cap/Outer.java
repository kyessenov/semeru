package test.cap;

public class Outer {
	final String m = "greeting from \t\n outer class";
	final A a = new A();

	class Inner {
		A a;

		void m() {
			System.out.println(Outer.this.m);
		}

		Inner(A a) {
			this.a = a;
		}

		Inner() {
			this(Outer.this.a);
		}
	}

	class A {

	}

	public static void main(String[] args) {
		Outer x = new Outer();
		Inner y = x.new Inner();
		y.m();
	}
}
