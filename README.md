# CourseCraft

SJSU course planning and analysis platform.

## Running locally

You need JDK 21, Apache Tomcat 10.1, and MySQL Community Server.

### 1. Load the database

Run `sql/schema.sql` and then `sql/seed.sql` as a MySQL admin user (for example root), in MySQL Workbench or from the terminal:

```sh
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/seed.sql
```

`schema.sql` deletes and rebuilds the `coursecraft` database, and creates the app's MySQL user `coursecraft` (password `coursecraft_dev`).
Run both files again every time you pull a change to either one.
If you skip this, sign-in or sign-up can fail with a database error.

### 2. Sign in with a sample account

All 12 sample students in `seed.sql` use the password `password123`, for example `jordan.lee@example.com`.
You can also create a new account on the Create account page.

### 3. Run the app

Tomcat compiles JSP pages by itself, but it does not compile the Java classes in `src/main/java` (servlets, DAOs).
If the classes are missing or old, pages like `/login` return 404 or run old code.

**Eclipse:** right-click the project and choose Run As > Run on Server, then pick Tomcat v10.1.
Eclipse compiles the Java code and publishes the app for you.
The app opens at `http://localhost:8080/CourseCraft/`.

**Any other setup** (for example VS Code with Community Server Connector, or Tomcat started by hand): compile the Java code into `src/main/webapp/WEB-INF/classes` first.
From the repo root in Git Bash, with `TC` set to your Tomcat folder:

```sh
TC="C:/apache-tomcat-10.1.59"
javac --release 21 -cp "$TC/lib/*;src/main/webapp/WEB-INF/lib/*" -d src/main/webapp/WEB-INF/classes $(find src/main/java -name '*.java')
```

Then deploy `src/main/webapp` to Tomcat.
After every change to a `.java` file, run the command again and restart Tomcat.
`.class` files are ignored by Git, so this never adds files to a commit.
The URL depends on the deployed folder name, for example `http://localhost:8080/coursecraft/`.

## Running tests

Tests live in `src/test/java` and use JUnit 6 (`lib/test/junit-platform-console-standalone-6.1.3.jar`).
`StudentDaoTest` needs local MySQL running with `sql/schema.sql` and `sql/seed.sql` loaded.
`SeedDataTest` only reads `sql/seed.sql` and needs no database.

From the repo root in Git Bash, set `TC` to your Tomcat folder, then compile and run:

```sh
TC="C:/apache-tomcat-10.1.59"
CP="$TC/lib/*;src/main/webapp/WEB-INF/lib/*;lib/test/*"
javac --release 21 -d build/test-classes -cp "$CP" $(find src/main/java src/test/java -name '*.java')
java -cp "build/test-classes;$CP" org.junit.platform.console.ConsoleLauncher execute --scan-class-path
```

On macOS or Linux, use `:` instead of `;` between classpath entries.
`lib/test/tomcat-juli-10.1.59.jar` is a copy of Tomcat's logging JAR.
The Tomcat connection pool needs it outside Tomcat, and Tomcat keeps it in `bin`, which neither Eclipse nor `$TC/lib/*` includes.

In Eclipse, right-click a test class or `src/test/java` and choose Run As > JUnit Test.
