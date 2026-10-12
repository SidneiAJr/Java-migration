package com.javel;

public class CreateUsersTable extends Migration {

    @Override
    public void up(Schema schema) {
        schema.create("users", table -> {
            table.id();
            table.string("name");
            table.string("email").unique();
            table.timestamps();
        });
    }

    @Override
    public void down(Schema schema) {
        schema.drop("users");
    }

    @Override
    public History history() {
        return History.of(
                "2026-10-12 - Pedro - Criou tabela users"
        );
    }
}