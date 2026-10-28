-- Fundação: organização, usuários, sessões, auditoria e idempotência.
-- Convenção: tabelas de negócio têm organizacao_id e unique (organizacao_id, id) para permitir
-- chaves estrangeiras compostas, que impedem no banco referências entre organizações diferentes.

create table organizacao (
    id          uuid primary key,
    nome        varchar(120) not null,
    criado_em   timestamptz  not null
);

create table usuario (
    id              uuid primary key,
    organizacao_id  uuid         not null references organizacao (id),
    nome            varchar(120) not null,
    email           varchar(254) not null,
    senha_hash      varchar(255),
    administrador   boolean      not null default false,
    ativo           boolean      not null default true,
    criado_em       timestamptz  not null,
    atualizado_em   timestamptz  not null,
    versao          bigint       not null default 0,
    constraint uk_usuario_org_id unique (organizacao_id, id),
    constraint ck_usuario_email_minusculo check (email = lower(email))
);
create unique index uk_usuario_email on usuario (email);

create table token_senha (
    id            uuid primary key,
    usuario_id    uuid        not null references usuario (id),
    finalidade    varchar(20) not null check (finalidade in ('REDEFINICAO', 'CONVITE')),
    token_hash    varchar(64) not null unique,
    criado_em     timestamptz not null,
    expira_em     timestamptz not null,
    usado_em      timestamptz,
    invalidado_em timestamptz,
    constraint ck_token_validade check (expira_em > criado_em)
);
create index ix_token_senha_usuario on token_senha (usuario_id) where usado_em is null and invalidado_em is null;

create table auditoria_evento (
    id             uuid primary key,
    organizacao_id uuid references organizacao (id),
    usuario_id     uuid references usuario (id),
    acao           varchar(80) not null,
    recurso_tipo   varchar(60),
    recurso_id     uuid,
    detalhes       jsonb       not null default '{}'::jsonb,
    ocorrido_em    timestamptz not null,
    request_id     varchar(64)
);
create index ix_auditoria_org_data on auditoria_evento (organizacao_id, ocorrido_em desc);
create index ix_auditoria_recurso on auditoria_evento (recurso_id);

create table idempotencia (
    organizacao_id  uuid        not null references organizacao (id),
    escopo          varchar(60) not null,
    chave           varchar(100) not null,
    hash_requisicao varchar(64) not null,
    resultado_id    uuid        not null,
    criado_em       timestamptz not null,
    primary key (organizacao_id, escopo, chave)
);

-- Sessões HTTP (Spring Session JDBC): sobrevivem a reinícios e podem ser revogadas no servidor.
create table spring_session (
    primary_id            char(36)     not null,
    session_id            char(36)     not null,
    creation_time         bigint       not null,
    last_access_time      bigint       not null,
    max_inactive_interval int          not null,
    expiry_time           bigint       not null,
    principal_name        varchar(100),
    constraint spring_session_pk primary key (primary_id)
);
create unique index spring_session_ix1 on spring_session (session_id);
create index spring_session_ix2 on spring_session (expiry_time);
create index spring_session_ix3 on spring_session (principal_name);

create table spring_session_attributes (
    session_primary_id char(36)     not null,
    attribute_name     varchar(200) not null,
    attribute_bytes    bytea        not null,
    constraint spring_session_attributes_pk primary key (session_primary_id, attribute_name),
    constraint spring_session_attributes_fk foreign key (session_primary_id)
        references spring_session (primary_id) on delete cascade
);
