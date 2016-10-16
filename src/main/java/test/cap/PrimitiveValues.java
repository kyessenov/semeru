package test.cap;

public class PrimitiveValues {

	public static void main(String[] args) {
		t(0);
		t(1L);
		t(1.0);
		t(true);
		t('1');
		t("test");
	}

	static void test(Object x) {
		System.out.print(x);
	}

	static int t(int i) {
		System.out.print("int");
		test(i);
		return i;
	}

	static long t(long i) {
		System.out.print("long");
		test(i);
		return i;
	}

	static double t(double i) {
		System.out.print("double");
		test(i);
		return i;
	}

	static boolean t(boolean i) {
		System.out.print("bool");
		test(i);
		return i;
	}

	static char t(char i) {
		System.out.print("char");
		test(i);
		return i;
	}

	static String t(String s) {
		return s;
	}

}
