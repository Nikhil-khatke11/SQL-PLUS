import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * JavaSqlPlus - A simple SQL*Plus-like CLI tool for Oracle Database
 *
 * Features:
 * - Executes any SQL: SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, etc.
 * - Dynamically reads and displays all columns (no hardcoded column names)
 * - Formatted table output with column headers and separators
 * - Shows row count for DML statements
 * - Commands: exit / quit to close, cls to clear, conn to reconnect
 * - Supports multi-line SQL (end with semicolon or slash on new line)
 */
public class JavaSqlPlus {

	// ── ANSI colors for a nicer terminal look ──────────────────────────────
	static final String RESET = "\u001B[0m";
	static final String CYAN = "\u001B[36m";
	static final String GREEN = "\u001B[32m";
	static final String YELLOW = "\u001B[33m";
	static final String RED = "\u001B[31m";
	static final String BOLD = "\u001B[1m";

	// ── Default connection settings (change as needed) ─────────────────────
	static String url = "jdbc:oracle:thin:@localhost:1521:orcl";
	static String dbUser = "system";
	static String dbPass = "nikhil";

	public static void main(String[] args) {

		printBanner();

		Scanner sc = new Scanner(System.in);
		Connection con = null;

		// ── Try connecting at startup ──────────────────────────────────────
		try {
			con = connect(url, dbUser, dbPass);
			System.out.println(GREEN + "Connected to: " + url + RESET);
			System.out.println(GREEN + "User        : " + dbUser.toUpperCase() + RESET);
		} catch (SQLException e) {
			System.out.println(RED + "Connection failed: " + e.getMessage() + RESET);
			System.out.println(YELLOW + "Type 'conn user/pass@host:port:sid'  to reconnect." + RESET);
		}

		System.out.println();

		// ── Main REPL loop ─────────────────────────────────────────────────
		StringBuilder sqlBuffer = new StringBuilder();

		while (true) {

			// Prompt: show continuation dots if mid-statement
			if (sqlBuffer.length() == 0) {
				System.out.print(CYAN + "SQL> " + RESET);
			} else {
				System.out.print(CYAN + "  2> " + RESET);
			}

			String line = sc.nextLine().trim();

			// ── Special commands -----------------------------------------------

			// Empty line → skip
			if (line.isEmpty())
				continue;

			// Exit
			if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
				System.out.println(YELLOW + "Disconnected. Bye!" + RESET);
				closeConnection(con);
				break;
			}

			// Clear screen (works on Windows & Linux)
			if (line.equalsIgnoreCase("cls") || line.equalsIgnoreCase("clear")) {
				System.out.print("\033[H\033[2J");
				System.out.flush();
				continue;
			}

			// Show connection info
			if (line.equalsIgnoreCase("conn?") || line.equalsIgnoreCase("status")) {
				showStatus(con);
				continue;
			}

			// Reconnect: conn user/pass@host:port:sid
			if (line.toLowerCase().startsWith("conn ")) {
				con = handleConn(line, con);
				continue;
			}

			// Help
			if (line.equalsIgnoreCase("help") || line.equalsIgnoreCase("?")) {
				printHelp();
				continue;
			}

			// ── Accumulate SQL lines -------------------------------------------

			// A slash "/" on its own line executes the buffer
			if (line.equals("/")) {
				if (sqlBuffer.length() > 0) {
					executeSQL(sqlBuffer.toString().trim(), con);
					sqlBuffer.setLength(0);
				}
				continue;
			}

			// Strip trailing semicolon
			boolean endsWithSemi = line.endsWith(";");
			if (endsWithSemi) {
				line = line.substring(0, line.length() - 1).trim();
			}

			sqlBuffer.append(line).append(" ");

			// Execute when semicolon found (or single-line command)
			if (endsWithSemi) {
				executeSQL(sqlBuffer.toString().trim(), con);
				sqlBuffer.setLength(0);
			}
		}

		sc.close();
	}

	// ── Execute any SQL statement ──────────────────────────────────────────
	static void executeSQL(String sql, Connection con) {
		if (sql.isEmpty())
			return;

		if (con == null) {
			System.out.println(RED + "ERROR: Not connected. Use 'conn user/pass@host:port:sid'" + RESET);
			return;
		}

		String upper = sql.trim().toUpperCase();

		try (Statement stmt = con.createStatement()) {

			// ── SELECT / SHOW queries ──────────────────────────────────────
			if (upper.startsWith("SELECT") || upper.startsWith("SHOW")
					|| upper.startsWith("WITH") || upper.startsWith("DESCRIBE")
					|| upper.startsWith("DESC")) {

				// Oracle DESC → use metadata approach
				if (upper.startsWith("DESC")) {
					describeTable(sql, con);
					return;
				}

				try (ResultSet rs = stmt.executeQuery(sql)) {
					int rows = printResultSet(rs);
					System.out.println(GREEN + rows + " row(s) selected." + RESET + "\n");
				}

			} else {
				// ── DML / DDL ──────────────────────────────────────────────
				int affected = stmt.executeUpdate(sql);
				System.out.println(GREEN + affected + " row(s) affected." + RESET + "\n");

				// Auto-commit is on by default; show tip for transaction control
			}

		} catch (SQLException e) {
			System.out.println(RED + "ERROR: " + e.getMessage() + RESET);
			System.out.println(RED + "SQLState: " + e.getSQLState()
					+ "  Code: " + e.getErrorCode() + RESET + "\n");
		}
	}

	// ── Print ResultSet as a formatted table ───────────────────────────────
	static int printResultSet(ResultSet rs) throws SQLException {
		ResultSetMetaData meta = rs.getMetaData();
		int colCount = meta.getColumnCount();

		// Collect column names and widths
		String[] colNames = new String[colCount];
		int[] colWidth = new int[colCount];

		for (int i = 1; i <= colCount; i++) {
			colNames[i - 1] = meta.getColumnLabel(i).toUpperCase();
			colWidth[i - 1] = Math.max(colNames[i - 1].length(), meta.getColumnDisplaySize(i));
			// Cap very wide columns at 40 chars for display
			colWidth[i - 1] = Math.min(colWidth[i - 1], 40);
			colWidth[i - 1] = Math.max(colWidth[i - 1], 5); // minimum 5
		}

		// ── Print header ───────────────────────────────────────────────────
		System.out.println();
		printSeparator(colWidth);
		printRow(colNames, colWidth, true);
		printSeparator(colWidth);

		// ── Print rows ─────────────────────────────────────────────────────
		int rowCount = 0;
		while (rs.next()) {
			rowCount++;
			String[] rowData = new String[colCount];
			for (int i = 1; i <= colCount; i++) {
				String val = rs.getString(i);
				rowData[i - 1] = (val == null) ? "(null)" : val;
			}
			printRow(rowData, colWidth, false);
		}

		printSeparator(colWidth);
		return rowCount;
	}

	static void printSeparator(int[] widths) {
		StringBuilder sb = new StringBuilder("+");
		for (int w : widths) {
			sb.append("-".repeat(w + 2)).append("+");
		}
		System.out.println(sb);
	}

	static void printRow(String[] data, int[] widths, boolean isHeader) {
		StringBuilder sb = new StringBuilder("|");
		for (int i = 0; i < data.length; i++) {
			String cell = data[i];
			if (cell.length() > widths[i]) {
				cell = cell.substring(0, widths[i] - 1) + "…";
			}
			String fmt = " %-" + widths[i] + "s |";
			if (isHeader) {
				sb.append(BOLD).append(String.format(fmt, cell)).append(RESET);
			} else {
				sb.append(String.format(fmt, cell));
			}
		}
		System.out.println(sb);
	}

	// ── DESCRIBE table ─────────────────────────────────────────────────────
	static void describeTable(String sql, Connection con) throws SQLException {
		// Extract table name: DESC tableName or DESCRIBE tableName
		String[] parts = sql.trim().split("\\s+");
		if (parts.length < 2) {
			System.out.println(RED + "Usage: DESC tableName" + RESET);
			return;
		}
		String tableName = parts[1].toUpperCase();

		DatabaseMetaData meta = con.getMetaData();
		try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
			System.out.println();
			System.out.printf(BOLD + " %-30s %-20s %-10s %-10s%n" + RESET,
					"COLUMN_NAME", "DATA_TYPE", "SIZE", "NULLABLE");
			System.out.println("-".repeat(72));

			int count = 0;
			while (rs.next()) {
				count++;
				System.out.printf(" %-30s %-20s %-10s %-10s%n",
						rs.getString("COLUMN_NAME"),
						rs.getString("TYPE_NAME"),
						rs.getString("COLUMN_SIZE"),
						rs.getString("IS_NULLABLE"));
			}
			if (count == 0) {
				System.out.println(YELLOW + "Table '" + tableName + "' not found or no columns." + RESET);
			}
			System.out.println();
		}
	}

	// ── Connect helper ─────────────────────────────────────────────────────
	static Connection connect(String url, String user, String pass) throws SQLException {
		return DriverManager.getConnection(url, user, pass);
	}

	// ── Handle CONN command: conn user/pass@host:port:sid ─────────────────
	static Connection handleConn(String line, Connection oldCon) {
		closeConnection(oldCon);
		// Format: conn user/password@host:port:sid
		try {
			String spec = line.substring(5).trim(); // remove "conn "
			String[] atSplit = spec.split("@");
			if (atSplit.length != 2)
				throw new IllegalArgumentException("bad format");

			String[] userPass = atSplit[0].split("/");
			if (userPass.length != 2)
				throw new IllegalArgumentException("missing password");

			String newUser = userPass[0];
			String newPass = userPass[1];
			String newUrl = "jdbc:oracle:thin:@" + atSplit[1];

			Connection con = connect(newUrl, newUser, newPass);
			System.out.println(GREEN + "Connected to: " + newUrl + RESET);
			System.out.println(GREEN + "User        : " + newUser.toUpperCase() + RESET + "\n");

			url = newUrl;
			dbUser = newUser;
			dbPass = newPass;
			return con;

		} catch (Exception e) {
			System.out.println(RED + "Connection failed: " + e.getMessage() + RESET);
			System.out.println(YELLOW + "Format: conn user/password@host:port:sid" + RESET + "\n");
			return null;
		}
	}

	// ── Show current connection status ─────────────────────────────────────
	static void showStatus(Connection con) {
		if (con == null) {
			System.out.println(RED + "Not connected." + RESET + "\n");
			return;
		}
		try {
			System.out.println(GREEN + "Connected  : " + !con.isClosed() + RESET);
			System.out.println(GREEN + "URL        : " + url + RESET);
			System.out.println(GREEN + "User       : " + dbUser.toUpperCase() + RESET);
			System.out.println(GREEN + "AutoCommit : " + con.getAutoCommit() + RESET + "\n");
		} catch (SQLException e) {
			System.out.println(RED + "Error: " + e.getMessage() + RESET);
		}
	}

	static void closeConnection(Connection con) {
		if (con != null) {
			try {
				con.close();
			} catch (SQLException ignored) {
			}
		}
	}

	// ── Startup banner ─────────────────────────────────────────────────────
	static void printBanner() {
		System.out.println(CYAN + BOLD);
		System.out.println("╔══════════════════════════════════════════╗");
		System.out.println("║        JavaSqlPlus  v1.0                 ║");
		System.out.println("║   A SQL*Plus-like CLI for Oracle DB       ║");
		System.out.println("║   Type 'help' or '?' for commands         ║");
		System.out.println("╚══════════════════════════════════════════╝");
		System.out.println(RESET);
	}

	// ── Help text ──────────────────────────────────────────────────────────
	static void printHelp() {
		System.out.println(YELLOW + BOLD + "\n── JavaSqlPlus Commands ──────────────────────────────" + RESET);
		System.out.println(YELLOW + "  exit / quit             " + RESET + "- Disconnect and exit");
		System.out.println(YELLOW + "  conn user/pass@host:port:sid " + RESET + "- Connect to a database");
		System.out.println(YELLOW + "  status / conn?          " + RESET + "- Show connection info");
		System.out.println(YELLOW + "  cls / clear             " + RESET + "- Clear the screen");
		System.out.println(YELLOW + "  help / ?                " + RESET + "- Show this help");
		System.out.println(YELLOW + BOLD + "\n── SQL Usage ─────────────────────────────────────────" + RESET);
		System.out.println(YELLOW + "  SELECT * FROM emp;      " + RESET + "- End with ; to execute");
		System.out.println(YELLOW + "  INSERT INTO ...\\n/       " + RESET + "- Or end multi-line with /");
		System.out.println(YELLOW + "  DESC tableName;         " + RESET + "- Show table structure");
		System.out.println(YELLOW + "  CREATE TABLE ...;       " + RESET + "- DDL is supported");
		System.out.println();
	}
}
