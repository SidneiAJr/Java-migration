package com.javel;
import java.sql.SQLException;
import org.h2.jdbcx.JdbcDataSource;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException {

        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        MigrationRunner runner = new MigrationRunner(ds, List.of(
                new CreateUsersTable(),
                new CreatePostsTable()
        ));

        System.out.println("=== RODANDO MIGRATIONS ===");
        runner.run();

        System.out.println("\n=== RODANDO DE NOVO (deve pular) ===");
        runner.run();

        System.out.println("\n=== ROLLBACK ===");
        runner.rollback();

        System.out.println("\n=== RODANDO DEPOIS DO ROLLBACK ===");
        runner.run();
    }
}