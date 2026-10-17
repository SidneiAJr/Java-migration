package com.javel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Schema {

    private final List<String> sqls = new ArrayList<>();
    private final List<SchemaOperation> operations = new ArrayList<>();
    private TableBuilder.Dialect dialect = TableBuilder.Dialect.H2;

    public static class SchemaOperation {
        public enum Tipo { CREATE, DROP, ADD_COLUMN, DROP_COLUMN, RENAME_COLUMN, RENAME_TABLE, RAW }

        public Tipo tipo;
        public String tabela;
        public List<String> colunas = new ArrayList<>();
        public String colunaAntiga;
        public String colunaNova;
    }

    public void setDialect(TableBuilder.Dialect dialect) {
        this.dialect = dialect;
    }

    public void create(String table, Consumer<TableBuilder> callback) {
        TableBuilder builder = new TableBuilder(table, dialect);
        callback.accept(builder);
        sqls.add(builder.toSQL());
        sqls.addAll(builder.getIndexSQL());

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.CREATE;
        op.tabela = table;
        op.colunas = new ArrayList<>(builder.getColumnNames());
        operations.add(op);
    }

    public void drop(String table) {
        sqls.add("DROP TABLE IF EXISTS " + table + ";");

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
        sqls.add("ALTER TABLE " + table + " ADD COLUMN " + colSQL + ";");
        sqls.addAll(builder.getIndexSQL());

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.ADD_COLUMN;
        op.tabela = table;
        op.colunas = new ArrayList<>(builder.getColumnNames());
        operations.add(op);
    }

    public void dropColumn(String table, String column) {
        sqls.add("ALTER TABLE " + table + " DROP COLUMN " + column + ";");

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.DROP_COLUMN;
        op.tabela = table;
        op.colunas = List.of(column);
        operations.add(op);
    }

    public void renameColumn(String table, String from, String to) {
        sqls.add("ALTER TABLE " + table + " RENAME COLUMN " + from + " TO " + to + ";");

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.RENAME_COLUMN;
        op.tabela = table;
        op.colunaAntiga = from;
        op.colunaNova = to;
        operations.add(op);
    }

    public void renameTable(String from, String to) {
        switch (dialect) {
            case MYSQL -> sqls.add("RENAME TABLE " + from + " TO " + to + ";");
            default    -> sqls.add("ALTER TABLE " + from + " RENAME TO " + to + ";");
        }

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.RENAME_TABLE;
        op.tabela = from;
        op.colunaNova = to;
        operations.add(op);
    }

    public void raw(String sql) {
        sqls.add(sql);

        SchemaOperation op = new SchemaOperation();
        op.tipo = SchemaOperation.Tipo.RAW;
        op.tabela = "raw";
        operations.add(op);
    }

    public List<String> getSQLs() { return sqls; }
    public List<SchemaOperation> getOperations() { return operations; }
}