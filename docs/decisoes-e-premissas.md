# Decisões, premissas e pontos a validar

## Decisões técnicas tomadas

| Decisão | Motivo |
|---|---|
| Spring Boot **4.1.x**, em vez de 3.x | O suporte gratuito da linha 3.x terminou (3.5 em 30/06/2026). A 4.1 tem suporte gratuito até 31/07/2027 e roda com Java 21 |
| Sessão no servidor (Spring Session JDBC), não JWT | Revogação imediata e sobrevivência a reinícios, sem infraestrutura extra. O SPA é servido na mesma origem |
| Administrador como atributo do usuário na organização, não perfil por propriedade | O administrador acessa todas as propriedades. Os demais perfis valem por propriedade |
| E-mail de login único em todo o sistema | Simplifica o login. Um mesmo e-mail em várias organizações exigiria escolher a organização no login (fora do MVP) |
| Recursos fora do escopo respondem 404 | Evita revelar a existência de dados de outras organizações ou propriedades |
| Lançamentos retroativos rejeitados (409) | Não reescrever o histórico silenciosamente. Uma correção retroativa controlada fica para uma fase posterior |
| Idempotência por `Idempotency-Key` | Duplo clique e reenvio em conexão instável não duplicam animais nem pesagens |
| Tarefa de pesagem conclui pelo registro da pesagem | Evita pesagem duplicada e conclusão sem evidência |

## Premissas de negócio (reversíveis)

- **Animais e identificação:**
  - Brinco obrigatório no recebimento. Identificador eletrônico opcional.
  - Unicidade entre animais ativos da organização.
- **Lotes e currais:**
  - Lote (manejo) e instalação (local físico) são vínculos independentes.
  - Um curral pode abrigar vários lotes.
  - A lotação é medida em cabeças, somando todos os lotes.
  - Instalação sem capacidade cadastrada não tem limite: a taxa de ocupação aparece como "capacidade não definida".
  - Depois do recebimento, o animal pode ficar sem lote. Ele sempre tem uma localização.
- **Categorias:** bezerro, bezerra, garrote, novilha, boi, vaca e touro. O sexo precisa ser compatível com a categoria.
- **Pesagem:**
  - Balança implica peso medido. Fita barimétrica ou avaliação visual implicam peso estimado. "Outro" exige informar a natureza.
  - No máximo uma pesagem de entrada válida por animal. Para corrigir, cancela-se a anterior com motivo.
  - Peso entre 0 e 2.000 kg, com até 2 casas decimais.
- **Datas e horários:**
  - Tolerância de 5 minutos para "data futura", por diferenças de relógio entre dispositivo e servidor.
  - O dia de "Rotinas de hoje" e do dashboard segue o fuso da propriedade.
  - A digitação de data e hora usa o fuso do dispositivo do usuário.
- **Tarefas:** qualquer usuário com permissão de execução pode concluir uma tarefa, não só o responsável. A autoria fica registrada.

## Indicadores

| Indicador | Situação nesta etapa |
|---|---|
| Animais ativos, sem lote, por categoria, ocupação | Contagem direta dos dados persistidos |
| Pesagens (30 dias) | Contagem das pesagens não canceladas, separando medidas de estimadas |
| Ganho de peso, consumo, conversão, custos | **Não calculados.** O dashboard mostra "indisponível". Antes de implementar, documentar fórmula, unidade, período, população e dados mínimos (seção 7 do escopo) |

## Validar com o produtor ou o responsável técnico

1. Padrão de identificação: brinco visual, SISBOV ou eletrônico. Qual é obrigatório? Há formato definido?
2. Lista de categorias e raças. A GTA é obrigatória no recebimento?
3. Compartilhamento de curral entre lotes. A capacidade é por cabeças ou por área (m²/cabeça)?
4. Até quando lançamentos retroativos são aceitos e quem pode corrigi-los.
5. Matriz de permissões por perfil. Hoje o operador movimenta animais, pesa e executa tarefas, mas não recebe animais.
6. Pesagem: exigência de jejum, equipamento padrão e se haverá pesagem coletiva (média do lote).
7. Critérios do ganho médio diário: intervalo mínimo entre pesagens e se estimativas entram no cálculo.
8. Responsável por tarefa: hoje só o administrador escolhe o responsável na tela de criação.
