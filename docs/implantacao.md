# Implantação, backup e restauração

Procedimento de referência para um servidor (VPS ou máquina virtual na nuvem) com Docker.
O procedimento ainda não foi executado em produção: valide em um ambiente de homologação antes de usar com dados reais.

## Requisitos

- Linux com Docker Engine e o plugin Compose.
- Um domínio apontando para o servidor, para o HTTPS.
- Um serviço SMTP real (convites e redefinição de senha).

## Configuração

1. Copie o repositório para o servidor e crie o `.env` a partir de `.env.example`:
   - `SYSAGROPEC_DB_SENHA`: senha forte e exclusiva.
   - `SYSAGROPEC_ADMIN_EMAIL` e `SYSAGROPEC_ADMIN_SENHA`: somente para o primeiro acesso. Remova a senha do `.env` depois.
   - Deixe `SPRING_PROFILES_ACTIVE` **vazio**. Nunca use `demo` com dados reais.
2. No serviço `backend`, ajuste estas variáveis do `docker-compose.yml`:
   - `SYSAGROPEC_SMTP_HOST`, `SYSAGROPEC_SMTP_PORTA`, `SYSAGROPEC_SMTP_USUARIO`, `SYSAGROPEC_SMTP_SENHA` e `SYSAGROPEC_EMAIL_REMETENTE`;
   - `SYSAGROPEC_URL_FRONTEND=https://seu-dominio`;
   - `SYSAGROPEC_COOKIE_SEGURO=true` (cookie de sessão só em HTTPS);
   - `SYSAGROPEC_OPENAPI=false` (desliga o Swagger em produção).
3. Remova o serviço `mailpit` do Compose de produção.

## HTTPS

Coloque um proxy reverso com TLS na frente do serviço `frontend` (porta 8081 local). Por exemplo, Caddy:

```
seu-dominio {
    reverse_proxy 127.0.0.1:8081
}
```

O Caddy obtém e renova o certificado automaticamente. As portas do Compose ficam presas em `127.0.0.1`: só o proxy fica exposto.

## Subir e atualizar

```bash
docker compose up -d --build
docker compose logs -f backend    # logs estruturados (ECS/JSON) com requestId
```

- As migrações do Flyway rodam na inicialização. Uma migração com erro impede o backend de subir, e o banco não fica em estado parcial.
- Saúde da aplicação: `GET /actuator/health/readiness`.

## Backup

Backup lógico diário com retenção de 14 dias. Exemplo de `cron` no servidor:

```bash
0 3 * * * cd /opt/sysagropec && docker compose exec -T postgres \
  pg_dump -U sysagropec -d sysagropec -Fc > /backup/sysagropec-$(date +\%F).dump \
  && find /backup -name 'sysagropec-*.dump' -mtime +14 -delete
```

- Copie `/backup` para outro local (outra região ou armazenamento S3-compatível). Um backup no mesmo disco não protege contra perda do servidor.
- Objetivos iniciais sugeridos, a confirmar com o produtor:
  - RPO de 24 horas (perda máxima de dados);
  - RTO de 4 horas (tempo para voltar a operar).
- Para um RPO menor, use arquivamento contínuo de WAL ou um PostgreSQL gerenciado.

## Restauração (testar periodicamente)

```bash
docker compose stop backend
docker compose exec -T postgres dropdb -U sysagropec sysagropec
docker compose exec -T postgres createdb -U sysagropec sysagropec
docker compose exec -T postgres pg_restore -U sysagropec -d sysagropec < /backup/sysagropec-AAAA-MM-DD.dump
docker compose start backend
```

Depois de restaurar:

1. Confirme que o login funciona.
2. Confira a contagem de animais ativos no dashboard.
3. Verifique em `flyway_schema_history` que a versão das migrações é a esperada.
