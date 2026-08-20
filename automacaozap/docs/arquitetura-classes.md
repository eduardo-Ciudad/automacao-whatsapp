# Arquitetura de classes — CiudadLab WhatsApp Automation

Este documento explica todas as classes/interfaces criadas até o momento no projeto,
organizadas na mesma ordem da estrutura de pacotes: domínio, ports de entrada, ports
de saída, entidades JPA, repositórios Spring Data e adapters de persistência.

O projeto segue **arquitetura hexagonal (ports & adapters)**: o domínio não conhece
nada sobre banco de dados, Spring ou Jakarta Persistence; a camada de aplicação
define contratos (ports); a infraestrutura implementa esses contratos usando
tecnologia concreta (JPA/Hibernate).

---

## 1. `domain.model`

Pacote: `com.eduar.automacaozap.domain.model`

### Por que nenhuma classe aqui tem anotação de framework

Nenhuma classe deste pacote importa `jakarta.persistence.*`, `org.springframework.*`
ou `org.hibernate.*`. Isso é intencional e é a base da arquitetura hexagonal: o
domínio representa as regras e a forma dos dados do negócio, e não pode depender de
como esses dados são persistidos ou expostos. Se amanhã o projeto trocar Postgres por
outro banco, trocar Hibernate por outro ORM, ou trocar Spring Web por outro
framework HTTP, nenhuma classe deste pacote deveria precisar mudar — só os adapters.
As únicas anotações presentes são do Lombok (`@Getter`, `@Builder`,
`@AllArgsConstructor`), que geram código boilerplate em tempo de compilação e não
acoplam a classe a nenhum framework em tempo de execução.

### `Contact`

Representa um contato do WhatsApp (o número que troca mensagens com o sistema).
Campos: `id`, `whatsappNumber` (formato E.164), `name`, `companyId`,
`createdAt`/`updatedAt`.

Está em `domain.model` porque é um conceito de negócio central e independente de
persistência — "um contato existe" é verdade seja qual for o banco de dados.

### `Flow`

Representa um fluxo de automação configurável: nome, versão, definição em JSON
(a árvore de passos), se está ativo, e a empresa dona do fluxo.

Está separado de `FlowStep` porque `Flow` é o **aggregate root** persistido como
linha de tabela, enquanto `FlowStep` é apenas a forma como o conteúdo do campo
`definition` (JSONB) é interpretado em memória — não é uma entidade própria.

### `FlowStep`

`record` de apoio usado para interpretar um nó da árvore de definição de fluxo:
`id`, `type` (`StepType`), `text`, `options` e `nextStepId`.

Está em `domain.model`, e não em `application` ou em uma futura camada de motor de
fluxo (`FlowEngine`), porque é vocabulário do domínio — descreve um conceito de
negócio ("um passo de fluxo"), não uma operação. É um `record` (não uma classe com
builder) porque é um valor imutável, sem identidade própria e sem necessidade de
mutação incremental — nasce completo a partir do JSON.

### `StepType`

Enum dos tipos de passo de um fluxo: `MESSAGE`, `MENU`, `INPUT`, `ACTION`,
`HANDOFF`. Vocabulário puro de domínio, usado por `FlowStep`.

### `Conversation`

Representa uma conversa em andamento (ou encerrada) entre um contato e o sistema:
qual fluxo está sendo executado, em que passo, o contexto acumulado (JSON), o status
e metadados de última mensagem recebida.

Está em `domain.model`, e não em `infrastructure`, porque as regras de quando uma
conversa está `ACTIVE`, `WAITING_HUMAN` ou `CLOSED` são regras de negócio — a forma
como isso é gravado em uma tabela Postgres é um detalhe de infraestrutura que não
deveria vazar para quem raciocina sobre o fluxo da conversa.

### `ConversationStatus`

Enum do status de uma conversa: `ACTIVE`, `WAITING_HUMAN`, `CLOSED`. Os valores
batem exatamente com o `CHECK` constraint da coluna `status` na migration
`V3__create_conversation_table.sql`, para que o mapeamento `@Enumerated(EnumType.STRING)`
na entidade JPA nunca produza um valor rejeitado pelo banco.

### `Message`

Representa uma mensagem trocada dentro de uma conversa (recebida ou enviada),
incluindo o payload bruto recebido da Meta, quando aplicável.

Está em `domain.model` separado de `OutboundMessage` porque são dois conceitos de
negócio diferentes: `Message` é o registro histórico de uma mensagem trocada;
`OutboundMessage` é uma tentativa de envio com controle de status/retentativas. Um
`OutboundMessage` bem-sucedido pode gerar um `Message` de saída, mas são
responsabilidades distintas.

### `Direction`

Enum do sentido de uma mensagem: `IN` (recebida) ou `OUT` (enviada). Usado por
`Message`.

### `OutboundMessage`

Representa uma tentativa de envio de mensagem para o WhatsApp: destinatário,
payload, tipo, status de envio, tentativas e último erro.

Está em `domain.model` porque a lógica de "isso ainda está pendente, falhou, ou foi
enviado" é uma regra de negócio de entrega, independente de qual API (Meta Cloud API
hoje, outra amanhã) é usada para efetivamente enviar — essa parte fica isolada
atrás do port `WhatsAppSenderPort`.

### `OutboundMessageType`

Enum do tipo de mensagem de saída: `TEXT`, `TEMPLATE`, `INTERACTIVE`. Vocabulário de
negócio sobre o que pode ser enviado ao WhatsApp.

### `OutboundStatus`

Enum do status de envio: `PENDING`, `SENT`, `FAILED`. Usado por `OutboundMessage` e
consultado pelo `OutboundMessageJpaRepository.findPendingBatch` para saber quais
linhas ainda precisam ser processadas.

### `Lead`

Representa uma oportunidade comercial identificada durante uma conversa: interesse,
descrição e status no funil (`NEW`, `IN_PROGRESS`, `WON`, `LOST`).

Está em `domain.model` porque a existência de um lead e seu estágio no funil é uma
regra de negócio de vendas, não um detalhe de como a Meta entrega mensagens ou como
o banco armazena isso.

### `LeadStatus`

Enum do estágio do lead: `NEW`, `IN_PROGRESS`, `WON`, `LOST`.

---

## 2. `application.port.in`

Pacote: `com.eduar.automacaozap.application.port.in`

### Diferença entre `port.in` e `port.out`

Um **port.in** é um contrato que a aplicação **oferece** para o mundo externo
chamar — é a porta de entrada do caso de uso. Quem implementa uma interface de
`port.in` é a própria camada `application.service` (ainda não criada); quem
**chama** essa interface é um adapter de entrada, como um controller REST ou um
handler de webhook. O fluxo é: `adapter.in` → `port.in` → `application.service`.

Um **port.out** é o oposto: é um contrato que a aplicação **exige** de uma
dependência externa (banco de dados, API da Meta). Quem implementa `port.out` é um
adapter de saída (ex: `ContactRepositoryAdapter`); quem **chama** essa interface é a
própria `application.service`. O fluxo é: `application.service` → `port.out` →
`adapter.out`.

Essa separação garante que a camada `application` só depende de interfaces que ela
mesma define, nunca de classes concretas de infraestrutura — a dependência sempre
aponta para dentro (regra de dependência da arquitetura hexagonal).

### `ProcessInboundMessageUseCase`

Caso de uso de entrada: processar uma mensagem recebida via webhook do WhatsApp.
Define apenas `void handle(InboundMessageCommand command)`.

Está em `port.in` porque representa uma intenção que vem de fora do sistema ("uma
mensagem chegou, processe-a") — é o ponto de entrada que um futuro
`WebhookController` vai chamar, sem esse controller precisar saber nada sobre
`Conversation`, `Flow` ou regras de roteamento.

### `InboundMessageCommand`

`record` com os dados de uma mensagem recebida: `whatsappNumber`, `metaMessageId`,
`text` e `rawPayload` (`tools.jackson.databind.JsonNode`).

É um `record` porque é um objeto de comando imutável — carrega dados de entrada para
o caso de uso e não tem comportamento próprio. Fica em `port.in` (e não em
`domain.model`) porque não é um conceito de negócio persistente, é a forma do
**pedido** de invocação do caso de uso — se o formato do webhook mudar, é esse
record que muda, não o domínio.

---

## 3. `application.port.out`

Pacote: `com.eduar.automacaozap.application.port.out`

### `ContactRepositoryPort`

Contrato de persistência para `Contact`: buscar por número de WhatsApp e salvar.
`findByWhatsappNumber` existe porque é assim que o sistema identifica um contato ao
receber uma mensagem — o número, não o UUID interno, é a chave de entrada natural.

### `FlowRepositoryPort`

Contrato de leitura para `Flow`: `findActiveById`. Não existe `save` neste port
porque, nesta etapa do projeto, os fluxos são consultados (para execução), não
criados via caso de uso já implementado — o port expõe só o que é necessário para os
consumidores atuais, evitando um contrato inflado com métodos sem chamador.
`findActiveById` (e não um `findById` genérico) existe porque o motor de fluxo nunca
deve executar um fluxo desativado — filtrar por `active = true` é uma regra de
negócio, não um detalhe de índice, por isso está no nome do método do contrato.

### `ConversationRepositoryPort`

Contrato de persistência para `Conversation`: `findByIdForUpdate`,
`findByContactId` e `save`.

`findByIdForUpdate` existe como método **separado** de um eventual `findById` porque
representa uma necessidade transacional diferente: no processamento de uma mensagem
recebida, o sistema precisa ler o estado atual da conversa e trava-lo até decidir e
gravar o próximo passo, para que duas mensagens da mesma conversa chegando quase
simultaneamente (ex: dois webhooks do mesmo número) não pisem uma na outra. Um
`findById` comum (leitura sem lock) serviria para telas de consulta, mas seria
perigoso usá-lo no caminho de escrita — por isso o nome do método já deixa explícito
na assinatura do port que essa chamada trava a linha.

### `MessageRepositoryPort`

Contrato de persistência para `Message`: `existsByMetaMessageId` e `save`.

`existsByMetaMessageId` existe para suportar idempotência: a Meta pode reentregar o
mesmo webhook mais de uma vez, e o sistema precisa conseguir perguntar "eu já
processei essa mensagem?" antes de processá-la de novo. Essa é exatamente a mesma
regra refletida no índice único parcial `idx_message_meta_id_in` da migration V4.

### `OutboundMessageRepositoryPort`

Contrato de persistência para `OutboundMessage`: `save` e `findPendingBatch(int
limit)`.

`findPendingBatch` existe como operação própria (e não um `findAll` filtrado depois
em memória) porque o objetivo é permitir que um worker de envio busque um lote
pequeno e travado de mensagens pendentes direto no banco, sem carregar a tabela
inteira e sem dois workers concorrentes pegarem a mesma mensagem — isso só é possível
expressando a busca como uma consulta com lock desde o port.

### `LeadRepositoryPort`

Contrato de persistência para `Lead`: apenas `save`. Não há métodos de busca porque,
nesta etapa, nada no sistema ainda precisa consultar leads — o port reflete somente o
que a aplicação usa hoje.

### `WhatsAppSenderPort`

Contrato de envio de mensagem para a Meta Cloud API: `String send(OutboundMessage
message)`, retornando o `metaMessageId` da resposta.

Está em `port.out`, e não em `port.in`, porque, do ponto de vista da aplicação,
"enviar uma mensagem pelo WhatsApp" é uma dependência externa que a aplicação
**consome** (assim como o banco de dados) — não é algo que o mundo externo chama
para acionar um caso de uso. Isolar esse contrato aqui é o que permite, no futuro,
trocar a implementação (`WhatsAppCloudApiAdapter`, ainda não criada) sem tocar em
nenhuma regra de negócio.

---

## 4. `infrastructure.adapter.out.persistence.entity`

Pacote: `com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity`

Cada `XxxJpaEntity` é o espelho, em anotações Jakarta Persistence, da classe de
domínio equivalente — mesmo conjunto de campos, mas com o vocabulário de mapeamento
que o Hibernate entende. Diferenças recorrentes em relação ao domínio:

- `@Entity` + `@Table(name = "...")`: liga a classe à tabela física criada nas
  migrations Flyway.
- `@Column(name = "...")` sempre que o nome da coluna (snake_case) difere do nome do
  campo Java (camelCase) — o domínio não precisa saber disso.
- `@Enumerated(EnumType.STRING)`: grava o enum como texto (`'ACTIVE'`, `'PENDING'`
  etc.), batendo com os valores usados nos `CHECK` constraints das migrations. Sem
  essa anotação, o Hibernate gravaria o índice ordinal do enum, que quebraria o
  `CHECK` e seria ilegível no banco.
- `@JdbcTypeCode(SqlTypes.JSON)`: usado nos campos `JsonNode` (`definition`,
  `context`, `payload`, `raw_payload`) para mapear colunas `JSONB` sem precisar de
  biblioteca externa — recurso nativo do Hibernate 6+.
- **Ausência de `@ManyToOne`/`@OneToMany`/`@OneToOne`**: nenhuma entidade JPA
  referencia outra entidade JPA como objeto. Toda referência entre agregados é um
  campo simples de FK (`UUID`/`Long`). Isso é intencional — ver seção "Decisões
  arquiteturais recorrentes".
- `@Id` sem `@GeneratedValue` nos IDs `UUID` (Contact, Flow, Conversation, Lead):
  o valor default é gerado pelo banco (`gen_random_uuid()`), conforme definido nas
  migrations. Já `Message` e `OutboundMessage` usam
  `@GeneratedValue(strategy = GenerationType.IDENTITY)` porque seus IDs são
  `BIGSERIAL`.

### `ContactJpaEntity`

Espelho de `Contact`. Sem diferenças estruturais relevantes além das citadas acima —
todos os campos são tipos simples.

### `FlowJpaEntity`

Espelho de `Flow`. `definition` usa `@JdbcTypeCode(SqlTypes.JSON)` para mapear a
coluna `JSONB` que guarda a árvore de passos do fluxo.

### `ConversationJpaEntity`

Espelho de `Conversation`. `status` usa `@Enumerated(EnumType.STRING)` mapeado para
`ConversationStatus` (reaproveitado do domínio — não existe um enum JPA duplicado).
`context` usa `@JdbcTypeCode(SqlTypes.JSON)`. `contactId` e `flowId` são campos `UUID`
simples, não relacionamentos — mesmo sendo referências para `contact` e `flow`, que
existem como tabelas com FK no banco (a FK garante integridade referencial no nível
do banco; o Hibernate não precisa saber navegar entre esses agregados em memória).

### `MessageJpaEntity`

Espelho de `Message`. `direction` usa `@Enumerated(EnumType.STRING)` mapeado para
`Direction`. `rawPayload` usa `@JdbcTypeCode(SqlTypes.JSON)`. `id` usa
`@GeneratedValue(strategy = GenerationType.IDENTITY)` porque a coluna é
`BIGSERIAL`.

### `OutboundMessageJpaEntity`

Espelho de `OutboundMessage`. `type` (`OutboundMessageType`) e `status`
(`OutboundStatus`) usam `@Enumerated(EnumType.STRING)`. `payload` usa
`@JdbcTypeCode(SqlTypes.JSON)`. Assim como em `MessageJpaEntity`, o `id` é
`IDENTITY`, e `messageId` é um `Long` simples (FK para `message`, sem
relacionamento JPA).

### `LeadJpaEntity`

Espelho de `Lead`. `status` usa `@Enumerated(EnumType.STRING)` mapeado para
`LeadStatus`. `contactId` e `conversationId` são campos `UUID` simples, mesma lógica
de FK-sem-relacionamento descrita acima.

---

## 5. `infrastructure.adapter.out.persistence.repository`

Pacote: `com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository`

Cada `XxxJpaRepository` é uma interface Spring Data JPA (`extends JpaRepository<...>`)
que expõe as consultas necessárias para os respectivos `RepositoryAdapter`
implementarem os ports. Essas interfaces só existem na camada de infraestrutura —
nunca são referenciadas por `application` ou `domain`, o que só é possível porque os
adapters fazem a ponte.

### `ContactJpaRepository`

`extends JpaRepository<ContactJpaEntity, UUID>` com `findByWhatsappNumber`, uma
query derivada simples que resolve sozinha pelo nome do método (sem `@Query`).

### `FlowJpaRepository`

`extends JpaRepository<FlowJpaEntity, UUID>` com `findByIdAndActiveTrue`, que
implementa o `findActiveById` do port filtrando por `active = true` diretamente na
consulta — o filtro de negócio "só fluxos ativos" vira parte da query, não um filtro
aplicado depois em memória.

### `ConversationJpaRepository`

`extends JpaRepository<ConversationJpaEntity, UUID>` com `findByContactId` (query
derivada) e `findByIdForUpdate`.

`findByIdForUpdate` usa `@Lock(LockModeType.PESSIMISTIC_WRITE)` junto de uma
`@Query` explícita (`select c from ConversationJpaEntity c where c.id = :id`) em vez
de depender de uma convenção de nome. Isso gera um `SELECT ... FOR UPDATE` no
Postgres: a linha da conversa fica travada até o fim da transação que fez a leitura,
impedindo que outra transação concorrente leia e sobrescreva o mesmo `currentStepId`/
`context` ao mesmo tempo. É um método **deliberadamente separado** de uma busca comum
por ID porque travar uma linha tem custo (outras transações que tentem escrever na
mesma conversa ficam bloqueadas até o commit) — usar isso indiscriminadamente em
qualquer leitura de conversa seria um problema de concorrência desnecessário. Ele
existe só para o hot path de processamento de mensagem, onde ler-decidir-escrever
precisa ser atômico.

### `MessageJpaRepository`

`extends JpaRepository<MessageJpaEntity, Long>` com `existsByMetaMessageId`, query
derivada que resolve pelo nome do método — usada para a checagem de idempotência
antes de gravar uma mensagem recebida.

### `OutboundMessageJpaRepository`

`extends JpaRepository<OutboundMessageJpaEntity, Long>` com `findPendingBatch(Pageable
pageable)`.

Essa consulta combina três elementos:

1. `@Query("select o from OutboundMessageJpaEntity o where o.status = 'PENDING' order by o.createdAt asc")` — filtra só o que está pendente, na ordem de chegada.
2. `@Lock(LockModeType.PESSIMISTIC_WRITE)` — trava as linhas retornadas.
3. `@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))` — instrui o Hibernate a usar `SKIP LOCKED` em vez de esperar (`-2` é o valor convencionado pela JPA/Hibernate para "pular linhas já travadas por outra transação").

O objetivo é permitir múltiplos workers de envio rodando em paralelo: cada um chama
`findPendingBatch` e recebe um lote de mensagens pendentes que **nenhum outro worker
está processando no momento** — sem essa combinação, um segundo worker ficaria
bloqueado esperando o primeiro liberar as linhas, ou os dois processariam a mesma
mensagem. O tamanho do lote é controlado pelo `Pageable` (construído como
`PageRequest.of(0, limit)` no adapter), não por `LIMIT` manual em SQL nativo, para
manter a consulta em JPQL portável.

### `LeadJpaRepository`

`extends JpaRepository<LeadJpaEntity, UUID>`, sem métodos adicionais — o port
`LeadRepositoryPort` só precisa de `save`, que já vem de `JpaRepository`.

---

## 6. `infrastructure.adapter.out.persistence`

Pacote: `com.eduar.automacaozap.infrastructure.adapter.out.persistence`

Cada `XxxRepositoryAdapter` implementa o respectivo `XxxRepositoryPort`, injeta o
`XxxJpaRepository` correspondente e faz a tradução entre a entidade JPA (que só a
infraestrutura conhece) e a classe de domínio (que é tudo que a camada `application`
enxerga). Nenhum destes adapters tem `@Transactional` — essa anotação é
responsabilidade da futura camada `application.service`, que é quem decide os
limites de uma transação de negócio (por exemplo, ler a conversa com lock, decidir o
próximo passo e salvar tudo precisa estar na mesma transação, e isso só faz sentido
orquestrado pelo service, não dentro de um único método de adapter).

Todos seguem o mesmo formato: dois métodos privados, `toDomain(XxxJpaEntity)` e
`toEntity(Xxx)`, com conversão campo a campo explícita. A conversão é manual (sem
MapStruct ou outra lib de mapeamento) por duas razões práticas: (1) o número de
campos é pequeno o suficiente para que uma lib de mapeamento não reduza
significativamente o código, e (2) uma conversão explícita deixa visível, no próprio
código-fonte, exatamente quais campos existem dos dois lados — o que importa
especialmente aqui, já que domínio e entidade JPA são propositalmente classes
desacopladas (mesmo tendo os mesmos campos hoje, podem divergir no futuro).

### `ContactRepositoryAdapter`

Implementa `ContactRepositoryPort`. `findByWhatsappNumber` delega para a query
derivada do `ContactJpaRepository` e mapeia o `Optional<ContactJpaEntity>` para
`Optional<Contact>`. `save` converte domínio → entidade, persiste, e converte o
resultado salvo de volta para domínio (garantindo que o `Contact` retornado reflita
qualquer valor gerado ou ajustado pelo banco).

### `FlowRepositoryAdapter`

Implementa `FlowRepositoryPort`. Só tem `toDomain` (não tem `toEntity`) porque o
port não expõe `save` — não existe motivo para o adapter ter um método de conversão
que nenhum caso de uso chamaria.

### `ConversationRepositoryAdapter`

Implementa `ConversationRepositoryPort`. `findByIdForUpdate` e `findByContactId`
delegam diretamente para os métodos equivalentes do `ConversationJpaRepository` —
o adapter não decide nada sobre lock, apenas repassa a chamada; a decisão de travar
a linha já está encapsulada na assinatura/anotações do repositório JPA.

### `MessageRepositoryAdapter`

Implementa `MessageRepositoryPort`. `existsByMetaMessageId` é um repasse direto
(`boolean` não precisa de conversão). `save` segue o padrão
converter→persistir→reconverter.

### `OutboundMessageRepositoryAdapter`

Implementa `OutboundMessageRepositoryPort`. `findPendingBatch(int limit)` é onde o
`int limit` do port vira um `PageRequest.of(0, limit)` para satisfazer a assinatura
`findPendingBatch(Pageable pageable)` do `OutboundMessageJpaRepository` — essa
tradução de "quantidade simples" para "paginação" fica isolada aqui, para que o port
(e quem o consome) continue falando a linguagem simples de "quero até N mensagens",
sem precisar saber que por baixo isso é implementado com `Pageable`.

### `LeadRepositoryAdapter`

Implementa `LeadRepositoryPort`. Único método, `save`, segue o mesmo padrão
converter→persistir→reconverter dos demais adapters.

---

## Decisões arquiteturais recorrentes

- **Referências entre agregados são sempre por ID, nunca por objeto.** Nenhuma
  classe de domínio ou entidade JPA tem um campo do tipo de outro agregado
  (`Contact contact`, por exemplo) — só `UUID`/`Long` (`contactId`, `flowId`,
  `messageId` etc.). Isso vale tanto no domínio (onde simplesmente não existe
  necessidade de navegação em memória entre agregados) quanto na JPA (onde é reforçado
  pela ausência proposital de `@ManyToOne`/`@OneToOne`/`@OneToMany`). Cada tabela —
  `contact`, `flow`, `conversation`, `message`, `outbound_message`, `lead` — é
  tratada como fronteira de consistência independente, carregada e salva sozinha,
  sem que o Hibernate precise (ou tenha permissão de) carregar grafos de objetos
  relacionados via lazy/eager loading.

- **Nenhuma classe de domínio tem anotação de framework.** `domain.model` é Java
  puro (mais Lombok para reduzir boilerplate de getters/builders), sem
  `jakarta.persistence`, sem `org.springframework`, sem `org.hibernate`. Isso é o
  que permite que `application` e `domain` sejam testados e raciocinados sem
  precisar de um contexto Spring ou de um banco de dados.

- **`tools.jackson.databind.JsonNode`, não `com.fasterxml.jackson.databind.JsonNode`.**
  O projeto está em Spring Boot 4.1 com Jackson 3, cuja classe `JsonNode` vive em
  `tools.jackson.databind` — o pacote `com.fasterxml.jackson.databind` (Jackson 2)
  sequer está no classpath. O Hibernate 7.4.1 detecta isso automaticamente e usa
  `Jackson3JsonFormatMapper` para mapear `@JdbcTypeCode(SqlTypes.JSON)`, sem
  configuração extra.

- **Enums de domínio são reaproveitados como enums JPA.** `ConversationStatus`,
  `Direction`, `OutboundMessageType`, `OutboundStatus` e `LeadStatus` não têm
  versões duplicadas — a mesma classe de `domain.model` é usada com
  `@Enumerated(EnumType.STRING)` nas entidades JPA. Os valores textuais batem
  exatamente com os `CHECK` constraints definidos nas migrations.

- **Nenhum adapter de persistência tem `@Transactional`.** A decisão de onde uma
  transação começa e termina pertence à camada `application.service` (ainda não
  criada nesta etapa), que orquestra múltiplas chamadas a ports.out dentro de uma
  única unidade de trabalho. Um adapter individual não tem visão do caso de uso
  completo para tomar essa decisão corretamente.

- **Locks pessimistas existem como métodos separados e nomeados, nunca como padrão
  implícito.** `findByIdForUpdate` (Conversation) e `findPendingBatch` (OutboundMessage,
  com `SKIP LOCKED`) são métodos à parte de qualquer busca "normal" — o nome do
  método já comunica, na assinatura do port e do repositório, que aquela chamada tem
  efeito colateral de concorrência (trava linha / pula linhas travadas). Isso evita
  que alguém use por engano uma consulta com lock onde só uma leitura simples era
  necessária, ou vice-versa.

- **Conversão domínio ↔ entidade JPA é sempre manual, via `toDomain`/`toEntity`
  privados em cada `RepositoryAdapter`.** Nenhuma lib de mapeamento (MapStruct ou
  similar) é usada — a tradução explícita, campo a campo, é o único lugar do
  código onde as duas representações (domínio e persistência) se encontram, e isso é
  mantido visível e sob controle direto do adapter.

- **Ports expõem só os métodos que a aplicação efetivamente usa hoje**, não um CRUD
  genérico. `FlowRepositoryPort` não tem `save`, `LeadRepositoryPort` não tem
  método de busca — os contratos crescem sob demanda, evitando abstrações
  especulativas sem chamador real.
