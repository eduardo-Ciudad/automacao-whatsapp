# Investigação: migrations Flyway não aplicadas

Data da investigação: 24/08/2026

## Resumo executivo

As migrations não chegam ao PostgreSQL porque o projeto usa Spring Boot 4.1.0, mas declara apenas as bibliotecas do Flyway (`flyway-core` e `flyway-database-postgresql`). No Spring Boot 4, a integração e a auto-configuração do Flyway estão em um módulo separado, `spring-boot-flyway`, normalmente incluído por `spring-boot-starter-flyway`.

Esse módulo não está no `pom.xml`. Portanto, os arquivos SQL estão presentes no classpath, mas não existe a auto-configuração que cria e executa o bean do Flyway na inicialização da aplicação.

## Comportamento esperado e observado

- Esperado: ao iniciar a aplicação, o Flyway encontra `classpath:db/migration`, cria/consulta `flyway_schema_history` e aplica as migrations pendentes.
- Observado no projeto: existem migrations `V1` a `V7`, porém a configuração atual não inclui o componente Spring Boot responsável por iniciar o Flyway.

Não foi iniciada uma execução contra o banco, pois a solicitação limita o trabalho à investigação e uma inicialização com a integração correta poderia alterar o banco.

## Evidências

### 1. Versão e dependências declaradas

O `pom.xml` usa Spring Boot `4.1.0` e declara:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Não há dependência de `org.springframework.boot:spring-boot-starter-flyway` nem de `org.springframework.boot:spring-boot-flyway`.

### 2. A auto-configuração está em outro módulo

A inspeção dos artefatos Maven locais da versão 4.1.0 mostrou que:

- `spring-boot-flyway-4.1.0.jar` contém `org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration` e o registro `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`;
- `flyway-core-12.4.0.jar` não contém essa auto-configuração;
- o POM de `spring-boot-starter-flyway:4.1.0` depende explicitamente de `spring-boot-flyway:4.1.0`.

Logo, adicionar somente `flyway-core` disponibiliza a API/engine do Flyway, mas não conecta sua execução ao ciclo de inicialização do Spring Boot 4.

### 3. Os scripts são descobríveis e empacotados

Os arquivos estão no caminho convencional correto:

```text
src/main/resources/db/migration/
  V1__create_contact_table.sql
  V2__create_flow_table.sql
  V3__create_conversation_table.sql
  V4__create_message_table.sql
  V5__create_outbound_message_table.sql
  V6__create_lead_table.sql
  V7__seed_flow_inicial.sql
```

Todos também aparecem em `target/classes/db/migration`. Isso elimina como causa principal:

- caminho incorreto;
- padrão de nome inválido;
- migrations ausentes do artefato compilado.

### 4. Há configuração de DataSource

`application.properties` fornece URL, usuário, senha e driver PostgreSQL. A URL possui o fallback `jdbc:postgresql://localhost:5432/automacao_db`; usuário e senha dependem de `DATABASE_USERNAME` e `DATABASE_PASSWORD`. A configuração de execução da IDE possui essas duas variáveis definidas.

Não foi encontrada configuração `spring.flyway.enabled=false`, alteração de `spring.flyway.locations` ou perfil que explique a desativação.

## Causa raiz

Incompatibilidade entre a forma antiga de habilitar Flyway e a modularização do Spring Boot 4: falta a integração `spring-boot-flyway` no classpath. Sem `FlywayAutoConfiguration`, a aplicação não instancia nem chama o Flyway, embora o engine e os scripts estejam presentes.

## Correção recomendada (não aplicada)

Substituir as dependências Flyway atuais pelo starter do Spring Boot e manter o módulo de banco PostgreSQL:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

O starter já traz `flyway-core` por meio de `spring-boot-flyway`; por isso a declaração direta de `flyway-core` se torna redundante.

## Como validar depois da correção

1. Iniciar a aplicação apontando primeiro para um banco descartável.
2. Confirmar no log a criação/inicialização do Flyway e a descoberta de sete migrations.
3. Consultar `flyway_schema_history` e verificar versões `1` a `7` com `success = true`.
4. Confirmar a existência das seis tabelas de domínio e do registro inicial em `flow`.
5. Em ambiente já populado sem histórico Flyway, avaliar conscientemente `baseline-on-migrate`; não habilitá-lo automaticamente, pois ele pode mascarar divergências de schema.

## Observações secundárias

- A execução local do Maven Wrapper falhou antes de iniciar o Maven por um erro no script PowerShell do wrapper (`Não é possível indexar em uma matriz nula`). Isso afeta a reprodução via terminal neste ambiente, mas não explica a ausência das migrations quando a aplicação é iniciada pela IDE.
- O `V7__seed_flow_inicial.sql` merece uma validação funcional posterior por conter texto acentuado; a exibição no console apresentou caracteres corrompidos. Isso não é a causa da ausência de todas as migrations e pode ser apenas codificação de exibição do terminal.
