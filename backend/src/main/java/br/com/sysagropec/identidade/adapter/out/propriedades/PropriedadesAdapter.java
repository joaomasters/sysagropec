package br.com.sysagropec.identidade.adapter.out.propriedades;

import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.PropriedadesPort;
import br.com.sysagropec.propriedades.application.port.in.PropriedadesInternasQuery;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component("identidadePropriedadesAdapter")
class PropriedadesAdapter implements PropriedadesPort {

    private final PropriedadesInternasQuery propriedades;

    PropriedadesAdapter(PropriedadesInternasQuery propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public Set<UUID> idsAtivos(UUID organizacaoId) {
        return propriedades.idsAtivos(organizacaoId);
    }
}
