package test.cap;

public class FailedArray {
	public static class A {
	};

	public static void main(String[] args) {
		A[] as = new A[1];
		try {
			System.out.println(get(as));
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	private static A get(A[] as) {
		return as[2];
	}
}
