package test.cap.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import test.cap.framework.B;
import test.cap.framework.F;
import test.cap.framework.A;

public class Subtypes {
	static class Super extends Sub {
		void n(Object v) {
			this.m(v);
		}
	}

	static class Sub {
		void m(Object v) {
			F.magic(v);
		}
	}

	public static void test1() {
		// indirect call but x is not needed in the output
		Super x = new Super();

		// make path to value "v" complicated
		for (Object v : make())
			x.n(v);
	}

	public static Set<Sub> make() {
		Sub[] a = new Sub[2];
		a[0] = new Sub();
		Map<String, Sub[]> map = new HashMap<String, Sub[]>();
		map.put("key", a);
		Set<Sub> out = new HashSet<Sub>();
		out.add(map.get("key")[0]);
		return out;
	}

	static class A1 implements A {
		public Object o;

		@Override
		public void x() {
		}

		void set(F f) {
			o = new A1();
			f.set(this);
		}

		@Override
		public B y() {
			return null;
		}

		@Override
		public Object junk() {
			return null;
		}
	}

	static class A2 extends A1 {
		@Override
		public void x() {
			F.magic(o);
		}
	}

	public static void test2() {
		A1 a = new A2();
		F f = new F();
		a.set(f);
		f.x();
	}
}
