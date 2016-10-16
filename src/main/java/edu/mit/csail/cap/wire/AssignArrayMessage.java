package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AssignArrayMessage implements Message {
	public long thread;
	public VMValue array;
	public VMValue newValue;
	public int index;
	public int length;
	public int line;

	@Override
	public int handle() {
		return 6;
	}

	public boolean repOk() {
		assert array != null;
		assert newValue != null;
		assert index >= 0;
		assert length > index;
		return true;
	}

	@Override
	public String toString() {
		return "assign array at " + line + ": " + array + "[" + index + "] = " + newValue;
	}

	public static AssignArrayMessage read(DataInputStream in) throws IOException {
		AssignArrayMessage m = new AssignArrayMessage();
		m.thread = in.readLong();
		m.array = VMValue.read(in);
		m.newValue = VMValue.read(in);
		m.index = in.readInt();
		m.length = in.readInt();
		m.line = in.readInt();
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(thread);
		array.write(out);
		newValue.write(out);
		out.writeInt(index);
		out.writeInt(length);
		out.writeInt(line);
	}
}
