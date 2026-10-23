package com.coursecraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// no database needed, only bcrypt
class PasswordsTest {

	@Test
	void hashMatchesSamePassword() {
		String hash = Passwords.hash("correct horse");
		assertTrue(Passwords.matches("correct horse", hash));
	}

	@Test
	void hashDoesNotMatchOtherPassword() {
		String hash = Passwords.hash("correct horse");
		assertFalse(Passwords.matches("Correct horse", hash));
	}

	// new salt each time, so stored hashes never show two students share password
	@Test
	void samePasswordGivesDifferentHashes() {
		assertNotEquals(Passwords.hash("password123"), Passwords.hash("password123"));
	}

	// hash looks like $2a$10$..., 10 there is cost, always written with 2 digits
	@Test
	void hashUsesConfiguredCost() {
		String hash = Passwords.hash("password123");
		String expectedStart = String.format("$2a$%02d$", Passwords.BCRYPT_COST);

		assertTrue(hash.startsWith(expectedStart), "unexpected cost in " + hash);
		assertEquals(60, hash.length());
	}
}
