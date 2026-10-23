package com.coursecraft.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

/* MySQL connections. */
public final class Db {

	// allowPublicKeyRetrieval: MySQL 8 auth, unencrypted.
	private static final String URL = "jdbc:mysql://localhost:3306/coursecraft"
			+ "?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";

	// From schema.sql. Rows only, localhost, not secret.
	private static final String USER = "coursecraft";
	private static final String PASSWORD = "coursecraft_dev";

	// one pool for whole app, made once when class first loads
	private static final BasicDataSource POOL = createPool();

	private Db() {
	}

	/*
	 * Pool class comes from Tomcat's own lib (tomcat-dbcp.jar), so no extra JAR.
	 * Pool keeps some connections open and lends them out,
	 * instead of opening new connection on every request.
	 */
	private static BasicDataSource createPool() {
		BasicDataSource pool = new BasicDataSource();
		pool.setDriverClassName("com.mysql.cj.jdbc.Driver");
		// pool lives in Tomcat lib but driver is in WEB-INF/lib, so pool needs our class loader to find it
		pool.setDriverClassLoader(Db.class.getClassLoader());
		pool.setUrl(URL);
		pool.setUsername(USER);
		pool.setPassword(PASSWORD);

		// at most 20 open at once, MySQL allows 151 by default
		pool.setMaxTotal(20);
		// when all 20 busy, wait 5s then fail instead of hanging forever
		pool.setMaxWait(Duration.ofSeconds(5));
		return pool;
	}

	// Caller closes. Close gives connection back to pool, doesnt really close it.
	public static Connection getConnection() throws SQLException {
		return POOL.getConnection();
	}

	// closes every pooled connection, DbShutdownListener calls this when app stops
	public static void closePool() throws SQLException {
		POOL.close();
	}
}
