package test.cap;

import java.util.ArrayList;
import java.util.List;

public class HeapChainTest {
	/** Basic heap structures */
	public static class CellList {
		public List<Cell> children;
	}

	public static class Cell {
		public Cell parent;
	}

	/** Signifies separator in the execution */
	public static void marker() {
	}

	public static void main(String[] args) {
		/* 0 */marker();
		Cell a = new Cell();
		Cell b = new Cell();
		Cell c = new Cell();
		Cell d = new Cell();
		b.parent = a;
		c.parent = b;
		d.parent = b;

		List<Cell> l = new ArrayList<Cell>();
		l.add(a);
		l.add(0, b);
		l.add(c);
		l.add(d);

		// should contain a, b, c, d

		/* 1 */marker();

		l.remove(a);
		l.remove(0);
		l.set(1, c);

		// should be c and c inside

		/* 2 */marker();

		l.clear();

		// should be empty

		/* 3 */marker();

		CellList m = new CellList();
		m.children = l;
		l.add(a);

		// should be a chain now: m -> l -> a

		/* 4 */marker();

		CellList[] n = new CellList[3];
		n[0] = m;
		for (int i = 0; i < 3; i++)
			n[i] = n[0];

		// should be a chain now: n -> m -> l -> a

		/* 5 */marker();
	}
}
