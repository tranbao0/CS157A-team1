package com.coursecraft.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

/*
 * Reads sql/seed.sql as plain text, no database needed.
 * Run from repo root (Eclipse does this by default).
 * Every sample student should sign in with password123.
 */
class SeedDataTest {

	private List<String> hashes;

	// pulls every bcrypt hash ('$2a$...') out of seed file before each test
	@BeforeEach
	void readHashesFromSeedFile() throws IOException {
		String seed = Files.readString(Path.of("sql", "seed.sql"));

		hashes = new ArrayList<>();
		Matcher matcher = Pattern.compile("'(\\$2a\\$[^']*)'").matcher(seed);
		while (matcher.find()) {
			hashes.add(matcher.group(1));
		}
	}

	@Test
	void seedFileHasPasswordHashes() {
		assertFalse(hashes.isEmpty(), "no bcrypt hashes found in seed.sql");
	}

	// bcrypt hash is always 60 chars, extra char (like hidden \r) breaks login
	@Test
	void everyHashIsSixtyCharacters() {
		for (String hash : hashes) {
			assertEquals(60, hash.length(), "wrong length: " + hash);
		}
	}

	@Test
	void everyHashMatchesDemoPassword() {
		for (String hash : hashes) {
			assertTrue(BCrypt.checkpw("password123", hash), "does not match password123: " + hash);
		}
	}

	// same password but own salt each, so no two hashes should be equal
	@Test
	void everyHashIsDifferent() {
		Set<String> unique = new HashSet<>(hashes);
		assertEquals(hashes.size(), unique.size());
	}
}
