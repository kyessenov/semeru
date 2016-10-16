package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ExitMethodMessage implements Message {
	public long thread;
	public VMValue returnValue;

	@Override
	public int handle() {
		return 1;
	}

	public boolean repOk() {
		assert returnValue != null;
		return true;
	}

	@Override
	public String toString() {
		return "exit method: " + returnValue;
	}

	public static ExitMethodMessage read(DataInputStream in) throws IOException {
		ExitMethodMessage m = new ExitMethodMessage();
		m.thread = in.readLong();
		m.returnValue = VMValue.read(in);
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(thread);
		returnValue.write(out);
	}

}
