package net.onrc.openvirtex.debugger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Properties;

public class DBHandler {

	private final static Logger log = LogManager.getLogger(DBHandler.class
			.getName());

	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost/vsdn_debugger";

	// Database credentials
	static final String USER = "vsdn";
	static final String PASS = "debugger";

	static Connection conn = null;

	public static void connect() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);

		} catch (ClassNotFoundException e) {
			log.error(e.toString());
		} catch (SQLException e) {
			log.error(e.toString());
		}
	}

	public static void disconnect() {
		try {
			conn.close();
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
	}

	public static boolean insertValues(String query) {
		boolean status = false;
		try {
			Statement stmt = conn.createStatement();
			int rc = stmt.executeUpdate(query);
			if (rc > 0) {
				status = true;
			}

		} catch (SQLException e) {
			log.error(e.getMessage());
		}

		return status;
	}

	public static ResultSet getValues(String query) {

		ResultSet rs = null;
		try {
			Statement stmt = conn.createStatement();
			rs = stmt.executeQuery(query);

		} catch (SQLException e) {
			log.error(e.getMessage());
		}

		return rs;
	}

}
