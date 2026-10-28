package br.com.sysagropec.tarefas.adapter.in.web;

import br.com.sysagropec.shared.application.Pagina;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.tarefas.application.port.in.TarefasUseCase;
import br.com.sysagropec.tarefas.application.port.in.TarefasUseCase.RotinasDoDia;
import br.com.sysagropec.tarefas.application.port.in.TarefasUseCase.TarefaDetalhe;
import br.com.sysagropec.tarefas.application.port.in.TarefasUseCase.TarefaResumo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class TarefasController {

    private final TarefasUseCase tarefas;
    private final TarefasUseCase.Consultas consultas;

    TarefasController(TarefasUseCase tarefas, TarefasUseCase.Consultas consultas) {
        this.tarefas = tarefas;
        this.consultas = consultas;
    }

    @PostMapping("/propriedades/{propriedadeId}/tarefas")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> criar(ContextoExecucao ctx, @PathVariable UUID propriedadeId,
                            @Valid @RequestBody NovaTarefaRequest r) {
        List<TarefasUseCase.ItemChecklistNovo> itens = r.checklist() == null ? List.of() : r.checklist().stream()
                .map(i -> new TarefasUseCase.ItemChecklistNovo(i.descricao(), Boolean.TRUE.equals(i.obrigatorio())))
                .toList();
        UUID id = tarefas.criar(ctx, new TarefasUseCase.CriarTarefaComando(propriedadeId, r.tipo(), r.titulo(),
                r.descricao(), r.alvoTipo(), r.alvoId(), r.responsavelId(), r.prazo(), r.prioridade(), itens));
        return Map.of("id", id);
    }

    @GetMapping("/propriedades/{propriedadeId}/tarefas")
    Pagina<TarefaResumo> listar(ContextoExecucao ctx, @PathVariable UUID propriedadeId,
                                @RequestParam(required = false) Set<String> status,
                                @RequestParam(required = false) UUID responsavelId,
                                @RequestParam(required = false) String alvoTipo,
                                @RequestParam(required = false) UUID alvoId,
                                @RequestParam(defaultValue = "false") boolean apenasAtrasadas,
                                @RequestParam(required = false) Integer pagina,
                                @RequestParam(required = false) Integer tamanho) {
        return consultas.listar(ctx, new TarefasUseCase.FiltroTarefas(propriedadeId, status, responsavelId,
                alvoTipo == null || alvoTipo.isBlank() ? null : alvoTipo, alvoId, apenasAtrasadas, pagina, tamanho));
    }

    @GetMapping("/propriedades/{propriedadeId}/rotinas-de-hoje")
    RotinasDoDia rotinasDeHoje(ContextoExecucao ctx, @PathVariable UUID propriedadeId) {
        return consultas.rotinasDeHoje(ctx, propriedadeId);
    }

    @GetMapping("/tarefas/{tarefaId}")
    TarefaDetalhe detalhar(ContextoExecucao ctx, @PathVariable UUID tarefaId) {
        return consultas.detalhar(ctx, tarefaId);
    }

    @PostMapping("/tarefas/{tarefaId}/inicio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void iniciar(ContextoExecucao ctx, @PathVariable UUID tarefaId) {
        tarefas.iniciar(ctx, tarefaId);
    }

    @PostMapping("/tarefas/{tarefaId}/conclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void concluir(ContextoExecucao ctx, @PathVariable UUID tarefaId,
                  @Valid @RequestBody(required = false) ConclusaoRequest r) {
        tarefas.concluir(ctx, tarefaId, r == null ? null : r.observacao(),
                r == null || r.itensConcluidos() == null ? Set.of() : r.itensConcluidos());
    }

    @PostMapping("/tarefas/{tarefaId}/impedimento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void impedir(ContextoExecucao ctx, @PathVariable UUID tarefaId, @Valid @RequestBody JustificativaRequest r) {
        tarefas.impedir(ctx, tarefaId, r.justificativa());
    }

    @PostMapping("/tarefas/{tarefaId}/cancelamento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelar(ContextoExecucao ctx, @PathVariable UUID tarefaId, @Valid @RequestBody JustificativaRequest r) {
        tarefas.cancelar(ctx, tarefaId, r.justificativa());
    }

    record NovaTarefaRequest(
            @NotBlank String tipo,
            @NotBlank @Size(max = 120) String titulo,
            @Size(max = 1000) String descricao,
            @NotBlank String alvoTipo,
            UUID alvoId,
            UUID responsavelId,
            @NotNull Instant prazo,
            String prioridade,
            @Size(max = 50) List<@Valid ItemRequest> checklist) {
    }

    record ItemRequest(@NotBlank @Size(max = 200) String descricao, Boolean obrigatorio) {
    }

    record ConclusaoRequest(@Size(max = 1000) String observacao, Set<UUID> itensConcluidos) {
    }

    record JustificativaRequest(@NotBlank @Size(max = 500) String justificativa) {
    }
}
