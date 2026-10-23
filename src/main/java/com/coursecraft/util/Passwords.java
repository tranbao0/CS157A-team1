package com.coursecraft.util;

import org.mindrot.jbcrypt.BCrypt;

// all bcrypt calls go through here so cost is set in one place
public final class Passwords {

	/*
	 * Work factor, 10 means 2^10 rounds of hashing.
	 * seed.sql hashes use 10 too. If this changes, regenerate seed hashes,
	 * otherwise LoginServlet's dummy hash and real hashes take different time.
	 */
	public static final int BCRYPT_COST = 10;

	// sign-up rules, bcrypt ignores everything past 72 bytes so longer is refused
	public static final int MIN_LENGTH = 8;
	public static final int MAX_BYTES = 72;

	private Passwords() {
	}

	// new random salt every call, so same password gives different hash
	public static String hash(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));
	}

	// hashes typed password with salt stored in hash and compares
	public static boolean matches(String password, String hash) {
		return BCrypt.checkpw(password, hash);
	}
}
