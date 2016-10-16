package edu.mit.csail.cap.instrument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple management interface to control the agent runtime.
 * 
 * @author kuat
 *
 */
class Management extends Thread {
	private final ServerSocket socket;
	private final ManagementClient client;

	Management(int port, ManagementClient client) throws IOException {
		this.socket = new ServerSocket(port);
		this.client = client;
	}

	/** Await for a connection from the client. */
	public void run() {
		Runtime.tstack.get().self = true;

		try {
			while (true) {
				Runtime.out.println("[agent] listening for management connection");
				Socket cl = socket.accept();
				Runtime.out.println("[agent] accepted a connection from: " + cl);
				try {
					final PrintWriter out = new PrintWriter(cl.getOutputStream(), true);
					final BufferedReader in = new BufferedReader(new InputStreamReader(cl.getInputStream()));
					out.println(client.prompt());

					String input;
					while ((input = in.readLine()) != null) {
						if (input.equals("exit"))
							break;
						String reply = client.receive(input);
						Runtime.out.println("[agent] " + reply);
						out.println(reply);
					}
				} catch (IOException e) {
					Runtime.out.println("[agent] management client I/O error");
				} finally {
					cl.close();
				}
			}
		} catch (IOException e) {
			Runtime.out.println("[agent] management server socket I/O error");
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	static interface ManagementClient {
		void init();

		String prompt();

		String receive(String command);
	}
}
