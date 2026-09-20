package com.coursecraft.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/* MySQL connections. */
public final class Db {

	// allowPublicKeyRetrieval: MySQL 8 auth, unencrypted.
	private static final String URL = "jdbc:mysql://localhost:3306/coursecraft"
			+ "?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";

	// From schema.sql. Rows only, localhost, not secret.
	private static final String USER = "coursecraft";
	private static final String PASSWORD = "coursecraft_dev";

	static {
		// Tomcat can miss WEB-INF/lib. Register explicitly.
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("MySQL driver not found in WEB-INF/lib.", e);
		}
	}

	private Db() {
	}

	// Caller closes. TODO: use a pool.
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}
}
