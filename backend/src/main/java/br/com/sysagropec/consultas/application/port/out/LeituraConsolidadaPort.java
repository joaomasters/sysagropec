package br.com.sysagropec.consultas.application.port.out;

import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.Dashboard;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.Evento;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeituraConsolidadaPort {

    /** Propriedade do animal, para autorização; vazio se não existir na organização. */
    Optional<UUID> propriedadeDoAnimal(UUID organizacaoId, UUID animalId);

    List<Evento> eventosDoAnimal(UUID organizacaoId, UUID animalId);

    Optional<ZoneId> fusoHorario(UUID organizacaoId, UUID propriedadeId);

    Dashboard dashboard(UUID organizacaoId, UUID propriedadeId, LocalDate data, ZoneId zona, Instant agora);
}
