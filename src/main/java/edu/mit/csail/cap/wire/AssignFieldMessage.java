package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AssignFieldMessage implements Message {
	public long thread;
	public VMValue thisObj;
	public VMValue newValue;
	public long owner;
	public String name;
	public int line;

	@Override
	public int handle() {
		return 4;
	}

	public boolean repOk() {
		assert thisObj != null;
		assert newValue != null;
		assert name != null;
		assert owner != 0L;
		return true;
	}

	@Override
	public String toString() {
		return "assign field at " + line + ": " + thisObj + "." + name + " = " + newValue;
	}

	public static AssignFieldMessage read(DataInputStream in) throws IOException {
		AssignFieldMessage m = new AssignFieldMessage();
		m.thread = in.readLong();
		m.thisObj = VMValue.read(in);
		m.newValue = VMValue.read(in);
		m.owner = in.readLong();
		m.name = in.readUTF();
		m.line = in.readInt();
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(thread);
		thisObj.write(out);
		newValue.write(out);
		out.writeLong(owner);
		out.writeUTF(name);
		out.writeInt(line);
	}
}
