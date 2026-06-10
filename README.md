JavaSqlPlus 🗄️
===============

A simple command-line tool for Oracle Database, just like SQL*Plus, but written in Java! ☕


📌 What is this?
-----------------
JavaSqlPlus is a beginner-friendly Java project that lets you connect to an Oracle Database
and run SQL commands directly from your terminal. Think of it as a lightweight version of
Oracle's own SQL*Plus tool.


✨ Features
-----------
✅ Run any SQL: SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, etc.
✅ Nicely formatted table output with column headers
✅ DESC tableName to see table structure
✅ Multi-line SQL support (end with ; or /)
✅ Colorful terminal output using ANSI colors
✅ Reconnect to a different database without restarting


🛠️ Requirements
----------------
🔹 Java (JDK) 8 or higher
🔹 Oracle Database (XE is fine)
🔹 Oracle JDBC Driver (ojdbc8.jar or similar)


🚀 How to Run
--------------

Step 1 - Download the Oracle JDBC Driver 📥
  Download ojdbc8.jar from https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html
  and place it in the project folder.

Step 2 - Set your database credentials 🔐
  Open JavaSqlPlus.java and update these lines with your own details:

    static String url    = "jdbc:oracle:thin:@localhost:1521:orcl";
    static String dbUser = "yourdb_userName";
    static String dbPass = "yourdb_password";

Step 3 - Compile 🔨

  Windows:
    javac -cp .;ojdbc8.jar JavaSqlPlus.java

  Linux / Mac:
    javac -cp .:ojdbc8.jar JavaSqlPlus.java

Step 4 - Run ▶️

  Windows:
    java -cp .;ojdbc8.jar JavaSqlPlus

  Linux / Mac:
    java -cp .:ojdbc8.jar JavaSqlPlus


💻 Available Commands
----------------------
  SELECT * FROM tableName;              - Run a SELECT query
  INSERT INTO ...;                      - Insert a row
  DESC tableName;                       - Show table columns and types
  conn user/pass@host:port:sid          - Connect to a different database
  status  or  conn?                     - Show current connection info
  cls  or  clear                        - Clear the screen 🧹
  help  or  ?                           - Show help
  exit  or  quit                        - Exit the program 🚪


📖 Example Usage
-----------------
  SQL> SELECT * FROM employees;

  +----+----------+-----------+
  | ID | NAME     | SALARY    |
  +----+----------+-----------+
  |  1 | Alice    | 50000     |
  |  2 | Bob      | 60000     |
  +----+----------+-----------+
  2 row(s) selected. ✅

  SQL> DESC employees;

   COLUMN_NAME     DATA_TYPE    SIZE    NULLABLE
  ------------------------------------------------
   ID              NUMBER       10      NO
   NAME            VARCHAR2     100     YES
   SALARY          NUMBER       10      YES

  SQL> exit
  Disconnected. Bye! 👋


📂 Project Structure
---------------------
  java_Sql_project/
  |
  |-- JavaSqlPlus.java     (Main source file, all code is here) ⭐
  |-- JavaSqlPlus.class    (Compiled class file, auto-generated)
  |-- ojdbc8.jar           (Oracle JDBC driver, you add this)
  |-- README.md            (This file) 📄


❓ Common Issues
----------------
🔴 Connection failed?
  - Make sure Oracle Database is running on your machine.
  - Double-check your url, dbUser, and dbPass values.
  - Make sure the ojdbc8.jar is in the same folder.

🔴 javac not found?
  - Java is not installed or not added to your system PATH.
  - Install JDK from https://www.java.com and try again.


👨‍💻 Author : Nikhil Khatke
-----------------------------
Made with ❤️ as a learning project to understand JDBC and Oracle Database connectivity in Java.

