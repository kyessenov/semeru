package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EnterMethodMessage implements Message {
	public long thread;
	public long method;
	public VMValue thisObj;
	public VMValue[] params;

	@Override
	public int handle() {
		return 0;
	}

	public boolean repOk() {
		assert thisObj != null;
		assert params != null;
		return true;
	}

	@Override
	public String toString() {
		return "enter method: " + method;
	}

	public static EnterMethodMessage read(DataInputStream in) throws IOException {
		EnterMethodMessage m = new EnterMethodMessage();
		m.thread = in.readLong();
		m.method = in.readLong();
		m.thisObj = VMValue.read(in);
		m.params = new VMValue[in.readInt()];
		for (int i = 0; i < m.params.length; i++)
			m.params[i] = VMValue.read(in);
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(thread);
		out.writeLong(method);
		thisObj.write(out);
		out.writeInt(params.length);
		for (VMValue param : params)
			param.write(out);
	}
}
