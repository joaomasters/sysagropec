create table tarefa (
    id                   uuid primary key,
    organizacao_id       uuid         not null,
    propriedade_id       uuid         not null,
    tipo                 varchar(30)  not null check (tipo in ('TRATO', 'LEITURA_COCHO', 'LIMPEZA', 'INSPECAO_AGUA',
        'PESAGEM', 'PROCEDIMENTO_SANITARIO', 'MANUTENCAO', 'OUTRA')),
    titulo               varchar(120) not null,
    descricao            varchar(1000),
    alvo_tipo            varchar(20)  not null check (alvo_tipo in ('PROPRIEDADE', 'INSTALACAO', 'LOTE', 'ANIMAL')),
    alvo_id              uuid         not null,
    responsavel_id       uuid,
    prazo                timestamptz  not null,
    prioridade           varchar(10)  not null check (prioridade in ('BAIXA', 'MEDIA', 'ALTA')),
    status               varchar(20)  not null
        check (status in ('PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA', 'IMPEDIDA', 'CANCELADA')),
    criada_por           uuid         not null,
    criada_em            timestamptz  not null,
    iniciada_em          timestamptz,
    iniciada_por         uuid,
    finalizada_em        timestamptz,
    finalizada_por       uuid,
    observacao_conclusao varchar(1000),
    justificativa        varchar(500),
    atualizado_em        timestamptz  not null,
    versao               bigint       not null default 0,
    constraint uk_tarefa_org_id unique (organizacao_id, id),
    constraint fk_tarefa_propriedade foreign key (organizacao_id, propriedade_id)
        references propriedade (organizacao_id, id),
    constraint fk_tarefa_responsavel foreign key (organizacao_id, responsavel_id)
        references usuario (organizacao_id, id),
    constraint fk_tarefa_criador foreign key (organizacao_id, criada_por) references usuario (organizacao_id, id),
    constraint ck_tarefa_finalizacao check (
        (status in ('PENDENTE', 'EM_ANDAMENTO')) = (finalizada_em is null)),
    constraint ck_tarefa_justificativa check (status not in ('IMPEDIDA', 'CANCELADA') or justificativa is not null)
);
create index ix_tarefa_agenda on tarefa (propriedade_id, status, prazo);
create index ix_tarefa_alvo on tarefa (alvo_tipo, alvo_id);

create table tarefa_checklist_item (
    id           uuid primary key,
    tarefa_id    uuid         not null references tarefa (id),
    ordem        integer      not null,
    descricao    varchar(200) not null,
    obrigatorio  boolean      not null,
    concluido    boolean      not null default false,
    concluido_por uuid,
    concluido_em timestamptz,
    constraint uk_checklist_ordem unique (tarefa_id, ordem)
);

-- Registro técnico produzido pela tarefa. Um registro só pode atender uma tarefa,
-- o que evita que a conclusão gere ou reaproveite lançamentos duplicados.
create table tarefa_resultado (
    tarefa_id     uuid        not null references tarefa (id),
    tipo          varchar(30) not null,
    resultado_id  uuid        not null,
    vinculado_em  timestamptz not null,
    primary key (tarefa_id, tipo, resultado_id),
    constraint uk_tarefa_resultado unique (tipo, resultado_id)
);

alter table pesagem
    add constraint fk_pesagem_tarefa foreign key (organizacao_id, tarefa_id) references tarefa (organizacao_id, id);
