package br.com.sysagropec.identidade.adapter.in.web;

import br.com.sysagropec.identidade.application.port.in.ContextoExecucaoQuery;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Converte a autenticação do Spring Security no contrato próprio {@link ContextoExecucao},
 * para que o núcleo não dependa do framework de segurança.
 */
@Component
class ContextoExecucaoArgumentResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

    private static final String ATRIBUTO = ContextoExecucao.class.getName();

    private final ContextoExecucaoQuery contextos;

    ContextoExecucaoArgumentResolver(ContextoExecucaoQuery contextos) {
        this.contextos = contextos;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ContextoExecucao.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Object existente = request.getAttribute(ATRIBUTO);
        if (existente instanceof ContextoExecucao ctx) {
            return ctx;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SessaoUsuario sessao)) {
            throw new SessaoInvalidaException();
        }
        ContextoExecucao ctx = contextos.obter(sessao.usuarioId())
                .filter(c -> c.organizacaoId().equals(sessao.organizacaoId()))
                .orElseThrow(() -> {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    return new SessaoInvalidaException();
                });
        request.setAttribute(ATRIBUTO, ctx);
        MDC.put("usuarioId", ctx.usuarioId().toString());
        return ctx;
    }

    static class SessaoInvalidaException extends RuntimeException {
        SessaoInvalidaException() {
            super("Sessão inválida ou expirada");
        }
    }
}
