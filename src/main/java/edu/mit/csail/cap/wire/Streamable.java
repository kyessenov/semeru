package edu.mit.csail.cap.wire;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Message interface for communicating from the client to the server. The
 * pattern requires each implementing class to provide a static de-serializer
 * method.
 * 
 * @author kuat
 *
 */
public interface Streamable {
	public boolean repOk();

	public void write(DataOutputStream out) throws IOException;
}
