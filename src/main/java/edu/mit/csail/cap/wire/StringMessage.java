package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** String declaration message. */
public class StringMessage implements Message {
	public int id;
	public String value;

	@Override
	public int handle() {
		return 10;
	}

	public boolean repOk() {
		return value != null;
	}

	@Override
	public String toString() {
		return "string " + value;
	}

	public static StringMessage read(DataInputStream in) throws IOException {
		StringMessage m = new StringMessage();
		m.id = in.readInt();
		m.value = in.readUTF();
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(id);
		out.writeUTF(value);
	}
}
