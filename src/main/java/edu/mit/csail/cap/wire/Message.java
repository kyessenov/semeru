package edu.mit.csail.cap.wire;

public interface Message extends Streamable {
	public int handle();
}
