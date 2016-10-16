package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Serializer {
	/** Assign codes based on ordering in the handlers list */
	public static GenericHandler[] handlers = new GenericHandler[] { new GenericHandler("enter") { // 0
		@Override
		public Message read(DataInputStream in) throws IOException {
			return EnterMethodMessage.read(in);
		}
	}, new GenericHandler("exit") { // 1
		@Override
		public Message read(DataInputStream in) throws IOException {
			return ExitMethodMessage.read(in);
		}
	}, new GenericHandler("exception") { // 2
		@Override
		public Message read(DataInputStream in) throws IOException {
			return ExceptionMessage.read(in);
		}
	}, new GenericHandler("read") { // 3
		@Override
		public Message read(DataInputStream in) throws IOException {
			return AccessFieldMessage.read(in);
		}
	}, new GenericHandler("write") { // 4
		@Override
		public Message read(DataInputStream in) throws IOException {
			return AssignFieldMessage.read(in);
		}
	}, new GenericHandler("arrayread") { // 5
		@Override
		public Message read(DataInputStream in) throws IOException {
			return AccessArrayMessage.read(in);
		}
	}, new GenericHandler("arraywrite") { // 6
		@Override
		public Message read(DataInputStream in) throws IOException {
			return AssignArrayMessage.read(in);
		}
	}, new GenericHandler("declare class") { // 7
		@Override
		public Message read(DataInputStream in) throws IOException {
			return DeclareClassMessage.read(in);
		}
	}, new GenericHandler("declare method") { // 8
		@Override
		public Message read(DataInputStream in) throws IOException {
			return DeclareMethodMessage.read(in);
		}
	}, new GenericHandler("declare field") { // 9
		@Override
		public Message read(DataInputStream in) throws IOException {
			return DeclareFieldMessage.read(in);
		}
	}, new GenericHandler("string") { // 10
		@Override
		public Message read(DataInputStream in) throws IOException {
			return StringMessage.read(in);
		}
	}, new GenericHandler("command") { // 11
		@Override
		public Message read(DataInputStream in) throws IOException {
			return CommandMessage.read(in);
		}
	} };

	/** Write a message to the stream */
	public static void write(DataOutputStream out, Message msg) throws IOException {
		int i = msg.handle();
		out.writeInt(i);
		msg.write(out);
	}

	/** Read an object from the stream */
	public static Message read(DataInputStream in) throws IOException {
		int i = in.readInt();
		assert 0 <= i && i < handlers.length : "incorrect handle " + i;
		final Message msg = handlers[i].read(in);
		assert msg.handle() == i : "incorrect handle";
		assert msg.repOk() : "corrupt message " + msg;
		return msg;
	}

	public abstract static class GenericHandler {
		public final String name;

		GenericHandler(String name) {
			this.name = name;
		}

		abstract Message read(DataInputStream in) throws IOException;
	}
}
