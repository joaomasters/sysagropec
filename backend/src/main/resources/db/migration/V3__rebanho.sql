-- Rebanho: animais, identificações, lotes de manejo, localização física e movimentações.
-- Chaves entre recebimento, animal, movimentação e vínculos são verificadas no commit (deferred),
-- pois esses registros são gravados juntos na mesma transação.
-- Lote (agrupamento de manejo) e instalação (localização física) são vínculos independentes,
-- cada um com histórico próprio. Índices únicos parciais garantem no máximo um vínculo aberto por animal.

create table sequencia_animal (
    organizacao_id uuid primary key references organizacao (id),
    ultimo         bigint not null check (ultimo >= 0)
);

create table lote (
    id             uuid primary key,
    organizacao_id uuid        not null,
    propriedade_id uuid        not null,
    codigo         varchar(40) not null,
    fase           varchar(20) not null
        check (fase in ('RECEPCAO', 'ADAPTACAO', 'CRESCIMENTO', 'TERMINACAO', 'RECRIA', 'OUTRA')),
    finalidade     varchar(20) not null check (finalidade in ('ENGORDA', 'RECRIA', 'REPRODUCAO')),
    status         varchar(20) not null check (status in ('ATIVO', 'ENCERRADO')),
    inicio         timestamptz not null,
    fim            timestamptz,
    observacao     varchar(500),
    criado_em      timestamptz not null,
    atualizado_em  timestamptz not null,
    versao         bigint      not null default 0,
    constraint uk_lote_org_id unique (organizacao_id, id),
    constraint fk_lote_propriedade foreign key (organizacao_id, propriedade_id)
        references propriedade (organizacao_id, id),
    constraint ck_lote_encerramento check ((status = 'ENCERRADO') = (fim is not null)),
    constraint ck_lote_periodo check (fim is null or fim >= inicio)
);
create unique index uk_lote_codigo_ativo on lote (propriedade_id, lower(codigo)) where status = 'ATIVO';

create table movimentacao (
    id             uuid primary key,
    organizacao_id uuid         not null,
    propriedade_id uuid         not null,
    tipo           varchar(30)  not null check (tipo in ('RECEBIMENTO', 'FORMACAO_LOTE', 'TRANSFERENCIA_INTERNA')),
    motivo         varchar(300) not null,
    ocorrido_em    timestamptz  not null,
    registrado_em  timestamptz  not null,
    responsavel_id uuid         not null,
    constraint uk_movimentacao_org_id unique (organizacao_id, id),
    constraint fk_movimentacao_propriedade foreign key (organizacao_id, propriedade_id)
        references propriedade (organizacao_id, id),
    constraint fk_movimentacao_responsavel foreign key (organizacao_id, responsavel_id)
        references usuario (organizacao_id, id)
);
create index ix_movimentacao_propriedade on movimentacao (propriedade_id, ocorrido_em desc);

create table recebimento (
    id               uuid primary key,
    organizacao_id   uuid         not null,
    propriedade_id   uuid         not null,
    instalacao_id    uuid         not null,
    lote_id          uuid,
    movimentacao_id  uuid         not null,
    ocorrido_em      timestamptz  not null,
    registrado_em    timestamptz  not null,
    origem_descricao varchar(200) not null,
    documento        varchar(60),
    observacao       varchar(500),
    quantidade       integer      not null check (quantidade > 0),
    responsavel_id   uuid         not null,
    constraint uk_recebimento_org_id unique (organizacao_id, id),
    constraint fk_recebimento_propriedade foreign key (organizacao_id, propriedade_id)
        references propriedade (organizacao_id, id),
    constraint fk_recebimento_instalacao foreign key (organizacao_id, instalacao_id)
        references instalacao (organizacao_id, id),
    constraint fk_recebimento_lote foreign key (organizacao_id, lote_id) references lote (organizacao_id, id),
    constraint fk_recebimento_movimentacao foreign key (organizacao_id, movimentacao_id)
        references movimentacao (organizacao_id, id) deferrable initially deferred,
    constraint fk_recebimento_responsavel foreign key (organizacao_id, responsavel_id)
        references usuario (organizacao_id, id)
);

create table animal (
    id                  uuid primary key,
    organizacao_id      uuid        not null,
    propriedade_id      uuid        not null,
    numero              bigint      not null,
    sexo                varchar(10) not null check (sexo in ('MACHO', 'FEMEA')),
    categoria           varchar(20) not null
        check (categoria in ('BEZERRO', 'BEZERRA', 'GARROTE', 'NOVILHA', 'BOI', 'VACA', 'TOURO')),
    finalidade          varchar(20) not null check (finalidade in ('ENGORDA', 'RECRIA', 'REPRODUCAO')),
    raca                varchar(60),
    data_nascimento     date,
    nascimento_estimado boolean     not null default false,
    origem              varchar(30) not null
        check (origem in ('COMPRA', 'NASCIMENTO_PROPRIO', 'TRANSFERENCIA', 'OUTRA')),
    origem_descricao    varchar(200),
    recebimento_id      uuid,
    situacao            varchar(20) not null check (situacao in ('ATIVO', 'VENDIDO', 'ABATIDO', 'MORTO', 'BAIXADO')),
    criado_em           timestamptz not null,
    atualizado_em       timestamptz not null,
    versao              bigint      not null default 0,
    constraint uk_animal_org_id unique (organizacao_id, id),
    constraint uk_animal_numero unique (organizacao_id, numero),
    constraint fk_animal_propriedade foreign key (organizacao_id, propriedade_id)
        references propriedade (organizacao_id, id),
    constraint fk_animal_recebimento foreign key (organizacao_id, recebimento_id)
        references recebimento (organizacao_id, id) deferrable initially deferred,
    constraint ck_animal_nascimento_estimado check (data_nascimento is not null or not nascimento_estimado)
);
create index ix_animal_propriedade on animal (propriedade_id, situacao);

create table animal_identificacao (
    id             uuid primary key,
    organizacao_id uuid        not null,
    animal_id      uuid        not null,
    tipo           varchar(20) not null check (tipo in ('BRINCO', 'ELETRONICO')),
    valor          varchar(40) not null,
    inicio         timestamptz not null,
    fim            timestamptz,
    constraint fk_identificacao_animal foreign key (organizacao_id, animal_id) references animal (organizacao_id, id),
    constraint ck_identificacao_periodo check (fim is null or fim >= inicio)
);
-- Unicidade entre identificações ativas da organização; identificações substituídas ficam no histórico.
create unique index uk_identificacao_ativa on animal_identificacao (organizacao_id, tipo, lower(valor)) where fim is null;
create unique index uk_identificacao_tipo_por_animal on animal_identificacao (animal_id, tipo) where fim is null;

create table animal_lote_vinculo (
    id              uuid primary key,
    organizacao_id  uuid        not null,
    animal_id       uuid        not null,
    lote_id         uuid        not null,
    movimentacao_id uuid        not null,
    inicio          timestamptz not null,
    fim             timestamptz,
    constraint fk_vinculo_lote_animal foreign key (organizacao_id, animal_id) references animal (organizacao_id, id),
    constraint fk_vinculo_lote_lote foreign key (organizacao_id, lote_id) references lote (organizacao_id, id),
    constraint fk_vinculo_lote_mov foreign key (organizacao_id, movimentacao_id)
        references movimentacao (organizacao_id, id) deferrable initially deferred,
    constraint ck_vinculo_lote_periodo check (fim is null or fim >= inicio)
);
create unique index uk_vinculo_lote_aberto on animal_lote_vinculo (animal_id) where fim is null;
create index ix_vinculo_lote_lote on animal_lote_vinculo (lote_id) where fim is null;

create table animal_localizacao (
    id              uuid primary key,
    organizacao_id  uuid        not null,
    animal_id       uuid        not null,
    instalacao_id   uuid        not null,
    movimentacao_id uuid        not null,
    inicio          timestamptz not null,
    fim             timestamptz,
    constraint fk_localizacao_animal foreign key (organizacao_id, animal_id) references animal (organizacao_id, id),
    constraint fk_localizacao_instalacao foreign key (organizacao_id, instalacao_id)
        references instalacao (organizacao_id, id),
    constraint fk_localizacao_mov foreign key (organizacao_id, movimentacao_id)
        references movimentacao (organizacao_id, id) deferrable initially deferred,
    constraint ck_localizacao_periodo check (fim is null or fim >= inicio)
);
create unique index uk_localizacao_aberta on animal_localizacao (animal_id) where fim is null;
create index ix_localizacao_instalacao on animal_localizacao (instalacao_id) where fim is null;

create table movimentacao_item (
    movimentacao_id       uuid not null references movimentacao (id) deferrable initially deferred,
    animal_id             uuid not null references animal (id) deferrable initially deferred,
    lote_origem_id        uuid references lote (id),
    lote_destino_id       uuid references lote (id),
    instalacao_origem_id  uuid references instalacao (id),
    instalacao_destino_id uuid references instalacao (id),
    primary key (movimentacao_id, animal_id)
);
create index ix_movimentacao_item_animal on movimentacao_item (animal_id);
