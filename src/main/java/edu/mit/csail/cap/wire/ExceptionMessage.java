package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ExceptionMessage implements Message {
	public long thread;
	public VMValue exception;

	@Override
	public int handle() {
		return 2;
	}

	public boolean repOk() {
		assert exception != null;
		return true;
	}

	@Override
	public String toString() {
		return "exception: " + exception;
	}

	public static ExceptionMessage read(DataInputStream in) throws IOException {
		ExceptionMessage m = new ExceptionMessage();
		m.thread = in.readLong();
		m.exception = VMValue.read(in);
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(thread);
		exception.write(out);
	}
}
