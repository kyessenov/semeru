package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DeclareFieldMessage implements Declaration {
	public String clazz;
	public String name;
	public String desc;
	public int access;
	public VMValue value;

	@Override
	public int handle() {
		return 9;
	}

	@Override
	public boolean repOk() {
		assert clazz != null;
		assert name != null;
		assert desc != null;
		assert value != null;
		return true;
	}

	@Override
	public String toString() {
		return "declare field: " + clazz + "." + name + " " + desc;
	}

	public static DeclareFieldMessage read(DataInputStream in) throws IOException {
		DeclareFieldMessage m = new DeclareFieldMessage();
		m.clazz = in.readUTF();
		m.name = in.readUTF();
		m.desc = in.readUTF();
		m.access = in.readInt();
		m.value = VMValue.read(in);
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(clazz);
		out.writeUTF(name);
		out.writeUTF(desc);
		out.writeInt(access);
		value.write(out);
		;
	}

}
