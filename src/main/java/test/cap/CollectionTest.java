package test.cap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionTest {
	private static class CTmp {
		int data;
		CTmp next = null;

		public String toString() {
			if (next == null) {
				return Integer.toString(data);
			} else {
				return Integer.toString(data) + "->" + next.toString();
			}
		}

		public CTmp(int d) {
			data = d;
		}
	}

	private static class Arr {
		CTmp[] arr = new CTmp[3];
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Lists:");
		List<String> l = new ArrayList<String>();
		l.add("Hello");
		l.add("World");
		l.set(0, "Hi");
		System.out.println(l.toString());
		l.remove(0);
		System.out.println(l.toString());
		l.clear();
		System.out.println(l.toString());

		System.out.println("Linked list:");
		CTmp a = new CTmp(0);
		a.data = 1;
		CTmp b = new CTmp(2);
		a.next = b;
		System.out.println(a.toString());

		System.out.println("Arrays:");
		Arr ar = new Arr();
		ar.arr[0] = new CTmp(0);
		ar.arr[1] = new CTmp(1);
		ar.arr[2] = new CTmp(2);
		ar.arr[0].next = ar.arr[1];
		ar.arr[1].next = ar.arr[2];
		System.out.println(ar.arr[0].toString());

		System.out.println("Hash map:");
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "one");
		map.put("2", "two");
		System.out.println(map.get("1"));

		System.out.println("Finish!");
	}

	@SuppressWarnings("unused")
	public static void testArrayList() {
		// exercise all functionality of array list
		List<Object> x = new ArrayList<Object>();
		ArrayList<Object> y = new ArrayList<Object>(x);
		List<Object> z = new ArrayList<Object>(5);

		Object elt = new Object();

		x.add(elt);
		y.add(elt);
		y.addAll(x);
		y.addAll(0, x);
		z.clear();

		Object t = y.clone();
		y.ensureCapacity(10);

		Object u = y.get(0);
		int i = y.indexOf(u);
		boolean b = y.isEmpty();
		int j = y.lastIndexOf(u);

		y.remove(0);
		y.remove(u);
		y.set(0, elt);
		int size = y.size();

		Object[] a1 = y.toArray();
		Object[] a2 = y.toArray(new Object[0]);
		y.trimToSize();
	}

	@SuppressWarnings("unused")
	public static void testHashMap() {
		// exercise all functionality of hash map
		HashMap<Object, Object> x = new HashMap<Object, Object>();
		Map<Object, Object> y = new HashMap<Object, Object>(10);
		Map<Object, Object> z = new HashMap<Object, Object>(10, .5f);
		Map<Object, Object> t = new HashMap<Object, Object>(x);
		x.clear();
		Object u = x.clone();

		boolean a = x.containsKey(y), b = x.containsValue(y);

		Set<?> entries = x.entrySet();
		Set<?> keys = x.keySet();

		x.put(y, y);
		Object v = x.get(y);
		boolean empty = x.isEmpty();
		x.putAll(y);
		x.remove(y);

		int size = x.size();
		Collection<?> values = x.values();
	}
}
