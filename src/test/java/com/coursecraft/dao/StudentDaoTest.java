package com.coursecraft.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import com.coursecraft.model.Student;

// talks to real local MySQL, needs schema.sql and seed.sql loaded first
class StudentDaoTest {

	private final StudentDao studentDao = new StudentDao();

	@Test
	void findByEmailReturnsSeededStudent() throws SQLException {
		Student student = studentDao.findByEmail("jordan.lee@example.com");

		assertNotNull(student);
		assertEquals(1, student.getStudentId());
		assertEquals("jordan.lee@example.com", student.getEmail());
	}

	// checks hash stored in DB, not just one in seed.sql file
	@Test
	void seededStudentHashMatchesDemoPassword() throws SQLException {
		Student student = studentDao.findByEmail("jordan.lee@example.com");

		assertNotNull(student);
		assertTrue(BCrypt.checkpw("password123", student.getPasswordHash()));
	}

	@Test
	void findByEmailReturnsNullForUnknownEmail() throws SQLException {
		assertNull(studentDao.findByEmail("nobody@example.com"));
	}

	// if email got pasted into SQL text, this would match every row
	@Test
	void findByEmailTreatsInputAsDataNotSql() throws SQLException {
		assertNull(studentDao.findByEmail("' OR '1'='1"));
	}

	@Test
	void isAdminTrueForSeededAdmin() throws SQLException {
		assertTrue(studentDao.isAdmin(15, "admin@example.com"));
	}

	@Test
	void isAdminFalseForStudent() throws SQLException {
		assertFalse(studentDao.isAdmin(1, "jordan.lee@example.com"));
	}

	// admin id with another account's email must not pass
	@Test
	void isAdminFalseWhenEmailDoesNotMatchId() throws SQLException {
		assertFalse(studentDao.isAdmin(15, "jordan.lee@example.com"));
	}

	@Test
	void isAdminFalseForMissingStudent() throws SQLException {
		assertFalse(studentDao.isAdmin(999, "admin@example.com"));
	}
}
