package br.com.sysagropec.identidade.application.port.in;

import java.util.Optional;
import java.util.UUID;

/** Cria a primeira organização e seu administrador quando o banco ainda não tem nenhuma. */
public interface InicializacaoUseCase {

    Optional<Inicializacao> inicializarSeVazio(String nomeOrganizacao, String nomeAdministrador, String email,
                                               String senha);

    record Inicializacao(UUID organizacaoId, UUID administradorId) {
    }
}
