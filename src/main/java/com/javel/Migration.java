package com.javel;

public abstract class Migration {

    public abstract void up(Schema schema);

    public abstract void down(Schema schema);

    public History history() {
        return History.of(); // opcional — padrão vazio
    }

    public String version() {
        // pega o nome da classe ex: V1__CreateUsersTable
        return this.getClass().getSimpleName();
    }
}