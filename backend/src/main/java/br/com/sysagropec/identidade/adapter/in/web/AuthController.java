package br.com.sysagropec.identidade.adapter.in.web;

import br.com.sysagropec.identidade.application.port.in.AutenticacaoUseCase;
import br.com.sysagropec.identidade.application.port.in.AutenticacaoUseCase.UsuarioAutenticado;
import br.com.sysagropec.identidade.application.port.in.ContextoExecucaoQuery;
import br.com.sysagropec.identidade.application.port.in.SenhaUseCase;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.acesso.Permissao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class AuthController {

    private final AutenticacaoUseCase autenticacao;
    private final SenhaUseCase senhas;
    private final ContextoExecucaoQuery contextos;
    private final SecurityContextRepository securityContextRepository;

    AuthController(AutenticacaoUseCase autenticacao, SenhaUseCase senhas, ContextoExecucaoQuery contextos,
                   SecurityContextRepository securityContextRepository) {
        this.autenticacao = autenticacao;
        this.senhas = senhas;
        this.contextos = contextos;
        this.securityContextRepository = securityContextRepository;
    }

    /** Força a emissão do cookie XSRF-TOKEN antes do primeiro POST do cliente. */
    @GetMapping("/auth/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("cabecalho", token.getHeaderName());
    }

    @PostMapping("/auth/login")
    SessaoResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest request, HttpServletResponse response) {
        UsuarioAutenticado autenticado = autenticacao.autenticar(r.email(), r.senha());

        // Proteção contra fixação de sessão: o identificador muda no login.
        request.getSession(true);
        request.changeSessionId();

        SessaoUsuario principal = new SessaoUsuario(autenticado.usuarioId(), autenticado.organizacaoId());
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null,
                AuthorityUtils.createAuthorityList("ROLE_USUARIO")));
        SecurityContextHolder.setContext(sc);
        securityContextRepository.saveContext(sc, request, response);

        ContextoExecucao ctx = contextos.obter(autenticado.usuarioId())
                .orElseThrow(AutenticacaoUseCase.CredenciaisInvalidasException::new);
        return sessao(ctx);
    }

    @GetMapping("/sessao")
    SessaoResponse sessaoAtual(ContextoExecucao ctx) {
        return sessao(ctx);
    }

    @PostMapping("/auth/senha/solicitar-redefinicao")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void solicitarRedefinicao(@Valid @RequestBody SolicitarRedefinicaoRequest r) {
        senhas.solicitarRedefinicao(r.email());
    }

    @PostMapping("/auth/senha/definir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void definirSenha(@Valid @RequestBody DefinirSenhaRequest r) {
        senhas.definirSenha(r.token(), r.novaSenha());
    }

    private SessaoResponse sessao(ContextoExecucao ctx) {
        ContextoExecucaoQuery.DadosSessao dados = contextos.dadosSessao(ctx);
        Map<UUID, Set<Permissao>> permissoes = new TreeMap<>();
        ctx.permissoesPorPropriedade().forEach((id, p) -> permissoes.put(id, new TreeSet<>(p)));
        return new SessaoResponse(ctx.usuarioId(), ctx.nomeUsuario(), dados.email(), ctx.organizacaoId(),
                dados.organizacaoNome(), ctx.administrador(), permissoes);
    }

    record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 128) String senha) {
    }

    record SolicitarRedefinicaoRequest(@NotBlank @Size(max = 254) String email) {
    }

    record DefinirSenhaRequest(@NotBlank @Size(max = 100) String token, @NotBlank @Size(max = 128) String novaSenha) {
    }

    record SessaoResponse(UUID usuarioId, String nome, String email, UUID organizacaoId, String organizacaoNome,
                          boolean administrador, Map<UUID, Set<Permissao>> permissoesPorPropriedade) {
    }
}
