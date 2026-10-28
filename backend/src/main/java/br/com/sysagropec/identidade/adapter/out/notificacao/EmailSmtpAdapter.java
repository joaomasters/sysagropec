package br.com.sysagropec.identidade.adapter.out.notificacao;

import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.NotificacaoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class EmailSmtpAdapter implements NotificacaoPort {

    private final JavaMailSender mailSender;
    private final String remetente;

    EmailSmtpAdapter(JavaMailSender mailSender, @Value("${sysagropec.email.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public void enviarEmail(String destinatario, String assunto, String corpo) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);
        mailSender.send(mensagem);
    }
}
