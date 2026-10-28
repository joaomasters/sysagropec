package br.com.sysagropec.consultas.adapter.in.web;

import br.com.sysagropec.consultas.application.port.in.ConsultasQuery;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.Dashboard;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.LinhaDoTempo;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class ConsultasController {

    private final ConsultasQuery consultas;

    ConsultasController(ConsultasQuery consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/animais/{animalId}/linha-do-tempo")
    LinhaDoTempo linhaDoTempo(ContextoExecucao ctx, @PathVariable UUID animalId) {
        return consultas.linhaDoTempo(ctx, animalId);
    }

    @GetMapping("/propriedades/{propriedadeId}/dashboard")
    Dashboard dashboard(ContextoExecucao ctx, @PathVariable UUID propriedadeId) {
        return consultas.dashboard(ctx, propriedadeId);
    }
}
