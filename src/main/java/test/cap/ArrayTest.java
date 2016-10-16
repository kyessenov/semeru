package test.cap;

public class ArrayTest {
	private static class CTmp {
		int data;

		public CTmp(int d) {
			data = d;
		}

		@SuppressWarnings("unused")
		public CTmp() {
		}
	}

	private static class Arr {
		private int n;

		public Arr(int n) {
			arr = new CTmp[n];
			this.n = n;
		}

		CTmp[] arr;

		void print() {
			for (int i = 0; i < n; ++i) {
				System.out.println("arr[" + i + "]=" + arr[i].data);
			}
		}
	}

	public static void main(String[] args) {

		Arr ar = new Arr(3);
		for (int i = 0; i < 3; ++i)
			ar.arr[i] = new CTmp(4 - i);

		CTmp[] copy = new CTmp[3];
		System.arraycopy(ar.arr, 0, copy, 0, 3);

		ar.print();
		System.out.println("Finish!");
	}
}
