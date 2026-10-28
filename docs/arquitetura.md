# Arquitetura do SysAgropec

Monólito modular organizado por capacidade de negócio. Cada módulo segue a arquitetura hexagonal.
As regras abaixo são verificadas por testes ArchUnit
([`ArquiteturaHexagonalTest`](../backend/src/test/java/br/com/sysagropec/arquitetura/ArquiteturaHexagonalTest.java)):
uma violação quebra o build.

## Módulos

| Módulo | Responsabilidade | Fase |
|---|---|---|
| `identidade` | Organização, usuários, perfis, acesso por propriedade, sessão, senha, contexto de execução | 1 |
| `propriedades` | Propriedades e instalações (curral, piquete, pasto) com capacidade | 1 |
| `rebanho` | Animais, identificações, lotes de manejo, localização física, movimentações, recebimento | 1 |
| `pesagem` | Pesagens individuais: medido ou estimado, método, cancelamento auditável | 1 |
| `tarefas` | Tarefas com checklist, estados, autoria e vínculo com o registro técnico produzido | 1 |
| `consultas` | Modelos de leitura entre módulos: linha do tempo do animal e dashboard | 1 |
| `shared` | Apenas conceitos estáveis: exceções, `ContextoExecucao`, perfis e permissões, portas de relógio, auditoria, idempotência e transação | 1 |
| `demo` | Carga de dados fictícios (perfil `demo`), usando os casos de uso reais | 1 |

Próximas fases (ainda sem código): `confinamento` (ciclos), `nutricao`, `sanidade`, `estoque`, `custos`,
`reproducao` (com maternidade), `genealogia`, `fabrica`, `financeiro`, `patrimonio`, `monitoramento`.

## Camadas e dependências permitidas

```
br.com.sysagropec.<modulo>
├── domain.model / domain.service   Java puro: entidades, invariantes, políticas
├── application.port.in             Casos de uso, comandos e resultados (contrato público do módulo)
├── application.port.out            O que o núcleo precisa do mundo externo
├── application.usecase             Implementação dos casos de uso
├── adapter.in.web | .startup       REST, inicialização
├── adapter.out.persistence         JPA/JDBC (entidades JPA não são públicas)
├── adapter.out.<integração>        Chamadas a outros módulos e serviços (e-mail, hash)
└── configuration                   Composição (@Bean) e fronteira transacional
```

Regras verificadas:

- O domínio depende só de `java..` e de `shared.domain`.
- A aplicação não depende de Spring, JPA, Servlet, Hibernate, adaptadores ou configuração.
- As portas não estendem `Repository` e não expõem tipos do Spring ou da JPA.
- Entidades `@Entity` ficam em `adapter.out.persistence` e não são públicas.
- Adaptadores de entrada não usam adaptadores de saída. Controllers usam apenas portas de entrada.
- Um módulo só acessa outro pelas portas de entrada dele (`application.port.in`).
- O núcleo de um módulo não conhece outros módulos. A integração passa por uma porta de saída própria, implementada por um adaptador que chama a porta de entrada do outro módulo.
- `@Transactional` não aparece no domínio nem na aplicação.
- `shared` não conhece nenhum módulo.

## Composição e transações

- Os casos de uso são construídos com `new` nas classes `*Configuration`. Apenas as portas de entrada viram beans.
- [`Transacional`](../backend/src/main/java/br/com/sysagropec/shared/configuration/Transacional.java) é um proxy que transforma cada chamada de porta em uma unidade transacional, de escrita ou só de leitura.
- Portas internas entre módulos (por exemplo, `PropriedadesInternasQuery`, `TarefasResultadoUseCase`) participam da transação de quem chama. Assim, a pesagem e o vínculo com a tarefa confirmam ou revertem juntos.
- Identidade usa `TransacaoPort` explicitamente para:
  - auditar uma falha de login, mesmo quando a operação termina em exceção;
  - enviar e-mails só depois do commit, sem segurar a transação durante a chamada SMTP.

## Integridade e concorrência

| Risco | Proteção |
|---|---|
| Animal em dois lotes ou dois locais ao mesmo tempo | Índices únicos parciais (`... where fim is null`) em `animal_lote_vinculo` e `animal_localizacao` |
| Brinco duplicado entre animais ativos | Verificação no caso de uso e índice único parcial em `(organizacao_id, tipo, lower(valor))` |
| Lotação excedida por requisições simultâneas | `SELECT … FOR UPDATE` na instalação de destino antes de contar a ocupação |
| Mesmo animal movido em duas operações | Versão otimista (`@Version`) no animal, sempre incrementada |
| Entrada no lote durante o encerramento | Bloqueio do lote nas duas operações. A ordem de bloqueio é fixa (lote, depois instalação) para evitar deadlock |
| Reenvio da mesma requisição | `Idempotency-Key` no recebimento e na pesagem, com a tabela `idempotencia` (chave primária por organização) |
| Lançamento retroativo que reescreveria o histórico | Rejeitado com 409: a movimentação não pode ser anterior à última alocação do animal |
| Referência entre organizações | Chaves estrangeiras compostas `(organizacao_id, id)` em todas as relações |
| Registro técnico usado por duas tarefas | `unique (tipo, resultado_id)` em `tarefa_resultado` |
| Duas pesagens de entrada | Índice único parcial por animal em `pesagem`, considerando só as não canceladas |

As chaves estrangeiras entre recebimento, animal, movimentação e vínculos são `DEFERRABLE INITIALLY DEFERRED`:
esses registros são gravados juntos e verificados no commit.

## Segurança

- **Sessão:**
  - Sessão no servidor, com cookie `HttpOnly` e `SameSite=Lax` (e `Secure` em produção).
  - Persistida no PostgreSQL com Spring Session JDBC, então sobrevive a reinícios.
  - Expira após 8 horas sem uso.
  - Logout invalida a sessão.
  - O identificador da sessão muda no login, contra fixação de sessão.
- **Permissões a cada requisição:**
  - O `ContextoExecucao` é recalculado a partir do banco em toda requisição.
  - Desativação de usuário e revogação de acesso valem na requisição seguinte, sem esperar a sessão expirar.
- **CSRF:** padrão SPA. O backend emite o cookie `XSRF-TOKEN` e o cliente o devolve no cabeçalho `X-XSRF-TOKEN`.
- **Senhas e tokens:**
  - Hash bcrypt, via `DelegatingPasswordEncoder`, o que permite migrar de algoritmo.
  - Tokens de redefinição (30 min) e de convite (72 h) são de uso único. Só o hash SHA-256 é gravado.
- **Autorização:**
  - Feita nos casos de uso (`ctx.exigir(...)`), não só nos controllers.
  - Recurso de outra organização, ou de propriedade sem acesso, responde **404**, sem revelar que existe.
  - Acesso existente mas sem a operação responde **403**.
- **Auditoria:** a tabela `auditoria_evento` registra:
  - login, falha de login e definição de senha;
  - usuários e acessos;
  - propriedades;
  - recebimentos e movimentações;
  - formação e encerramento de lotes;
  - cancelamentos e impedimentos.

## Modelo de dados (Fase 1)

| Área | Tabelas |
|---|---|
| Identidade | `organizacao`, `usuario`, `acesso_propriedade`, `token_senha`, `spring_session*` |
| Estrutura | `propriedade`, `instalacao` |
| Rebanho | `animal`, `animal_identificacao`, `lote`, `animal_lote_vinculo`, `animal_localizacao`, `movimentacao`, `movimentacao_item`, `recebimento`, `sequencia_animal` |
| Produção | `pesagem` |
| Rotina | `tarefa`, `tarefa_checklist_item`, `tarefa_resultado` |
| Transversal | `auditoria_evento`, `idempotencia` |

Todo evento registra dois horários: `ocorrido_em` (quando aconteceu) e `registrado_em` (quando foi lançado).

Relacionamentos previstos (ainda sem tabela):

- `ciclo_confinamento` (lote × instalação × período);
- `dieta_versao` → `lote_dieta` → `trato`;
- `procedimento_sanitario` → `restricao_carencia`;
- `movimento_estoque` (razão de movimentações);
- `lancamento_custo`, com `origem_tipo` e `origem_id` únicos, para evitar dupla contagem;
- `genealogia_vinculo`, com verificação de ciclos no domínio.

## API

Documentação OpenAPI em `/swagger-ui.html` quando `SYSAGROPEC_OPENAPI=true`, que é o padrão local.

Erros no formato RFC 9457 (`application/problem+json`):

| Código | Quando |
|---|---|
| 400 | Formato inválido, com o campo `campos` |
| 401 | Sem sessão |
| 403 | Perfil sem a operação, ou CSRF ausente |
| 404 | Inexistente ou fora do escopo do usuário |
| 409 | Duplicidade, versão desatualizada ou lançamento retroativo |
| 422 | Regra de negócio violada |
