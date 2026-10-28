package br.com.sysagropec.identidade.adapter.in.web;

import br.com.sysagropec.identidade.application.port.in.UsuariosUseCase;
import br.com.sysagropec.identidade.application.port.in.UsuariosUseCase.UsuarioResumo;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
class UsuariosController {

    private final UsuariosUseCase usuarios;

    UsuariosController(UsuariosUseCase usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    List<UsuarioResumo> listar(ContextoExecucao ctx) {
        return usuarios.listar(ctx);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioResumo criar(ContextoExecucao ctx, @Valid @RequestBody NovoUsuarioRequest r) {
        return usuarios.criar(ctx, new UsuariosUseCase.CriarUsuarioComando(r.nome(), r.email(), r.administrador()));
    }

    @PutMapping("/{usuarioId}/acessos/{propriedadeId}")
    UsuarioResumo concederAcesso(ContextoExecucao ctx, @PathVariable UUID usuarioId, @PathVariable UUID propriedadeId,
                                 @Valid @RequestBody AcessoRequest r) {
        return usuarios.concederAcesso(ctx, usuarioId, propriedadeId, r.perfil());
    }

    @DeleteMapping("/{usuarioId}/acessos/{propriedadeId}")
    UsuarioResumo revogarAcesso(ContextoExecucao ctx, @PathVariable UUID usuarioId, @PathVariable UUID propriedadeId) {
        return usuarios.revogarAcesso(ctx, usuarioId, propriedadeId);
    }

    @PostMapping("/{usuarioId}/desativacao")
    UsuarioResumo desativar(ContextoExecucao ctx, @PathVariable UUID usuarioId) {
        return usuarios.desativar(ctx, usuarioId);
    }

    record NovoUsuarioRequest(@NotBlank @Size(max = 120) String nome,
                              @NotBlank @Email @Size(max = 254) String email,
                              boolean administrador) {
    }

    record AcessoRequest(@NotNull String perfil) {
    }
}
