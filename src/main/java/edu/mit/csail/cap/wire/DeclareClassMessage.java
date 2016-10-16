package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DeclareClassMessage implements Declaration {
	public String name;
	public int access;
	public String[] supers;

	@Override
	public int handle() {
		return 7;
	}

	public boolean repOk() {
		assert name != null;
		for (String sup : supers)
			assert sup != null;
		return true;
	}

	@Override
	public String toString() {
		return "declare class: " + name;
	}

	public static DeclareClassMessage read(DataInputStream in) throws IOException {
		DeclareClassMessage m = new DeclareClassMessage();
		m.name = in.readUTF();
		m.access = in.readInt();
		m.supers = new String[in.readInt()];
		for (int i = 0; i < m.supers.length; i++)
			m.supers[i] = in.readUTF();
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(name);
		out.writeInt(access);
		out.writeInt(supers.length);
		for (String sup : supers)
			out.writeUTF(sup);
	}
}
