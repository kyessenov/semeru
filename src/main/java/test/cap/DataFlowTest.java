package test.cap;

public class DataFlowTest {

	public static class A {
		int a;
		Object b;

		public A(int x) {
			this.a = x;
			this.b = this;
		}
	}

	public static class B extends A {
		Object c;

		public B() {
			super(0);
			this.b = this;
			this.c = this;
		}
	}

	public static void main(String[] args) {
		A a = new A(5);
		for (int i = 0; i < 5; i++)
			a.a = ((A) a.b).a + 1;

		new B();
	}
}
