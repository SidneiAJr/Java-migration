package com.javel;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class MigrationCLI {

    public static void main(String[] args) throws Exception {
        String command = args.length > 0 ? args[0] : "migrate";

        Properties props = loadProperties();
        DataSource ds = buildDataSource(props);
        List<Migration> migrations = scanMigrations();

        MigrationRunner runner = new MigrationRunner(ds, migrations);

        switch (command) {
            case "migrate" -> {
                System.out.println("\n=== RODANDO MIGRATIONS / RUNNING MIGRATIONS ===");
                runner.run();
            }
            case "rollback" -> {
                System.out.println("\n=== ROLLBACK ===");
                runner.rollback();
            }
            default -> System.out.println("Comando desconhecido / Unknown command: " + command);
        }

        System.exit(0);
    }

    private static Properties loadProperties() throws IOException {
        Path path = Paths.get("src/main/resources/application.properties");
        if (!Files.exists(path)) {
            throw new RuntimeException("application.properties nao encontrado / not found.");
        }
        Properties props = new Properties();
        props.load(new FileInputStream(path.toFile()));
        return props;
    }

    private static DataSource buildDataSource(Properties props) throws Exception {
        String url = props.getProperty("spring.datasource.url", "");
        String user = props.getProperty("spring.datasource.username", "sa");
        String password = props.getProperty("spring.datasource.password", "");

        if (url.contains("h2")) {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL(url);
            ds.setUser(user);
            ds.setPassword(password);
            return ds;
        }

        if (url.contains("mysql")) {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } else if (url.contains("postgresql")) {
            Class.forName("org.postgresql.Driver");
        }

        return new SimpleDataSource(url, user, password);
    }

    @SuppressWarnings("unchecked")
    private static List<Migration> scanMigrations() throws Exception {
        Path classesDir = Paths.get("target/classes");
        if (!Files.exists(classesDir)) {
            throw new RuntimeException("target/classes nao encontrado. Rode 'mvn compile' primeiro / Run 'mvn compile' first.");
        }

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ classesDir.toUri().toURL() },
                MigrationCLI.class.getClassLoader()
        );

        List<Migration> migrations = new ArrayList<>();

        Files.walk(classesDir)
                .filter(p -> p.toString().endsWith(".class"))
                .filter(p -> p.toString().contains("migration"))
                .forEach(p -> {
                    String relative = classesDir.relativize(p).toString();
                    String className = relative
                            .replace(FileSystems.getDefault().getSeparator(), ".")
                            .replace(".class", "");

                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (Migration.class.isAssignableFrom(clazz)
                                && !Modifier.isAbstract(clazz.getModifiers())) {
                            migrations.add((Migration) clazz.getDeclaredConstructor().newInstance());
                        }
                    } catch (Exception ignored) {}
                });

        migrations.sort(Comparator.comparing(m -> m.getClass().getSimpleName()));
        return migrations;
    }

    static class SimpleDataSource implements DataSource {
        private final String url, user, password;

        SimpleDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }
        @Override public Connection getConnection(String u, String p) throws SQLException {
            return DriverManager.getConnection(url, u, p);
        }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter pw) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return null; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}