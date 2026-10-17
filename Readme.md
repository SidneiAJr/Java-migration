# javel-migration 🚀

![java](https://img.shields.io/badge/java-%3E%3D17-orange)
![maven](https://img.shields.io/badge/maven-%3E%3D3.8-blue)
![version](https://img.shields.io/badge/version-0.0.1-brightgreen)
![license](https://img.shields.io/badge/license-MIT-green)

> Laravel-style database migrations for Java. No SQL by hand, no YAML, no Flyway — just pure Java classes.

---

## 📋 Requirements

- Java 17+
- Maven 3.8+
- One of: H2, MySQL, PostgreSQL or MariaDB

---

## 📦 Installation

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>io.github.sidneiajr</groupId>
  <artifactId>javel-migration</artifactId>
  <version>0.0.1</version>
</dependency>
```

---

## 🚀 Quick Start

### Step 1 — Create your migration class

Inside `src/main/java/<your-package>/migration/`, create a class that extends `Migration`:

```java
public class CreateUsersTable extends Migration {

    @Override
    public void up(Schema schema) {
        schema.create("users", table -> {
            table.id();
            table.string("name");
            table.string("email").unique();
            table.string("password");
            table.bool("active").defaultValue("true");
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
            "2026-10-16 - test - Created users table"
        );
    }
}
```

- `up()` — defines what to create or alter
- `down()` — defines how to undo it
- `history()` — optional log entry saved to the database

---

### Step 2 — Register the migration

Open your `JavelConfig.java` and register the migration in order:

```java
@Configuration
public class JavelConfig {

    private final DataSource dataSource;

    public JavelConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public MigrationRunner migrationRunner() {
        return new MigrationRunner(dataSource, List.of(
            new CreateUsersTable()
            // add more migrations here in order
        ));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() throws SQLException {
        migrationRunner().run();
    }
}
```

> **Important:** always add new migrations at the end of the list, never change the order of existing ones.

---

### Step 3 — Configure your database

In `src/main/resources/application.properties`:

```properties
# MySQL example
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=none
```

---

### Step 4 — Run via Javel CLI

```bash
# Run pending migrations
javel migrate

# Rollback last migration
javel migrate:rollback
```

Or run directly with Spring Boot — migrations execute automatically on startup via `@EventListener(ApplicationReadyEvent.class)`.

---

## 🧱 Schema — available methods

```java
// Create a table
schema.create("users", table -> { ... });

// Drop a table
schema.drop("users");

// Add a column to existing table
schema.addColumn("users", table -> {
    table.string("phone");
});

// Drop a column
schema.dropColumn("users", "phone");

// Rename a column
schema.renameColumn("users", "phone", "telephone");

// Rename a table
schema.renameTable("users", "accounts");

// Raw SQL (escape hatch)
schema.raw("ALTER TABLE users ADD INDEX idx_email (email)");
```

---

## 🔨 TableBuilder — column types

```java
table.id();                           // BIGINT PRIMARY KEY AUTO_INCREMENT
table.string("name");                 // VARCHAR(255) NOT NULL
table.string("name", 100);            // VARCHAR(100) NOT NULL
table.text("bio");                    // TEXT
table.integer("age");                 // INT NOT NULL
table.bigInteger("views");            // BIGINT NOT NULL
table.bool("active");                 // BOOLEAN NOT NULL DEFAULT FALSE
table.decimal("price", 10, 2);        // DECIMAL(10,2) NOT NULL
table.floatCol("score");              // FLOAT NOT NULL
table.date("birth_date");             // DATE
table.time("start_time");             // TIME
table.datetime("scheduled_at");       // DATETIME
table.json("metadata");               // JSON (JSONB on PostgreSQL)
table.uuid("token");                  // CHAR(36) (UUID on PostgreSQL)
table.enumCol("status", "active", "inactive", "banned"); // ENUM
table.foreign("user_id", "users", "id"); // BIGINT + FOREIGN KEY
table.timestamps();                   // created_at + updated_at
```

### Chainable modifiers

```java
table.string("email").unique();           // adds UNIQUE
table.string("phone").nullable();         // removes NOT NULL
table.integer("age").defaultValue("18");  // adds DEFAULT 18
table.bigInteger("views").unsigned();     // adds UNSIGNED (MySQL only)
table.string("name").comment("Full name"); // adds COMMENT (MySQL only)
table.string("phone").after("email");     // column position (MySQL only)
table.index("email", "name");             // creates a composite index
```

---

## 🗄️ Supported databases

| Database   | Support |
|------------|---------|
| H2         | ✅      |
| MySQL      | ✅      |
| PostgreSQL | ✅      |
| MariaDB    | ✅      |

The driver is detected automatically from your `application.properties` — no extra configuration needed.

---

## 📜 Migration History

Every migration run is automatically logged to two tables created on first run:

**`javel_migrations`** — tracks which migrations already ran:

| Field        | Description                  |
|--------------|------------------------------|
| id           | Auto increment               |
| migration    | Class name                   |
| executed_at  | Timestamp of execution       |

**`javel_migration_history`** — rich audit log:

| Field         | Example                                   |
|---------------|-------------------------------------------|
| migration     | `CreateUsersTable`                        |
| operacao      | `up` / `rollback`                         |
| tabela        | `users`                                   |
| tipo_operacao | `CREATE` / `DROP` / `ADD_COLUMN` / ...   |
| colunas       | `id, name, email, created_at, updated_at` |
| executado_por | `test` (OS username)                      |
| executado_em  | `2026-10-16 20:37:56`                     |
| entry         | `2026-10-16 - test - Created users table` |

> No setup needed — both tables are created automatically on first run.

---

## ▶️ Runner output

```
=== RODANDO MIGRATIONS / RUNNING MIGRATIONS ===

Running / Rodando: CreateUsersTable
   History / Historico:
      2026-10-16 - test - Created users table
   Done / Feito: CreateUsersTable

Running / Rodando: CreatePostsTable
   Done / Feito: CreatePostsTable

=== second run ===

Skipping / Pulando: CreateUsersTable
Skipping / Pulando: CreatePostsTable

=== rollback ===

Rolling back / Revertendo: CreatePostsTable
   Rolled back / Revertido: CreatePostsTable
```

---

## 🤝 Used with Javel CLI

`javel-migration` is the migration engine behind [Javel](https://github.com/SidneiAJr/javel) —
a Laravel-style CLI for Spring Boot projects.

```bash
npm install -g javel

javel new my-project
javel make:migration CreateUsersTable
javel migrate
```

---

## 📄 License

MIT