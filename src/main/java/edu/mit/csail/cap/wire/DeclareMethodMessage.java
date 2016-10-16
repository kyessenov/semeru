package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DeclareMethodMessage implements Declaration {
	public String clazz;
	public String method;
	public String desc;
	public int access;

	@Override
	public int handle() {
		return 8;
	}

	@Override
	public boolean repOk() {
		assert clazz != null;
		assert method != null;
		assert desc != null;
		return true;
	}

	@Override
	public String toString() {
		return "declare method: " + clazz + "." + method + " " + desc;
	}

	public static DeclareMethodMessage read(DataInputStream in) throws IOException {
		DeclareMethodMessage m = new DeclareMethodMessage();
		m.clazz = in.readUTF();
		m.method = in.readUTF();
		m.desc = in.readUTF();
		m.access = in.readInt();
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(clazz);
		out.writeUTF(method);
		out.writeUTF(desc);
		out.writeInt(access);
	}
}
