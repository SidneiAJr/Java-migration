package com.javel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Schema {

    private String sql = "";
    private TableBuilder.Dialect dialect = TableBuilder.Dialect.H2;
    private final List<SchemaOperation> operations = new ArrayList<>();

    public static class SchemaOperation {
        public enum Tipo { CREATE, DROP, ADD_COLUMN, DROP_COLUMN, RENAME_COLUMN }

        public Tipo tipo;
        public String tabela;
        public List<String> colunas = new ArrayList<>();
        public String colunaAntiga; // rename
        public String colunaNova;   // rename
    }

    public void setDialect(TableBuilder.Dialect dialect) {
        this.dialect = dialect;
    }

    public void create(String table, Consumer<TableBuilder> callback) {
        TableBuilder builder = new TableBuilder(table, dialect);
        callback.accept(builder);
        this.sql = builder.toSQL();

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.CREATE;
        op.tabela = table;
        op.colunas = new ArrayList<>(builder.getColumnNames());
        operations.add(op);
    }

    public void drop(String table) {
        this.sql = "DROP TABLE IF EXISTS " + table + ";";

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.DROP;
        op.tabela = table;
        operations.add(op);
    }

    public void addColumn(String table, Consumer<TableBuilder> callback) {
        TableBuilder builder = new TableBuilder(table, dialect);
        callback.accept(builder);
        String colSQL = builder.toSQL()
                .replaceAll("CREATE TABLE .*?\\(\n  ", "")
                .replaceAll("\n\\);", "");
        this.sql = "ALTER TABLE " + table + " ADD COLUMN " + colSQL + ";";

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.ADD_COLUMN;
        op.tabela = table;
        op.colunas = new ArrayList<>(builder.getColumnNames());
        operations.add(op);
    }

    public void dropColumn(String table, String column) {
        this.sql = "ALTER TABLE " + table + " DROP COLUMN " + column + ";";

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.DROP_COLUMN;
        op.tabela = table;
        op.colunas = List.of(column);
        operations.add(op);
    }

    public void renameColumn(String table, String from, String to) {
        this.sql = "ALTER TABLE " + table + " RENAME COLUMN " + from + " TO " + to + ";";

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.RENAME_COLUMN;
        op.tabela = table;
        op.colunaAntiga = from;
        op.colunaNova = to;
        operations.add(op);
    }

    public String getSQL() { return sql; }
    public List<SchemaOperation> getOperations() { return operations; }
}