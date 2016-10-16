package edu.mit.csail.cap.wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.mit.csail.cap.instrument.Runtime;

/**
 * A public static final long reference from the target VM.
 * 
 * @author kuat
 * 
 */
public class VMValue implements Streamable {
	public static final int NULL = 0;
	public static final int UNKNOWN = -1;

	public static final long PRIMITIVE_TYPE = 0L;

	public int value;
	public long typeHash;
	public int dim;

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public boolean repOk() {
		return true;
	}

	public static VMValue primitive(int id) {
		final VMValue out = new VMValue();
		out.value = id;
		out.dim = 0;
		out.typeHash = PRIMITIVE_TYPE;
		return out;
	}

	public static VMValue instance(Object o) {
		final VMValue out = new VMValue();
		out.value = System.identityHashCode(o);
		out.dim = 0;

		Class<?> c = o.getClass();
		while (c.isArray()) {
			out.dim++;
			c = c.getComponentType();
		}

		final Long hash = Runtime.typeID(c);
		out.typeHash = (hash == null) ? Runtime.declareType(c) : hash;

		return out;
	}

	public static VMValue read(DataInputStream in) throws IOException {
		final VMValue v = new VMValue();
		v.value = in.readInt();
		v.typeHash = in.readLong();
		v.dim = in.readInt();
		return v;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(value);
		out.writeLong(typeHash);
		out.writeInt(dim);
	}

	public static VMValue make(Object o) {
		if (o == null) {
			return VMValue.primitive(NULL);
		} else if (o instanceof Byte) {
			return VMValue.primitive(((Byte) o).intValue());
		} else if (o instanceof Double) {
			return VMValue.primitive(((Double) o).intValue());
		} else if (o instanceof Float) {
			return VMValue.primitive(((Float) o).intValue());
		} else if (o instanceof Integer) {
			return VMValue.primitive(((Integer) o).intValue());
		} else if (o instanceof Long) {
			return VMValue.primitive(((Long) o).intValue());
		} else if (o instanceof Short) {
			return VMValue.primitive(((Short) o).intValue());
		} else if (o instanceof Boolean) {
			return VMValue.primitive(((Boolean) o).booleanValue() ? 1 : 0);
		} else if (o instanceof Character) {
			return VMValue.primitive(((Character) o).charValue());
		} else {
			final VMValue out = VMValue.instance(o);

			if (Runtime.policy.recordStrings && (o instanceof String)) {
				final String s = (String) o;
				if (s.length() <= Runtime.policy.recordMaxStringSize)
					Runtime.string(out.value, s);
			}

			return out;
		}
	}

	public static VMValue[] makeAll(Object[] in) {
		VMValue[] out = new VMValue[in.length];
		for (int i = 0; i < in.length; i++)
			out[i] = make(in[i]);
		return out;
	}
}
