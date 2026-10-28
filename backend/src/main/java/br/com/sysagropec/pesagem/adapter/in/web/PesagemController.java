package br.com.sysagropec.pesagem.adapter.in.web;

import br.com.sysagropec.pesagem.application.port.in.PesagemUseCase;
import br.com.sysagropec.pesagem.application.port.in.PesagemUseCase.PesagemRegistrada;
import br.com.sysagropec.pesagem.application.port.in.PesagemUseCase.PesagemResumo;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class PesagemController {

    private final PesagemUseCase pesagens;
    private final PesagemUseCase.Consultas consultas;

    PesagemController(PesagemUseCase pesagens, PesagemUseCase.Consultas consultas) {
        this.pesagens = pesagens;
        this.consultas = consultas;
    }

    @PostMapping("/animais/{animalId}/pesagens")
    @ResponseStatus(HttpStatus.CREATED)
    PesagemRegistrada registrar(ContextoExecucao ctx, @PathVariable UUID animalId,
                                @RequestHeader(name = "Idempotency-Key", required = false) String chave,
                                @Valid @RequestBody PesagemRequest r) {
        return pesagens.registrar(ctx, new PesagemUseCase.RegistrarPesagemComando(animalId, r.pesoKg(), r.natureza(),
                r.metodo(), r.finalidade(), r.equipamento(), r.observacao(), r.ocorridoEm(), r.tarefaId(), chave));
    }

    @GetMapping("/animais/{animalId}/pesagens")
    List<PesagemResumo> listar(ContextoExecucao ctx, @PathVariable UUID animalId) {
        return consultas.listarPorAnimal(ctx, animalId);
    }

    @PostMapping("/pesagens/{pesagemId}/cancelamento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelar(ContextoExecucao ctx, @PathVariable UUID pesagemId, @Valid @RequestBody CancelamentoRequest r) {
        pesagens.cancelar(ctx, pesagemId, r.motivo());
    }

    record PesagemRequest(
            @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("2000") @Digits(integer = 4, fraction = 2)
            BigDecimal pesoKg,
            String natureza,
            String metodo,
            String finalidade,
            @Size(max = 80) String equipamento,
            @Size(max = 500) String observacao,
            @NotNull Instant ocorridoEm,
            UUID tarefaId) {
    }

    record CancelamentoRequest(@NotBlank @Size(max = 300) String motivo) {
    }
}
