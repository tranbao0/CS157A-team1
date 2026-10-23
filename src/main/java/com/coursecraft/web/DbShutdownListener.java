package com.coursecraft.web;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import com.coursecraft.util.Db;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/*
 * Tomcat calls contextDestroyed when app stops or reloads.
 * Without this, every reload leaves old pool's connections open,
 * MySQL driver registered and its background thread running.
 */
@WebListener
public class DbShutdownListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		try {
			Db.closePool();
		} catch (SQLException e) {
			event.getServletContext().log("Could not close connection pool.", e);
		}

		/*
		 * Driver registers itself with DriverManager when it loads.
		 * Unregister ours so old copy of app can be freed, Tomcat warns otherwise.
		 * Only drivers loaded by this app's class loader, not ones other apps own.
		 */
		ClassLoader appClassLoader = getClass().getClassLoader();
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			if (driver.getClass().getClassLoader() == appClassLoader) {
				try {
					DriverManager.deregisterDriver(driver);
				} catch (SQLException e) {
					event.getServletContext().log("Could not unregister JDBC driver.", e);
				}
			}
		}

		// MySQL driver starts this thread itself, Tomcat warns at shutdown if it is still running
		AbandonedConnectionCleanupThread.checkedShutdown();
	}
}
