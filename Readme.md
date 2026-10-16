# javel-migration 🚀

![license](https://img.shields.io/github/license/seu-usuario/javel-migration)
![java](https://img.shields.io/badge/java-%3E%3D17-orange)
![maven](https://img.shields.io/badge/maven-%3E%3D3.8-blue)
![version](https://img.shields.io/badge/version-0.0.1-brightgreen)

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
  <groupId>com.javel</groupId>
  <artifactId>javel-migration</artifactId>
  <version>0.0.1</version>
</dependency>
```

> **Note:** `javel-migration` is not yet on Maven Central.
> Install it locally first:
> ```bash
> git clone https://github.com/seu-usuario/javel-migration
> cd javel-migration
> mvn install
> ```

---

## 🚀 Quick Start

### 1. Create a migration class

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
            "2026-10-16 - teste - Created users table"
        );
    }
}
```

### 2. Register the migration

In your `JavelConfig.java`:

```java
@Bean
public MigrationRunner migrationRunner(DataSource dataSource) {
    return new MigrationRunner(dataSource, List.of(
        new CreateUsersTable()
        // add more migrations here in order
    ));
}
```

### 3. Run

```bash
# via Javel CLI
javel migrate

# rollback last migration
javel migrate:rollback
```

---

## 🧱 Schema — available methods

```java
// Create table
schema.create("users", table -> { ... });

// Drop table
schema.drop("users");

// Add column
schema.addColumn("users", table -> {
    table.string("phone");
});

// Drop column
schema.dropColumn("users", "phone");

// Rename column
schema.renameColumn("users", "phone", "telephone");
```

---

## 🔨 TableBuilder — column types

```java
table.id();                          // BIGINT PRIMARY KEY AUTO_INCREMENT
table.string("name");                // VARCHAR(255) NOT NULL
table.text("bio");                   // TEXT
table.integer("age");                // INT NOT NULL
table.bigInteger("views");           // BIGINT NOT NULL
table.bool("active");                // BOOLEAN NOT NULL DEFAULT FALSE
table.decimal("price", 10, 2);       // DECIMAL(10,2) NOT NULL
table.timestamps();                  // created_at + updated_at
```

### Chainable modifiers

```java
table.string("email").unique();          // UNIQUE
table.string("phone").nullable();        // removes NOT NULL
table.integer("age").defaultValue("18"); // DEFAULT 18
```

---

## 🗄️ Supported databases

| Database   | Support |
|------------|---------|
| H2         | ✅      |
| MySQL      | ✅      |
| PostgreSQL | ✅      |
| MariaDB    | ✅      |

The driver is detected automatically from your `application.properties`.

---

## 📜 Migration History

Every migration run is logged to the `javel_migration_history` table automatically:

| Field         | Example                                   |
|---------------|-------------------------------------------|
| migration     | `CreateUsersTable`                        |
| operacao      | `up` / `rollback`                         |
| tabela        | `users`                                   |
| tipo_operacao | `CREATE` / `DROP` / `ADD_COLUMN` / ...   |
| colunas       | `id, name, email, created_at, updated_at` |
| executado_por | `teste` (OS user)                         |
| executado_em  | `2026-10-16 20:37:56`                     |
| entry         | `2026-10-16 - teste - Created users`      |

No setup needed — both `javel_migrations` and `javel_migration_history` are created automatically on first run.

---

## ▶️ Runner output

```
=== RUNNING MIGRATIONS ===

▶  Running CreateUsersTable
   📋 History:
      2026-10-16 - teste - Created users table
   ✔ Done

=== RUNNING AGAIN (should skip) ===
⏭  Skipping CreateUsersTable

=== ROLLBACK ===
▶  Rolling back CreateUsersTable
   ✔ Rolled back
```

---

## 🤝 Used with Javel CLI

`javel-migration` is the migration engine behind [Javel](https://github.com/seu-usuario/javel) —
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