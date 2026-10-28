package br.com.sysagropec.rebanho.adapter.in.web;

import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.AnimalDetalhe;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.AnimalResumo;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.LoteResumo;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase.LoteFormado;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase.RecebimentoRegistrado;
import br.com.sysagropec.shared.application.Pagina;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class RebanhoController {

    private final RebanhoUseCase rebanho;
    private final RebanhoConsultas consultas;

    RebanhoController(RebanhoUseCase rebanho, RebanhoConsultas consultas) {
        this.rebanho = rebanho;
        this.consultas = consultas;
    }

    /** Aceita o cabeçalho Idempotency-Key: reenvios com a mesma chave devolvem o recebimento original. */
    @PostMapping("/propriedades/{propriedadeId}/recebimentos")
    @ResponseStatus(HttpStatus.CREATED)
    RecebimentoRegistrado receber(ContextoExecucao ctx, @PathVariable UUID propriedadeId,
                                  @RequestHeader(name = "Idempotency-Key", required = false) String chave,
                                  @Valid @RequestBody RecebimentoRequest r) {
        List<RebanhoUseCase.NovoAnimal> animais = r.animais().stream()
                .map(a -> new RebanhoUseCase.NovoAnimal(a.brinco(), a.identificadorEletronico(), a.sexo(),
                        a.categoria(), a.finalidade(), a.raca(), a.dataNascimento(),
                        Boolean.TRUE.equals(a.nascimentoEstimado())))
                .toList();
        return rebanho.receber(ctx, new RebanhoUseCase.ReceberAnimaisComando(propriedadeId, r.instalacaoId(),
                r.loteId(), r.ocorridoEm(), r.origem(), r.origemDescricao(), r.documento(), r.observacao(), chave,
                animais));
    }

    @GetMapping("/propriedades/{propriedadeId}/animais")
    Pagina<AnimalResumo> listarAnimais(ContextoExecucao ctx, @PathVariable UUID propriedadeId,
                                       @RequestParam(required = false) String busca,
                                       @RequestParam(required = false) UUID loteId,
                                       @RequestParam(required = false) UUID instalacaoId,
                                       @RequestParam(required = false) String categoria,
                                       @RequestParam(required = false) String situacao,
                                       @RequestParam(required = false) Boolean semLote,
                                       @RequestParam(required = false) Integer pagina,
                                       @RequestParam(required = false) Integer tamanho) {
        return consultas.listarAnimais(ctx, new RebanhoConsultas.FiltroAnimais(propriedadeId, busca, loteId,
                instalacaoId, vazioComoNulo(categoria), vazioComoNulo(situacao), semLote, pagina, tamanho));
    }

    @GetMapping("/animais/{animalId}")
    AnimalDetalhe detalharAnimal(ContextoExecucao ctx, @PathVariable UUID animalId) {
        return consultas.detalharAnimal(ctx, animalId);
    }

    @PostMapping("/propriedades/{propriedadeId}/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    LoteFormado formarLote(ContextoExecucao ctx, @PathVariable UUID propriedadeId, @Valid @RequestBody LoteRequest r) {
        return rebanho.formarLote(ctx, new RebanhoUseCase.FormarLoteComando(propriedadeId, r.codigo(), r.fase(),
                r.finalidade(), r.observacao(), r.ocorridoEm(), r.animalIds(), r.instalacaoId(), r.motivo()));
    }

    @GetMapping("/propriedades/{propriedadeId}/lotes")
    List<LoteResumo> listarLotes(ContextoExecucao ctx, @PathVariable UUID propriedadeId,
                                 @RequestParam(defaultValue = "false") boolean incluirEncerrados) {
        return consultas.listarLotes(ctx, propriedadeId, incluirEncerrados);
    }

    @PostMapping("/lotes/{loteId}/encerramento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void encerrarLote(ContextoExecucao ctx, @PathVariable UUID loteId, @RequestBody(required = false) EncerramentoRequest r) {
        rebanho.encerrarLote(ctx, loteId, r == null ? null : r.ocorridoEm());
    }

    @PostMapping("/propriedades/{propriedadeId}/movimentacoes")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> movimentar(ContextoExecucao ctx, @PathVariable UUID propriedadeId,
                                 @Valid @RequestBody MovimentacaoRequest r) {
        UUID id = rebanho.movimentar(ctx, new RebanhoUseCase.MovimentarAnimaisComando(propriedadeId, r.animalIds(),
                r.loteDestinoId(), Boolean.TRUE.equals(r.retirarDoLote()), r.instalacaoDestinoId(), r.motivo(),
                r.ocorridoEm()));
        return Map.of("movimentacaoId", id);
    }

    private static String vazioComoNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    record RecebimentoRequest(
            @NotNull UUID instalacaoId,
            UUID loteId,
            @NotNull Instant ocorridoEm,
            String origem,
            @NotBlank @Size(max = 200) String origemDescricao,
            @Size(max = 60) String documento,
            @Size(max = 500) String observacao,
            @NotEmpty @Size(max = 500) List<@Valid AnimalRequest> animais) {
    }

    record AnimalRequest(
            @NotBlank @Size(max = 40) String brinco,
            @Size(max = 40) String identificadorEletronico,
            @NotBlank String sexo,
            @NotBlank String categoria,
            String finalidade,
            @Size(max = 60) String raca,
            LocalDate dataNascimento,
            Boolean nascimentoEstimado) {
    }

    record LoteRequest(
            @NotBlank @Size(max = 40) String codigo,
            @NotBlank String fase,
            String finalidade,
            @Size(max = 500) String observacao,
            @NotNull Instant ocorridoEm,
            @Size(max = 500) List<UUID> animalIds,
            UUID instalacaoId,
            @Size(max = 300) String motivo) {
    }

    record MovimentacaoRequest(
            @NotEmpty @Size(max = 500) List<UUID> animalIds,
            UUID loteDestinoId,
            Boolean retirarDoLote,
            UUID instalacaoDestinoId,
            @NotBlank @Size(max = 300) String motivo,
            @NotNull Instant ocorridoEm) {
    }

    record EncerramentoRequest(Instant ocorridoEm) {
    }
}
