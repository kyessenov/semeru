package test.cap;

/**
 * Exercises multiple stack event sequences:
 * <ol>
 * <li>calling an overridden method
 * <li>calling super-constructor
 * <li>throwing an exception (within method sequence)
 * <li>catching an exception
 * </ol>
 * 
 * @author kuat
 *
 */
public class ControlFlowTest {

	static class A {
		void m1() {
		}

		void m2() {
		}

		final void e1() {
			throw new RuntimeException();
		}

		final void e2() {
			e1();
		}

		final void e3() {
			try {
				e1();
			} finally {
				m1();
			}
		}

		@SuppressWarnings("finally")
		final void e4() {
			try {
				e1();
			} finally {
				throw new RuntimeException();
			}
		}

		final RuntimeException exception() {
			return new RuntimeException();
		}

		final Object e5() {
			throw exception();
		}

		@SuppressWarnings("finally")
		final Object e6() {
			try {
				// something
			} finally {
				return this;
			}
		}

	}

	static class B extends A {
		@Override
		void m1() {
		}

		@Override
		void m2() {
			super.m2();
		}

		B() {
			super();
		}
	}

	static class C extends B {
		C() {
			throw new RuntimeException();
		}
	}

	/**
	 * This test exercises a problem with duplicate exit events. This happens
	 * because an array needs to be constructed before making a constructor
	 * call.
	 * 
	 * @author kuat
	 * @date 2/25/2010
	 */
	static class D {
		D(Object[] x) {
		}

		D(Object elt) {
			this(new Object[] { elt });
		}

		static D make() {
			return new D(new Object());
		}
	}

	static class E {
		static int S = 0;
		int i = 1;

		E(int i, int j) {
		}

		E(E other) {
			this(E.S, other.i);
		}

		static E make() {
			return new E(new E(0, 0));
		}
	}

	/**
	 * Enter event is not triggered if a constructor argument throws.
	 */
	static class F0 {
		F0(int i) {
		}
	}

	static class F extends F0 {
		F() {
			super(getI());
		}

		static int getI() {
			throw new RuntimeException();
		}
	}

	/**
	 * Checking what gets executed this particular strange but permitted snippet
	 * of code.
	 * 
	 * @return
	 */
	@SuppressWarnings("finally")
	public static Object puzzle() {
		try {
			return null;
		} finally {
			return new Object();
		}
	}

	public static void main(String[] args) {
		A a = new A();
		a.m1();
		a.m2();
		try {
			a.e1();
		} catch (Exception e) {
		}

		try {
			a.e2();
		} catch (Exception e) {
		}
		try {
			a.e3();
		} catch (Exception e) {
		}
		try {
			a.e4();
		} catch (Exception e) {
		}
		try {
			a.e5();
		} catch (Exception e) {
		}
		a.e6();

		A b = new B();
		b.m1();
		b.m2();

		try {
			new C();
		} catch (Exception e) {
		}

		puzzle();

		D.make();
		E.make();
		try {
			new F();
		} catch (Exception e) {
		}
	}
}
