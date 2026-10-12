package com.javel;

public class CreatePostsTable extends Migration {

    @Override
    public void up(Schema schema) {
        schema.create("posts", table -> {
            table.id();
            table.string("title");
            table.text("body").nullable();
            table.bigInteger("user_id");
            table.timestamps();
        });
    }

    @Override
    public void down(Schema schema) {
        schema.drop("posts");
    }

    @Override
    public History history() {
        return History.of(
                "2026-10-12 - Pedro - Criou tabela posts"
        );
    }
}