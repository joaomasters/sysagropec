package br.com.sysagropec.propriedades.adapter.in.web;

import br.com.sysagropec.propriedades.application.port.in.PropriedadesUseCase;
import br.com.sysagropec.propriedades.application.port.in.PropriedadesUseCase.InstalacaoResumo;
import br.com.sysagropec.propriedades.application.port.in.PropriedadesUseCase.PropriedadeResumo;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/propriedades")
class PropriedadesController {

    private final PropriedadesUseCase propriedades;
    private final PropriedadesUseCase.Consultas consultas;

    PropriedadesController(PropriedadesUseCase propriedades, PropriedadesUseCase.Consultas consultas) {
        this.propriedades = propriedades;
        this.consultas = consultas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PropriedadeResumo cadastrar(ContextoExecucao ctx, @Valid @RequestBody NovaPropriedadeRequest r) {
        return propriedades.cadastrar(ctx,
                new PropriedadesUseCase.CadastrarPropriedadeComando(r.nome(), r.municipio(), r.uf(), r.fusoHorario()));
    }

    @GetMapping
    List<PropriedadeResumo> listar(ContextoExecucao ctx) {
        return consultas.listar(ctx);
    }

    @PostMapping("/{propriedadeId}/instalacoes")
    @ResponseStatus(HttpStatus.CREATED)
    InstalacaoResumo cadastrarInstalacao(ContextoExecucao ctx, @PathVariable UUID propriedadeId,
                                         @Valid @RequestBody NovaInstalacaoRequest r) {
        return propriedades.cadastrarInstalacao(ctx, new PropriedadesUseCase.CadastrarInstalacaoComando(
                propriedadeId, r.tipo(), r.codigo(), r.descricao(), r.capacidadeCabecas()));
    }

    @GetMapping("/{propriedadeId}/instalacoes")
    List<InstalacaoResumo> listarInstalacoes(ContextoExecucao ctx, @PathVariable UUID propriedadeId) {
        return consultas.listarInstalacoes(ctx, propriedadeId);
    }

    record NovaPropriedadeRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 120) String municipio,
            @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "UF deve ter 2 letras") String uf,
            @Size(max = 60) String fusoHorario) {
    }

    record NovaInstalacaoRequest(
            @NotNull @Pattern(regexp = "CURRAL|PIQUETE|PASTO|OUTRA") String tipo,
            @NotBlank @Size(max = 30) String codigo,
            @Size(max = 200) String descricao,
            @Positive @Max(100_000) Integer capacidadeCabecas) {
    }
}
