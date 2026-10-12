package com.javel.migration;

import java.util.function.Consumer;

public class Schema {

    private String sql = "";

    public void create(String table, Consumer<TableBuilder> callback) {
        TableBuilder builder = new TableBuilder(table);
        callback.accept(builder);
        this.sql = builder.toSQL();
    }

    public void drop(String table) {
        this.sql = "DROP TABLE IF EXISTS " + table + ";";
    }

    public void addColumn(String table, Consumer<TableBuilder> callback) {
        TableBuilder builder = new TableBuilder(table);
        callback.accept(builder);
        // pega só a última coluna definida
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