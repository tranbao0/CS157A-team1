# CourseCraft

SJSU course planning and analysis platform.

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
