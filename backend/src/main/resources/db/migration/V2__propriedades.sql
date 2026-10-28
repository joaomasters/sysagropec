create table propriedade (
    id             uuid primary key,
    organizacao_id uuid         not null references organizacao (id),
    nome           varchar(120) not null,
    municipio      varchar(120),
    uf             varchar(2),
    fuso_horario   varchar(60)  not null,
    ativa          boolean      not null default true,
    criado_em      timestamptz  not null,
    atualizado_em  timestamptz  not null,
    versao         bigint       not null default 0,
    constraint uk_propriedade_org_id unique (organizacao_id, id)
);
create unique index uk_propriedade_nome on propriedade (organizacao_id, lower(nome));

create table instalacao (
    id                 uuid primary key,
    organizacao_id     uuid        not null,
    propriedade_id     uuid        not null,
    tipo               varchar(20) not null check (tipo in ('CURRAL', 'PIQUETE', 'PASTO', 'OUTRA')),
    codigo             varchar(30) not null,
    descricao          varchar(200),
    capacidade_cabecas integer check (capacidade_cabecas > 0),
    ativa              boolean     not null default true,
    criado_em          timestamptz not null,
    atualizado_em      timestamptz not null,
    versao             bigint      not null default 0,
    constraint uk_instalacao_org_id unique (organizacao_id, id),
    constraint fk_instalacao_propriedade foreign key (organizacao_id, propriedade_id)
        references propriedade (organizacao_id, id)
);
create unique index uk_instalacao_codigo on instalacao (propriedade_id, lower(codigo));

-- Pertence ao módulo identidade; criada aqui porque referencia propriedade.
create table acesso_propriedade (
    usuario_id     uuid        not null,
    organizacao_id uuid        not null,
    propriedade_id uuid        not null,
    perfil         varchar(20) not null
        check (perfil in ('GESTOR', 'VETERINARIO', 'NUTRICIONISTA', 'FINANCEIRO', 'OPERADOR', 'CONSULTA')),
    primary key (usuario_id, propriedade_id),
    constraint fk_acesso_usuario foreign key (organizacao_id, usuario_id) references usuario (organizacao_id, id),
    constraint fk_acesso_propriedade foreign key (organizacao_id, propriedade_id)
        references propriedade (organizacao_id, id)
);
