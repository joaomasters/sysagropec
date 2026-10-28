# SysAgropec

Gestão agropecuária com foco inicial em confinamento bovino.

Esta é a **Fase 1**: o primeiro fluxo operacional completo, com frontend, API e banco integrados. O fluxo:

1. cadastrar propriedade e currais;
2. receber animais;
3. formar lote e movimentar animais;
4. registrar pesagens (medidas ou estimadas);
5. criar e executar tarefas com checklist ("Rotinas de hoje");
6. consultar a ficha e a linha do tempo de cada animal;
7. acompanhar o dashboard calculado só com dados registrados.

Também fazem parte desta fase:

- organizações, usuários, convites, redefinição de senha e permissões por propriedade;
- auditoria das ações críticas.

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Security, Spring Session JDBC, JPA/Hibernate (só no adaptador de persistência), Flyway, PostgreSQL 17 |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query |
| Testes | JUnit 5, ArchUnit, Testcontainers, Playwright |

Documentação complementar:

- [docs/arquitetura.md](docs/arquitetura.md): módulos, regras hexagonais, transações, concorrência, segurança e modelo de dados.
- [docs/decisoes-e-premissas.md](docs/decisoes-e-premissas.md): decisões tomadas, premissas e o que validar com o produtor.
- [docs/implantacao.md](docs/implantacao.md): produção, HTTPS, backup e restauração.

## Requisitos

- JDK 21. O Maven não precisa estar instalado: use `./mvnw`.
- Node.js 22 ou mais recente (testado com 24).
- Docker com Compose: PostgreSQL e Mailpit locais, e Testcontainers nos testes de integração.

## Executar localmente

### Opção A — tudo em contêineres

```bash
cp .env.example .env          # defina SYSAGROPEC_DB_SENHA e, para o 1º acesso, ADMIN_EMAIL/ADMIN_SENHA
docker compose up --build
```

| Endereço | Para quê |
|---|---|
| http://localhost:8081 | Aplicação |
| http://localhost:8025 | Caixa de e-mail local (Mailpit), com os links de convite e de redefinição de senha |

### Opção B — desenvolvimento (recarga automática)

```bash
cp .env.example .env
docker compose up -d postgres mailpit

# Backend (porta 8080). No Windows: mvnw.cmd
cd backend
SYSAGROPEC_DB_SENHA=... SYSAGROPEC_ADMIN_EMAIL=admin@exemplo.com.br SYSAGROPEC_ADMIN_SENHA=uma-senha-longa \
  ./mvnw spring-boot:run

# Frontend (porta 5173, com proxy de /api para o backend)
cd frontend
npm install
npm run dev
```

No PowerShell, defina as variáveis antes de rodar: `$env:SYSAGROPEC_DB_SENHA='...'`.

### Primeiro acesso

Com o banco vazio, o backend cria a organização e o administrador a partir de:

- `SYSAGROPEC_ORGANIZACAO`;
- `SYSAGROPEC_ADMIN_EMAIL`;
- `SYSAGROPEC_ADMIN_SENHA` (mínimo de 10 caracteres).

Ao entrar, o administrador cadastra a primeira propriedade. Outros usuários são convidados em **Usuários**: recebem um link para definir a senha.

### Dados de demonstração

O perfil `demo` (`SPRING_PROFILES_ACTIVE=demo`), **somente em banco vazio**, cria:

- uma organização e uma fazenda identificadas como fictícias;
- 3 currais, 12 animais, um lote, pesagens de entrada e tarefas.

Login: `admin@demo.sysagropec.local`, senha `demonstracao-2026`.

Não use esse perfil com dados reais.

## Testes

| Comando | O que roda | Requer |
|---|---|---|
| `cd backend && ./mvnw test` | Unitários de domínio e de casos de uso (sem Spring e sem banco) e regras de arquitetura (ArchUnit) | JDK |
| `cd backend && ./mvnw verify` | Os anteriores, mais integração com PostgreSQL real (Testcontainers) | Docker |
| `cd frontend && npm run verificar` | Checagem de tipos, lint e build | Node |
| `cd frontend && npm run e2e` | Fluxo completo pela interface (Playwright) | Aplicação rodando |

Testes de integração (`*IT`):

| Teste | O que verifica |
|---|---|
| `FluxoPrimeiraEntregaIT` | Critério de aceite completo pela API, idempotência, retroatividade e duplicidade |
| `IsolamentoAcessoIT` | Isolamento entre organizações e propriedades, perfis, revogação imediata, CSRF, login sem enumeração de usuários |
| `ConcorrenciaIT` | Lotação sob movimentações simultâneas e brinco duplicado sob recebimentos simultâneos |
| `MigracoesIT` | Migrações aplicadas e schema compatível com o mapeamento (`ddl-auto=validate`) |

### Teste de ponta a ponta

1. Suba a aplicação (opção A ou B) com um administrador de uma organização **sem propriedades**.
2. Rode:

```bash
cd frontend
npx playwright install chromium
E2E_EMAIL=admin@exemplo.com.br E2E_SENHA=uma-senha-longa npm run e2e
# Opção A (contêineres): acrescente E2E_URL=http://localhost:8081
```

### Estado da verificação (26/10/2026)

Executado nesta máquina, com PostgreSQL 17.11 local, ainda sem Docker:

| Verificação | Resultado |
|---|---|
| `./mvnw test` (46 testes: domínio, caso de uso e ArchUnit) | Todos passaram |
| Migrações V1–V5 em banco novo, com `ddl-auto=validate` | OK |
| `npm run verificar` (tipos, lint, build) | OK |
| E2E Playwright, fluxo completo no desktop e navegação no celular | 2 de 2 passaram |
| Perfil `demo` em banco vazio | Carregou |
| API manual: CSRF, validação 400, lotação 422 | Respostas corretas |

**Ainda não executado:** `./mvnw verify` (testes `*IT` com Testcontainers) e `docker compose up`. Os dois dependem do Docker Desktop, que precisa do WSL2 ativo; aqui isso só acontece depois de reiniciar o Windows.

## Estrutura

```
backend/   Spring Boot. Pacotes por módulo: domain, application (port.in, port.out, usecase), adapter, configuration
frontend/  React + Vite. Páginas em src/paginas, componentes em src/componentes, E2E em e2e/
docs/      Arquitetura, decisões e implantação
```

## Limitações conhecidas desta entrega

- **Fora desta fase:**
  - dieta e trato, sanidade, estoque, custos, importação CSV, anexos, tarefas recorrentes, offline/PWA e dispositivos;
  - baixa de animais (venda, morte, abate) e transferência entre propriedades;
  - substituição de brinco: o modelo preserva o histórico de identificações, mas ainda não há tela nem endpoint.
- **Sem cálculo ainda:** ganho de peso, consumo e custos. O dashboard mostra "indisponível" em vez de números estimados.
- **Responsável por tarefa:** só o administrador escolhe o responsável ao criar uma tarefa. Os demais perfis criam tarefas sem responsável.
- **Fuso horário:** a data e hora digitadas usam o fuso do dispositivo. O "hoje" das rotinas e do dashboard usa o fuso da propriedade.
- **Login:** não há limitação de tentativas por IP ou conta. Recomenda-se um proxy com rate limiting em produção.
- **Migrações:** não há versões anteriores publicadas. O teste de migração cobre o banco novo; o teste de atualização entre versões passa a valer a partir da primeira versão publicada.
