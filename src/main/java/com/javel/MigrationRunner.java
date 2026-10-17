package com.javel;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class MigrationRunner {

    static {
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        System.setErr(new java.io.PrintStream(System.err, true, java.nio.charset.StandardCharsets.UTF_8));
    }

    private final DataSource dataSource;
    private final List<Migration> migrations;

    public MigrationRunner(DataSource dataSource, List<Migration> migrations) {
        this.dataSource = dataSource;
        this.migrations = migrations;
    }

    public void run() throws SQLException {
        TableBuilder.Dialect dialect = detectDialect();
        ensureHistoryTable();

        for (Migration migration : migrations) {
            String version = migration.version();

            if (alreadyRan(version)) {
                System.out.println("Skipping / Pulando: " + version);
                continue;
            }

            System.out.println("\nRunning / Rodando: " + version);

            History history = migration.history();
            if (!history.isEmpty()) {
                System.out.println("   History / Historico:");
                history.getEntries().forEach(e -> System.out.println("      " + e));
            }

            Schema schema = new Schema();
            schema.setDialect(dialect);
            migration.up(schema);

            // conexão única por migration
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    for (String sql : schema.getSQLs()) {
                        try (Statement st = conn.createStatement()) {
                            st.execute(sql);
                        }
                    }
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }

            record(version);
            recordHistory(version, "up", schema.getOperations(), history);

            System.out.println("   Done / Feito: " + version);
        }
    }

    public void rollback() throws SQLException {
        TableBuilder.Dialect dialect = detectDialect();

        if (migrations.isEmpty()) return;

        String last = getLastRan();
        if (last == null) {
            System.out.println("Nothing to rollback / Nada pra reverter!");
            return;
        }

        Migration migration = migrations.stream()
                .filter(m -> m.version().equals(last))
                .findFirst()
                .orElse(null);

        if (migration == null) return;

        System.out.println("\nRolling back / Revertendo: " + last);

        Schema schema = new Schema();
        schema.setDialect(dialect);
        migration.down(schema);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String sql : schema.getSQLs()) {
                    try (Statement st = conn.createStatement()) {
                        st.execute(sql);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        deleteRecord(last);
        recordHistory(last, "rollback", schema.getOperations(), migration.history());

        System.out.println("Rolled back / Revertido: " + last);
    }

    // ── Tabelas de controle ───────────────────────────────────

    private void ensureHistoryTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS javel_migrations (
                  id           INT AUTO_INCREMENT PRIMARY KEY,
                  migration    VARCHAR(255) NOT NULL,
                  executed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS javel_migration_history (
                  id              INT AUTO_INCREMENT PRIMARY KEY,
                  migration       VARCHAR(255) NOT NULL,
                  operacao        VARCHAR(10)  NOT NULL,
                  tabela          VARCHAR(255),
                  tipo_operacao   VARCHAR(30),
                  colunas         TEXT,
                  coluna_antiga   VARCHAR(255),
                  coluna_nova     VARCHAR(255),
                  executado_por   VARCHAR(255),
                  executado_em    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  entry           TEXT
                )
                """);
        }
    }

    // ── Gravar historico rico ─────────────────────────────────

    private void recordHistory(
            String version,
            String operacao,
            List<Schema.SchemaOperation> ops,
            History history
    ) throws SQLException {

        String executadoPor = System.getProperty("user.name", "unknown");
        String entries = history.isEmpty() ? null : String.join(" | ", history.getEntries());

        String sql = """
            INSERT INTO javel_migration_history
              (migration, operacao, tabela, tipo_operacao, colunas, coluna_antiga, coluna_nova, executado_por, entry)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (ops.isEmpty()) {
                ps.setString(1, version);
                ps.setString(2, operacao);
                ps.setNull(3, Types.VARCHAR);
                ps.setNull(4, Types.VARCHAR);
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.VARCHAR);
                ps.setString(8, executadoPor);
                ps.setString(9, entries);
                ps.executeUpdate();
                return;
            }

            for (Schema.SchemaOperation op : ops) {
                ps.setString(1, version);
                ps.setString(2, operacao);
                ps.setString(3, op.tabela);
                ps.setString(4, op.tipo.name());

                switch (op.tipo) {
                    case CREATE, ADD_COLUMN -> {
                        ps.setString(5, String.join(", ", op.colunas));
                        ps.setNull(6, Types.VARCHAR);
                        ps.setNull(7, Types.VARCHAR);
                    }
                    case DROP, RAW -> {
                        ps.setNull(5, Types.VARCHAR);
                        ps.setNull(6, Types.VARCHAR);
                        ps.setNull(7, Types.VARCHAR);
                    }
                    case DROP_COLUMN -> {
                        ps.setString(5, String.join(", ", op.colunas));
                        ps.setNull(6, Types.VARCHAR);
                        ps.setNull(7, Types.VARCHAR);
                    }
                    case RENAME_COLUMN, RENAME_TABLE -> {
                        ps.setNull(5, Types.VARCHAR);
                        ps.setString(6, op.colunaAntiga);
                        ps.setString(7, op.colunaNova);
                    }
                }

                ps.setString(8, executadoPor);
                ps.setString(9, entries);
                ps.executeUpdate();
            }
        }
    }

    // ── Privados ──────────────────────────────────────────────

    private TableBuilder.Dialect detectDialect() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL().toLowerCase();
            if (url.contains("mysql"))      return TableBuilder.Dialect.MYSQL;
            if (url.contains("postgresql")) return TableBuilder.Dialect.POSTGRESQL;
            return TableBuilder.Dialect.H2;
        }
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
}