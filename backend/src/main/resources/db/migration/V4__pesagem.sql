create table pesagem (
    id                  uuid primary key,
    organizacao_id      uuid         not null,
    propriedade_id      uuid         not null,
    animal_id           uuid         not null,
    peso_kg             numeric(7, 2) not null check (peso_kg > 0 and peso_kg <= 2000),
    natureza            varchar(10)  not null check (natureza in ('MEDIDO', 'ESTIMADO')),
    metodo              varchar(30)  not null
        check (metodo in ('BALANCA_INDIVIDUAL', 'FITA_BARIMETRICA', 'VISUAL', 'OUTRO')),
    finalidade          varchar(20)  not null check (finalidade in ('ENTRADA', 'ACOMPANHAMENTO', 'SAIDA')),
    equipamento         varchar(80),
    observacao          varchar(500),
    ocorrido_em         timestamptz  not null,
    registrado_em       timestamptz  not null,
    responsavel_id      uuid         not null,
    tarefa_id           uuid,
    cancelada_em        timestamptz,
    cancelada_por       uuid,
    motivo_cancelamento varchar(300),
    versao              bigint       not null default 0,
    constraint fk_pesagem_animal foreign key (organizacao_id, animal_id) references animal (organizacao_id, id),
    constraint fk_pesagem_propriedade foreign key (organizacao_id, propriedade_id)
        references propriedade (organizacao_id, id),
    constraint fk_pesagem_responsavel foreign key (organizacao_id, responsavel_id)
        references usuario (organizacao_id, id),
    constraint ck_pesagem_cancelamento check (
        (cancelada_em is null and cancelada_por is null and motivo_cancelamento is null)
        or (cancelada_em is not null and cancelada_por is not null and motivo_cancelamento is not null))
);
create index ix_pesagem_animal on pesagem (animal_id, ocorrido_em desc);
-- No máximo uma pesagem de entrada válida por animal; para corrigir, cancela-se a anterior.
create unique index uk_pesagem_entrada on pesagem (animal_id) where finalidade = 'ENTRADA' and cancelada_em is null;
create index ix_pesagem_propriedade on pesagem (propriedade_id, ocorrido_em desc);
