package br.com.sysagropec.rebanho.adapter.out.propriedades;

import br.com.sysagropec.propriedades.application.port.in.PropriedadesInternasQuery;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.PropriedadesPort;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/** Traduz o contrato público do módulo propriedades para a porta de saída do rebanho. */
@Component("rebanhoPropriedadesAdapter")
class PropriedadesAdapter implements PropriedadesPort {

    private final PropriedadesInternasQuery propriedades;

    PropriedadesAdapter(PropriedadesInternasQuery propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public Optional<InstalacaoAlocavel> instalacaoComBloqueio(UUID organizacaoId, UUID instalacaoId) {
        return propriedades.instalacaoComBloqueio(organizacaoId, instalacaoId)
                .map(i -> new InstalacaoAlocavel(i.id(), i.propriedadeId(), i.codigo(), i.capacidadeCabecas(),
                        i.ativa()));
    }

    @Override
    public ZoneId fusoHorario(UUID organizacaoId, UUID propriedadeId) {
        return propriedades.fusoHorario(organizacaoId, propriedadeId)
                .orElseThrow(() -> new NaoEncontradoException("Propriedade"));
    }
}
