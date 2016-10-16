package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AccessFieldMessage implements Message {
	public long thread;
	public VMValue thisObj;
	public VMValue oldValue;
	public long owner;
	public String name;
	public int line;

	@Override
	public int handle() {
		return 3;
	}

	public boolean repOk() {
		assert thisObj != null;
		assert oldValue != null;
		assert name != null;
		assert owner != 0L;
		return true;
	}

	@Override
	public String toString() {
		return "access field at " + line + ": " + thisObj + "." + name + " = " + oldValue;
	}

	public static AccessFieldMessage read(DataInputStream in) throws IOException {
		AccessFieldMessage m = new AccessFieldMessage();
		m.thread = in.readLong();
		m.thisObj = VMValue.read(in);
		m.oldValue = VMValue.read(in);
		m.owner = in.readLong();
		m.name = in.readUTF();
		m.line = in.readInt();
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(thread);
		thisObj.write(out);
		oldValue.write(out);
		out.writeLong(owner);
		out.writeUTF(name);
		out.writeInt(line);
	}
}
