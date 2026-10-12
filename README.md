# javel-migration 🚀

## O problema que isso resolve

No ecossistema Java, migrations sempre foram dolorosas:

- **Flyway** → você escreve `.sql` na mão
- **Liquibase** → YAML verboso e cheio de boilerplate
- **ddl-auto=update** → funciona em dev, perigoso em produção

O `javel-migration` traz a experiência do Laravel para o Java. Você escreve uma classe com `up()` e `down()`, e o runner cuida do resto — sem SQL na mão, sem arquivo externo, tudo em Java puro.

## Instalação

Adicione no seu `pom.xml`:

```xml
<dependency>
  <groupId>com.javel</groupId>
  <artifactId>javel-migration</artifactId>
  <version>0.0.1</version>
</dependency>
```

## Criando uma migration

Cada migration é uma classe Java que estende `Migration`:

```java
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
            "2026-10-12 - Testador - Criou tabela users",
            "2026-10-13 - Testador - Adicionou campo email"
        );
    }
}
```

| Método | Obrigatório | O que faz |
| --- | --- | --- |
| `up()` | ✅ Sim | Define o que criar/alterar |
| `down()` | ✅ Sim | Define como desfazer |
| `history()` | ❌ Opcional | Log de alterações da migration |

## Schema — métodos disponíveis

```java
// Criar tabela
schema.create("users", table -> { ... });

// Dropar tabela
schema.drop("users");

// Adicionar coluna
schema.addColumn("users", table -> {
    table.string("phone");
});

// Remover coluna
schema.dropColumn("users", "phone");

// Renomear coluna
schema.renameColumn("users", "phone", "telephone");
```

## TableBuilder — tipos de coluna

```java
table.id();                           // BIGINT PRIMARY KEY AUTO_INCREMENT
table.string("name");                 // VARCHAR(255) NOT NULL
table.text("bio");                    // TEXT
table.integer("age");                // INT NOT NULL
table.bigInteger("views");           // BIGINT NOT NULL
table.bool("active");                // BOOLEAN DEFAULT FALSE
table.decimal("price", 10, 2);       // DECIMAL(10,2) NOT NULL
table.timestamps();                   // created_at + updated_at
```

**Modificadores encadeáveis:**

```java
table.string("email").unique();          // UNIQUE
table.string("phone").nullable();        // remove NOT NULL
table.integer("age").defaultValue("18"); // DEFAULT 18
```

## Registrando e rodando

No Spring Boot, configure uma vez e esqueça:

```java
@Configuration
public class JavelConfig {

    @Bean
    public MigrationRunner migrationRunner(DataSource dataSource) {
        return new MigrationRunner(dataSource, List.of(
            new CreateUsersTable(),
            new CreatePostsTable()
            // adicione novas migrations aqui, em ordem
        ));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() throws SQLException {
        migrationRunner(null).run();
    }
}
```

Desative o `ddl-auto` do Hibernate no `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=none
```

> ⚠️ Com o javel-migration no controle, o Hibernate só faz queries — quem cuida da estrutura do banco é o runner.

## Comportamento do runner


O `MigrationRunner` é inteligente — não roda o que já rodou:

```
=== RODANDO MIGRATIONS ===
▶  Running CreateUsersTable
   📋 History:
      2026-10-12 - Pedro - Criou tabela users
   ✔ Done

=== RODANDO DE NOVO (deve pular) ===
⏭  Skipping CreateUsersTable

=== ROLLBACK ===
▶  Rolling back CreateUsersTable
   ✔ Rolled back
```

O runner cria automaticamente a tabela `javel_migrations` no banco para controlar o histórico de execução:

```sql
CREATE TABLE IF NOT EXISTS javel_migrations (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  migration    VARCHAR(255) NOT NULL,
  executed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Você não precisa criar essa tabela — ela é gerada automaticamente na primeira execução.

## Bancos suportados

| Banco | Suporte |
| --- | --- |
| H2 (in-memory) | ✅ |
| MySQL | ✅ |
| PostgreSQL | ✅ |
| MariaDB | ✅ |

## Filosofia


O `javel-migration` nasceu de uma pergunta simples:

> *Por que em Java eu preciso escrever SQL na mão para criar uma tabela, se o Laravel faz isso com uma classe?*

A resposta é: não precisa mais.

Sem Flyway, sem Liquibase, sem YAML, sem `.sql` na mão — só Java puro do jeito que deveria ser desde o início.

---

**Licença:** MIT
