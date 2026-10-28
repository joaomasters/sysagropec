package br.com.sysagropec.identidade.adapter.in.web;

import br.com.sysagropec.identidade.application.port.in.AutenticacaoUseCase.CredenciaisInvalidasException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class IdentidadeErrosHandler {

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ProblemDetail credenciais(CredenciaisInvalidasException e) {
        return naoAutenticado(e.getMessage());
    }

    @ExceptionHandler(ContextoExecucaoArgumentResolver.SessaoInvalidaException.class)
    ProblemDetail sessao(RuntimeException e) {
        return naoAutenticado(e.getMessage());
    }

    private static ProblemDetail naoAutenticado(String detalhe) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detalhe);
        p.setTitle("Não autenticado");
        return p;
    }
}
