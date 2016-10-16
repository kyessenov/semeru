package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CommandMessage implements Message {
	public int command;
	public String param;

	public static int SET_LOG = 1;
	public static int TAG = 2;

	@Override
	public int handle() {
		return 11;
	}

	public boolean repOk() {
		assert param != null;
		return true;
	}

	@Override
	public String toString() {
		return "command " + command + "(" + param + ")";
	}

	public static CommandMessage read(DataInputStream in) throws IOException {
		CommandMessage m = new CommandMessage();
		m.command = in.readInt();
		m.param = in.readUTF();
		return m;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(command);
		out.writeUTF(param);
	}
}
