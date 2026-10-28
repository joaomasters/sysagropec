package br.com.sysagropec.shared.adapter.in.web;

import br.com.sysagropec.shared.application.seguranca.AcessoNegadoException;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

/** Converte exceções em respostas RFC 9457 (application/problem+json). */
@RestControllerAdvice
class TratamentoErrosHandler {

    private static final Logger log = LoggerFactory.getLogger(TratamentoErrosHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos informados");
        List<Map<String, String>> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("campo", f.getField(), "mensagem", String.valueOf(f.getDefaultMessage())))
                .toList();
        p.setProperty("campos", campos);
        return p;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail formato(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição malformada", "O corpo ou os parâmetros não estão no formato esperado");
    }

    @ExceptionHandler(NaoEncontradoException.class)
    ProblemDetail naoEncontrado(NaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ProblemDetail acessoNegado(AcessoNegadoException e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail versao(OptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Conflito",
                "O registro foi alterado por outra operação. Atualize a tela e tente novamente");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito",
                "A operação conflita com dados já registrados (por exemplo, identificação duplicada)");
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regra(RegraNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Operação não permitida", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalhe);
        p.setTitle(titulo);
        return p;
    }
}
