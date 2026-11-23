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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import com.coursecraft.util.Passwords;

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

	// login's dummy hash uses app cost, seed hashes must match or timing leaks again
	@Test
	void everyHashUsesAppCost() {
		String expectedStart = String.format("$2a$%02d$", Passwords.BCRYPT_COST);
		for (String hash : hashes) {
			assertTrue(hash.startsWith(expectedStart), "different cost: " + hash);
		}
	}

	// transcript GE rows, roadmap GE status, and plan GE picks need courses in every area.
	// SJSU lists only two Area 1A courses (ENGL 1A, ENGL 1AS), every other area needs three.
	@Test
	void everyGeAreaHasEnoughCourses() throws IOException {
		String seed = Files.readString(Path.of("sql", "seed.sql"));

		Map<String, Integer> counts = new TreeMap<>();
		Matcher areas = Pattern.compile("^\\s*\\('([^']+)',\\s*'[^']+'\\)", Pattern.MULTILINE)
				.matcher(insertBlock(seed, "ge_areas"));
		while (areas.find()) {
			counts.put(areas.group(1), 0);
		}
		assertFalse(counts.isEmpty(), "no GE areas found in seed.sql");

		// last value of each courses row is ge_area_code, NULL or 'code'
		Matcher courses = Pattern.compile("^\\s*\\(\\d+,.*,\\s*(?:NULL|'([^']+)')\\)[,;]", Pattern.MULTILINE)
				.matcher(insertBlock(seed, "courses"));
		while (courses.find()) {
			String area = courses.group(1);
			if (area != null) {
				assertTrue(counts.containsKey(area), "course has unknown GE area: " + area);
				counts.merge(area, 1, Integer::sum);
			}
		}

		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			int needed = entry.getKey().equals("1A") ? 2 : 3;
			assertTrue(entry.getValue() >= needed,
					"GE area " + entry.getKey() + " has " + entry.getValue() + " courses, needs " + needed);
		}
	}

	// text of one INSERT INTO table statement, up to its closing semicolon
	private static String insertBlock(String seed, String table) {
		int start = seed.indexOf("INSERT INTO " + table + " ");
		assertTrue(start >= 0, "no INSERT INTO " + table + " in seed.sql");
		return seed.substring(start, seed.indexOf(";", start) + 1);
	}

	// same password but own salt each, so no two hashes should be equal
	@Test
	void everyHashIsDifferent() {
		Set<String> unique = new HashSet<>(hashes);
		assertEquals(hashes.size(), unique.size());
	}
}
