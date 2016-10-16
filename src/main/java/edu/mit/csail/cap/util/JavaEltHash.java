package edu.mit.csail.cap.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Strategies for hashing Java element descriptors.
 * 
 * Thread-safe.
 * 
 * @author kuat
 * 
 */
public class JavaEltHash {
	private static final MessageDigest hash = getMD5();

	private static MessageDigest getMD5() {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			return md5;
		} catch (NoSuchAlgorithmException e) {
			throw new Error("cannot load MD5 hash");
		}
	}

	/**
	 * Convert an array of 16 bytes to a long.
	 */
	private static long toLong(byte[] b) {
		assert b.length == 16;

		long l1 = 0;
		for (int i = 0; i < 8; i++) {
			l1 <<= 8;
			l1 ^= (long) b[i] & 0xFF;
		}

		long l2 = 0;
		for (int i = 8; i < 16; i++) {
			l2 <<= 8;
			l2 ^= (long) b[i] & 0xFF;
		}

		return l1 ^ l2;
	}

	/**
	 * Hash a string to a long.
	 */
	private static long hash(String id) {
		hash.reset();
		hash.update(id.getBytes());
		return toLong(hash.digest());
	}

	public static synchronized long hashClass(String name) {
		long out = hash(name);
		return out;
	}

	public static synchronized long hashMethod(String declaringType, String name, String sig) {
		return hash(declaringType + "." + name + "." + sig);
	}

	public static synchronized long hashField(String declaringType, String name) {
		return hash(declaringType + "." + name);
	}
}
