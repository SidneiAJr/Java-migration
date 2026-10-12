package com.javel.migration;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class MigrationRunner {

    private final DataSource dataSource;
    private final List<Migration> migrations;

    public MigrationRunner(DataSource dataSource, List<Migration> migrations) {
        this.dataSource = dataSource;
        this.migrations = migrations;
    }

    public void run() throws SQLException {
        ensureHistoryTable();

        for (Migration migration : migrations) {
            String version = migration.version();

            if (alreadyRan(version)) {
                System.out.println("⏭  Skipping " + version);
                continue;
            }

            System.out.println("\n▶  Running " + version);

            // printa o history da migration
            History history = migration.history();
            if (!history.isEmpty()) {
                System.out.println("   📋 History:");
                history.getEntries().forEach(e -> System.out.println("      " + e));
            }

            Schema schema = new Schema();
            migration.up(schema);

            execute(schema.getSQL());
            record(version);

            System.out.println("   ✔ Done");
        }
    }

    public void rollback() throws SQLException {
        if (migrations.isEmpty()) return;

        // pega a última migration executada
        String last = getLastRan();
        if (last == null) {
            System.out.println("⚠  Nada pra reverter.");
            return;
        }

        Migration migration = migrations.stream()
                .filter(m -> m.version().equals(last))
                .findFirst()
                .orElse(null);

        if (migration == null) return;

        System.out.println("\n▶  Rolling back " + last);

        Schema schema = new Schema();
        migration.down(schema);

        execute(schema.getSQL());
        deleteRecord(last);

        System.out.println("   ✔ Rolled back");
    }

    // ---- privados ----

    private void ensureHistoryTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS javel_migrations (
              id           INT AUTO_INCREMENT PRIMARY KEY,
              migration    VARCHAR(255) NOT NULL,
              executed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;
        execute(sql);
    }

    private boolean alreadyRan(String version) throws SQLException {
        String sql = "SELECT COUNT(*) FROM javel_migrations WHERE migration = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, version);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private String getLastRan() throws SQLException {
        String sql = "SELECT migration FROM javel_migrations ORDER BY id DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString("migration") : null;
        }
    }

    private void record(String version) throws SQLException {
        String sql = "INSERT INTO javel_migrations (migration) VALUES (?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, version);
            ps.executeUpdate();
        }
    }

    private void deleteRecord(String version) throws SQLException {
        String sql = "DELETE FROM javel_migrations WHERE migration = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, version);
            ps.executeUpdate();
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }
}