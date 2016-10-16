package test.cap;

import edu.mit.csail.cap.util.Stack;

public class TestStack {
	public static void main(String[] args) {
		final Stack<String> s = new Stack<String>();
		Assert(s.get(0) == null);
		Assert(s.pop() == null);
		s.push("a");
		s.push("b");
		s.push("c");
		Assert(s.peek().equals("c"));
		Assert(s.pop().equals("c"));
		Assert(s.peek().equals("b"));
		Assert(s.get(0).equals("b"));
		Assert(s.get(1).equals("a"));
		Assert(s.get(2) == null);
		Assert(s.get(3) == null);
		s.push("c");
		Assert(s.get(0).equals("c"));
		Assert(s.get(1).equals("b"));
		Assert(s.get(2).equals("a"));
		Assert(s.get(3) == null);
		Assert(s.get(4) == null);

		System.out.println("stack tests passed");
	}

	private static void Assert(boolean test) {
		if (!test)
			throw new RuntimeException("assertion failure");
	}
}
