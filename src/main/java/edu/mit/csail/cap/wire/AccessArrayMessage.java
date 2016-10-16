package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AccessArrayMessage implements Message {
	public long thread;
	public VMValue array;
	public VMValue oldValue;
	public int index;
	public int length;
	public int line;

	@Override
	public int handle() {
		return 5;
	}

	public boolean repOk() {
		assert array != null;
		assert oldValue != null;
		assert index >= 0;
		assert length > index;
		return true;
	}

	@Override
	public String toString() {
		return "access array at " + line + ": " + array + "[" + index + "] = " + oldValue;
	}

	public static AccessArrayMessage read(DataInputStream in) throws IOException {
		AccessArrayMessage m = new AccessArrayMessage();
		m.thread = in.readLong();
		m.array = VMValue.read(in);
		m.oldValue = VMValue.read(in);
		m.index = in.readInt();
		m.length = in.readInt();
		m.line = in.readInt();
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(thread);
		array.write(out);
		oldValue.write(out);
		out.writeInt(index);
		out.writeInt(length);
		out.writeInt(line);
	}
}
