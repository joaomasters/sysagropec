package br.com.sysagropec.identidade.application.usecase;

import br.com.sysagropec.identidade.application.port.in.InicializacaoUseCase;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.OrganizacaoRepositoryPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.SenhaHasherPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.UsuarioRepositoryPort;
import br.com.sysagropec.identidade.domain.model.PoliticaSenha;
import br.com.sysagropec.identidade.domain.model.Usuario;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort.EventoAuditoria;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.domain.Verificar;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InicializacaoService implements InicializacaoUseCase {

    private final OrganizacaoRepositoryPort organizacoes;
    private final UsuarioRepositoryPort usuarios;
    private final SenhaHasherPort hasher;
    private final AuditoriaPort auditoria;
    private final RelogioPort relogio;

    public InicializacaoService(OrganizacaoRepositoryPort organizacoes, UsuarioRepositoryPort usuarios,
                                SenhaHasherPort hasher, AuditoriaPort auditoria, RelogioPort relogio) {
        this.organizacoes = organizacoes;
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Override
    public Optional<Inicializacao> inicializarSeVazio(String nomeOrganizacao, String nomeAdministrador, String email,
                                             String senha) {
        if (organizacoes.existeAlguma()) {
            return Optional.empty();
        }
        Instant agora = relogio.agora();
        UUID organizacaoId = UUID.randomUUID();
        organizacoes.criar(organizacaoId, Verificar.texto(nomeOrganizacao, "Nome da organização", 120), agora);
        Usuario admin = Usuario.novo(UUID.randomUUID(), organizacaoId, nomeAdministrador, email, true, agora);
        PoliticaSenha.validar(senha, admin.email());
        admin.definirSenhaHash(hasher.gerarHash(senha));
        usuarios.salvar(admin);
        auditoria.registrar(new EventoAuditoria(organizacaoId, admin.id(), "ORGANIZACAO_INICIALIZADA", "ORGANIZACAO",
                organizacaoId, Map.of(), agora));
        return Optional.of(new Inicializacao(organizacaoId, admin.id()));
    }
}
