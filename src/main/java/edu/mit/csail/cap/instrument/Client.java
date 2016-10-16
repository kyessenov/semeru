package edu.mit.csail.cap.instrument;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import edu.mit.csail.cap.instrument.Management.ManagementClient;
import edu.mit.csail.cap.wire.CommandMessage;
import edu.mit.csail.cap.wire.Declaration;
import edu.mit.csail.cap.wire.Message;
import edu.mit.csail.cap.wire.Serializer;

/**
 * A light-weight client that uses buffered output streams. Declarations may
 * appear out of order of references by field descriptors. Thread-safe.
 * 
 * @author kuat
 * 
 */
public class Client implements ManagementClient {
	private DataOutputStream log;
	private DataOutputStream meta;

	Client(String host, int port) throws Exception {
		InetAddress addr = InetAddress.getByName(host);
		System.out.println("[agent] connecting to " + host + ":" + port);
		@SuppressWarnings("resource")
		Socket socket = new Socket(addr, port);
		this.log = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		this.meta = this.log;
	}

	Client(String log, String meta) throws FileNotFoundException, IOException {
		System.out.println("[agent] writing to " + log + ", " + meta);
		this.log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(log))));
		this.meta = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(meta))));
	}

	private void setLog(String log) {
		Runtime.out.println("[agent] writing to " + log);
		try {
			this.log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(log))));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void flush() {
		try {
			this.log.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.meta.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void send(Message msg) {
		try {
			if (msg instanceof Declaration)
				Serializer.write(meta, msg);
			else
				Serializer.write(log, msg);
		} catch (Throwable e) {
			e.printStackTrace();
			Runtime.out.println("[agent] failed to send a message " + msg);
			throw new Error(e);
		}
	}

	public synchronized void close() {
		Runtime.out.println("[agent] closing output streams");
		flush();
		try {
			log.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			meta.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void init() {
		Runtime.out.println("[agent] runtime disabled");
		Runtime.DISABLE = true;
	}

	public String prompt() {
		StringBuilder sb = new StringBuilder();
		sb.append("type 'log [log-name]' to set log database name\n");
		sb.append("type 'start' to start collection\n");
		sb.append("type 'stop' to stop collection\n");
		sb.append("type 'status' to check if collection is enabled\n");
		sb.append("type 'tag [tag-name]' to tag an event\n");
		sb.append("type 'out [file-name]' to change log file\n");
		sb.append("type 'exit' to quit\n");
		return sb.toString();
	}

	public synchronized String receive(String command) {
		if (command.startsWith("start")) {
			Runtime.DISABLE = false;
			return "runtime enabled";
		} else if (command.startsWith("stop")) {
			Runtime.DISABLE = true;
			return "runtime disabled";
		} else if (command.startsWith("log")) {
			final String log = command.substring(3).trim();
			if (log.length() > 0) {
				final CommandMessage cmd = new CommandMessage();
				cmd.command = CommandMessage.SET_LOG;
				cmd.param = log;
				send(cmd);
				return "switch log " + log;
			} else
				return "empty log";
		} else if (command.startsWith("tag")) {
			final String tag = command.substring(3).trim();
			final CommandMessage cmd = new CommandMessage();
			cmd.command = CommandMessage.TAG;
			cmd.param = tag;
			send(cmd);
			return "tag " + tag;
		} else if (command.equals("status")) {
			return Runtime.DISABLE ? "disabled" : "enabled";
		} else if (command.equals("flush")) {
			flush();
			return "flushed";
		} else if (command.startsWith("out")) {
			final String file = command.substring(4);
			setLog(file);
			return "out file set to " + file;
		} else {
			return "unrecognized command " + command;
		}
	}
}
