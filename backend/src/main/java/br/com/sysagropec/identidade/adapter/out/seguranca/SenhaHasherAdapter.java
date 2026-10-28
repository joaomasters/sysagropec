package br.com.sysagropec.identidade.adapter.out.seguranca;

import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.SenhaHasherPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class SenhaHasherAdapter implements SenhaHasherPort {

    private final PasswordEncoder encoder;
    /** Hash descartável para equalizar o tempo de resposta quando o usuário não existe. */
    private final String hashFicticio;

    SenhaHasherAdapter(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.hashFicticio = encoder.encode("hash-ficticio-para-tempo-constante");
    }

    @Override
    public String gerarHash(String senha) {
        return encoder.encode(senha);
    }

    @Override
    public boolean confere(String senha, String hash) {
        if (hash == null) {
            encoder.matches(senha, hashFicticio);
            return false;
        }
        return encoder.matches(senha, hash);
    }
}
