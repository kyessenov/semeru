package edu.mit.csail.cap.util;

// Linked list implementation of a stack
public class Stack<T> {
	private class Node {
		T data;
		Node next;
	}

	private Node head = null;

	/** Push an element to the top of the stack. */
	public void push(T a) {
		final Node n = new Node();
		n.data = a;
		n.next = this.head;
		this.head = n;
	}

	/** Returns last pushed element or null if empty. */
	public T pop() {
		final Node n = this.head;
		if (n == null)
			return null;

		this.head = n.next;
		return n.data;
	}

	public boolean isEmpty() {
		return head == null;
	}

	/**
	 * Return i-th element from the top of the stack. Last pushed element is
	 * 0th.
	 */
	public T get(int i) {
		assert i >= 0 : "negative index";
		Node cur = this.head;
		while (i > 0) {
			cur = cur == null ? null : cur.next;
			i = i - 1;
		}
		return cur == null ? null : cur.data;
	}

	public T peek() {
		if (head == null)
			return null;
		else
			return head.data;
	}

	@Override
	public String toString() {
		Node cur = this.head;
		String out = "[stack:\n";
		while (cur != null) {
			out = out + cur.data + "\n";
			cur = cur.next;
		}
		out = out + "]";
		return out;
	}
}
