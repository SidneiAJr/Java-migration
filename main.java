import java.sql.SQLException;
import org.h2.jdbcx.JdbcDataSource;
import java.util.List;

public class main {
    public static void main(String[] args) throws SQLException {

        // banco H2 em memória — só pra testar
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        // registra as migrations
        MigrationRunner runner = new MigrationRunner(ds, List.of(
                new CreateUsersTable()
        ));

        // roda
        runner.run();
    }
}