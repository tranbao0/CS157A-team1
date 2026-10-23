package com.coursecraft.model;

// One row of students table, only columns login needs for now
public class Student {

	private final int studentId;
	private final String email;
	private final String passwordHash;

	public Student(int studentId, String email, String passwordHash) {
		this.studentId = studentId;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	public int getStudentId() {
		return studentId;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}
}
