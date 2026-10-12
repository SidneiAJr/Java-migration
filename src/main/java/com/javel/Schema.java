package com.javel;
import java.util.function.Consumer;

public class Schema {

    private String sql = "";
    private TableBuilder.Dialect dialect = TableBuilder.Dialect.H2;

    public void setDialect(TableBuilder.Dialect dialect) {
        this.dialect = dialect;
    }

    public void create(String table, Consumer<TableBuilder> callback) {
        TableBuilder builder = new TableBuilder(table, dialect);
        callback.accept(builder);
        this.sql = builder.toSQL();
    }

    public void drop(String table) {
        this.sql = "DROP TABLE IF EXISTS " + table + ";";
    }

    public void addColumn(String table, Consumer<TableBuilder> callback) {
        TableBuilder builder = new TableBuilder(table, dialect);
        callback.accept(builder);
        String colSQL = builder.toSQL()
                .replaceAll("CREATE TABLE .*?\\(\n  ", "")
                .replaceAll("\n\\);", "");
        this.sql = "ALTER TABLE " + table + " ADD COLUMN " + colSQL + ";";
    }

    public void dropColumn(String table, String column) {
        this.sql = "ALTER TABLE " + table + " DROP COLUMN " + column + ";";
    }

    public void renameColumn(String table, String from, String to) {
        this.sql = "ALTER TABLE " + table + " RENAME COLUMN " + from + " TO " + to + ";";
    }

    public String getSQL() {
        return sql;
    }
}