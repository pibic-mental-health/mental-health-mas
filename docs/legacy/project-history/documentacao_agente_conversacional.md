# Documentação Técnica — AgenteConversacional

**Projeto:** `mental-health-mas`  
**Módulo:** `AgenteConversacional.java`  
**Pacote:** `br.com.pibic.agentes`  
**Tecnologia:** Java + JADE  
**Finalidade:** orquestração da conversa, integração com agentes auxiliares e geração de resposta segura com apoio de IA.

---

## 1. Visão geral

O `AgenteConversacional` é o agente responsável por coordenar a interação principal entre o usuário simulado e os demais agentes do sistema multiagente.

Ele atua como o **orquestrador central da conversa**, recebendo mensagens do `AgentePaciente`, consultando agentes especializados e gerando uma resposta final com apoio de IA.

O agente não realiza diagnóstico, tratamento ou acompanhamento psicológico real. Sua função é organizar informações iniciais, oferecer apoio textual, acionar módulos complementares e reforçar que a plataforma possui finalidade acadêmica e demonstrativa.

---

## 2. Papel do agente no sistema

O `AgenteConversacional` fica no centro do fluxo do sistema.

Fluxo resumido:

```text
AgentePaciente
    ↓
AgenteConversacional
    ↓
AgenteSeguranca
    ↓
AgenteMemoria
    ↓
ClienteLLM / ClienteNvidia
    ↓
AgenteConteudo
    ↓
AgentePsicologo
    ↓
AgenteLocalAtendimento
    ↓
AgenteMonitoramento
    ↓
AgenteRelatorio
```

Responsabilidades principais:

- receber a mensagem inicial do paciente simulado;
- extrair perfil e mensagem do usuário;
- salvar informações na memória;
- consultar o nível de risco;
- consultar histórico resumido da memória;
- montar prompt seguro para a IA;
- gerar resposta empática;
- limpar termos clínicos da resposta;
- oferecer próximos módulos do sistema;
- tratar respostas do usuário para conteúdo, psicólogo, locais e monitoramento;
- receber relatório simulado e encaminhar ao paciente.

---

## 3. Inicialização do agente

O agente é iniciado pelo método `setup()`.

Ao iniciar, ele imprime no terminal:

```text
Agente Conversacional iniciado: agenteConversacional
```

Depois, registra um `CyclicBehaviour`, permitindo que o agente permaneça ativo aguardando novas mensagens.

Estrutura geral:

```java
@Override
protected void setup() {
    System.out.println("Agente Conversacional iniciado: " + getLocalName());

    addBehaviour(new CyclicBehaviour() {
        @Override
        public void action() {
            ACLMessage mensagem = receive();

            if (mensagem != null) {
                // tratamento da mensagem
            } else {
                block();
            }
        }
    });
}
```

---

## 4. Entrada principal do agente

O agente recebe mensagens no formato textual com pares `chave=valor`, separados por ponto e vírgula.

Exemplo de entrada principal:

```text
perfil=MISTO;mensagem=Hoje estou muito preocupado e com dificuldade para relaxar.
```

Campos principais:

| Campo | Descrição |
|---|---|
| `perfil` | Perfil identificado pelo agente de triagem |
| `mensagem` | Mensagem atual do usuário |
| `tipo` | Indica mensagens especiais, como relatório simulado |
| `respostaConteudo` | Resposta do usuário sobre receber conteúdos |
| `respostaPsicologo` | Resposta do usuário sobre ver psicólogos |
| `respostaLocalAtendimento` | Resposta do usuário sobre ver locais |
| `respostaMonitoramento` | Resposta do usuário sobre simulação de monitoramento |

---

## 5. Roteamento de mensagens

O primeiro passo do agente é identificar o tipo de mensagem recebida.

A ordem de tratamento é:

```text
1. Relatório simulado
2. Resposta sobre monitoramento
3. Resposta sobre conteúdo
4. Resposta sobre psicólogo
5. Resposta sobre locais de atendimento
6. Mensagem comum do usuário
```

Essa ordem evita que o agente trate uma resposta específica como se fosse uma nova mensagem comum.

Trecho conceitual:

```java
String tipo = extrairValor(conteudo, "tipo");

if (tipo.equalsIgnoreCase("relatorio_simulado")) {
    tratarRelatorioSimulado(conteudo);
    return;
}

String respostaMonitoramento = extrairValor(conteudo, "respostaMonitoramento");
String respostaConteudo = extrairValor(conteudo, "respostaConteudo");
String respostaPsicologo = extrairValor(conteudo, "respostaPsicologo");
String respostaLocalAtendimento = extrairValor(conteudo, "respostaLocalAtendimento");
```

---

## 6. Fluxo da mensagem comum

Quando o agente recebe uma mensagem comum do usuário, ele segue o fluxo abaixo:

```text
1. Extrai perfil e mensagem
2. Salva perfil na memória
3. Salva mensagem na memória
4. Consulta o AgenteSeguranca
5. Salva o risco na memória
6. Se risco for RISCO, retorna resposta de segurança
7. Se risco não for RISCO, consulta memória
8. Monta prompt para IA
9. Chama ClienteLLM
10. Limpa termos clínicos da resposta
11. Usa fallback se houver erro
12. Adiciona avisos e convites
13. Salva resposta na memória
14. Envia resposta ao paciente
```

Fluxo visual:

```text
Mensagem do usuário
    ↓
Extrair perfil e texto
    ↓
Salvar na memória
    ↓
Consultar segurança
    ↓
RISCO?
    ├── Sim → respostaSeguranca()
    └── Não → consultarMemoria()
                ↓
             montarPrompt()
                ↓
             ClienteLLM.gerarResposta()
                ↓
             limparRespostaClinica()
                ↓
             adicionar convites
                ↓
             responder paciente
```

---

## 7. Integração com AgenteMemoria

O `AgenteConversacional` utiliza o `AgenteMemoria` para armazenar e recuperar informações importantes.

Informações salvas:

```text
perfil
mensagem
risco
resposta
conteudo
psicologo
local_atendimento
monitoramento_simulado
relatorio_simulado
```

Método utilizado para salvar:

```java
private void salvarNaMemoria(String tipo, String valor) {
    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));
    msg.setContent("tipo=" + tipo + ";valor=" + valor);
    send(msg);
}
```

Consulta de memória:

```java
private String consultarMemoria() {
    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
    msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));
    msg.setContent("tipo=consulta");

    send(msg);

    ACLMessage resposta = blockingReceive();

    if (resposta != null) {
        return resposta.getContent();
    }

    return "";
}
```

A memória permite que o agente gere uma resposta mais contextualizada, usando nome, perfil, risco e histórico.

---

## 8. Integração com AgenteSeguranca

Antes de gerar uma resposta com IA, o agente consulta o `AgenteSeguranca`.

Método:

```java
private String consultarSeguranca(String mensagemUsuario) {
    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
    msg.addReceiver(new AID("agenteSeguranca", AID.ISLOCALNAME));
    msg.setContent(mensagemUsuario);

    send(msg);

    ACLMessage resposta = blockingReceive();

    if (resposta != null) {
        return resposta.getContent();
    }

    return "BAIXO_RISCO";
}
```

Possíveis retornos:

```text
BAIXO_RISCO
ATENCAO
RISCO
```

Se o retorno for `RISCO`, o agente não chama a IA comum. Ele envia uma resposta fixa de segurança.

---

## 9. Resposta de segurança

Quando o risco é classificado como `RISCO`, o agente usa uma resposta segura e direta.

```java
private String respostaSeguranca() {
    return "Sinto muito que voce esteja passando por um momento tao dificil.\n"
            + "Esta plataforma nao substitui ajuda profissional. Procure imediatamente um profissional de saude, um servico de emergencia ou alguem de confianca que possa estar com voce agora.\n"
            + "Voce nao precisa lidar com isso sozinho(a).";
}
```

Essa resposta evita aprofundar detalhes sensíveis e direciona a pessoa para apoio profissional ou emergência.

---

## 10. Integração com ClienteLLM

Quando o risco não é `RISCO`, o agente chama o módulo `ClienteLLM`.

Trecho principal:

```java
System.out.println("[CONVERSACIONAL] Provedor de IA configurado: " + ClienteLLM.obterProvedor());
resposta = ClienteLLM.gerarResposta(prompt);
resposta = limparRespostaClinica(resposta);
```

O `ClienteLLM` permite abstrair o provedor de IA. Atualmente, o fluxo utiliza o provedor:

```text
NVIDIA_NIM
```

Isso permite alterar o provedor sem modificar diretamente o `AgenteConversacional`.

---

## 11. Prompt seguro da IA

O método `montarPrompt()` cria as instruções enviadas para o modelo de linguagem.

Objetivos do prompt:

- definir que o sistema é um protótipo acadêmico;
- impedir diagnóstico;
- impedir indicação de tratamento;
- evitar termos clínicos fortes;
- reforçar apoio inicial;
- orientar resposta breve e acolhedora;
- incentivar busca profissional quando fizer sentido.

Regras principais do prompt:

```text
- Use o nome do usuário se estiver disponível.
- Seja acolhedor, simples, breve e humano.
- Não afirme diagnósticos.
- Não diga que o usuário possui transtorno, doença ou condição clínica.
- Não diga que está tratando o usuário.
- Não prometa melhora.
- Não prometa acompanhamento clínico.
- Evite termos clínicos fortes.
- Prefira termos como sinais, momento de estresse, sobrecarga emocional, preocupação e apoio inicial.
- Reforce que a plataforma não substitui acompanhamento psicológico profissional.
```

---

## 12. Limpeza de termos clínicos

Mesmo com um prompt seguro, modelos de linguagem podem gerar termos com tom clínico. Para reduzir esse risco, o agente possui o método:

```java
private String limparRespostaClinica(String resposta)
```

Esse método substitui expressões clínicas por termos mais seguros.

Exemplos:

| Termo original | Termo substituído |
|---|---|
| `sintomas de um desequilíbrio emocional` | `sinais de um momento de estresse ou sobrecarga emocional` |
| `diagnóstico` | `triagem inicial` |
| `tratamento` | `apoio profissional` |
| `transtorno de ansiedade` | `momento de ansiedade ou preocupação intensa` |
| `doença mental` | `questão de saúde emocional` |

Esse filtro ajuda a manter o sistema mais adequado para contexto acadêmico e demonstrativo.

---

## 13. Resposta fallback

Se a IA retornar erro, vazio ou uma resposta inválida, o agente usa uma resposta fallback.

Condição:

```java
if (resposta == null || resposta.trim().isEmpty() || resposta.contains("Erro")) {
    resposta = gerarRespostaFallback(perfil, mensagemUsuario, risco);
}
```

Perfis tratados:

```text
ANSIEDADE
DEPRESSAO
MISTO
GERAL
```

Exemplo para perfil `MISTO`:

```java
return "Percebo que seu formulario trouxe diferentes sinais de preocupacao e sobrecarga emocional. Podemos conversar com calma, mas uma avaliacao adequada deve ser feita por um profissional.";
```

---

## 14. Convites para módulos complementares

Após gerar a resposta inicial, o agente adiciona convites para outros módulos.

Convites adicionados:

```text
1. Sugestões de conteúdo
2. Indicações de psicólogos cadastrados
3. Locais demonstrativos de atendimento
4. Simulação acadêmica de acompanhamento semanal
```

Exemplo:

```text
Posso sugerir alguns conteudos de apoio, como meditacao, respiracao, musica ou textos educativos. Deseja receber sugestoes de conteudo? (sim/nao)
```

Esses convites acionam outros agentes dependendo da resposta do paciente simulado.

---

## 15. Tratamento da resposta sobre conteúdo

Quando o usuário responde sobre receber conteúdos, o agente executa:

```java
private void tratarRespostaConteudo(...)
```

Se a resposta for `sim`:

```text
1. Consulta o AgenteConteudo
2. Salva aceite na memória
3. Envia sugestões ao paciente
```

Se a resposta for diferente de `sim`:

```text
1. Salva recusa na memória
2. Informa que os conteúdos podem ser acessados em outro momento
```

Consulta ao agente:

```java
msg.addReceiver(new AID("agenteConteudo", AID.ISLOCALNAME));
msg.setContent("perfil=" + perfil + ";mensagem=" + mensagemUsuario);
```

---

## 16. Tratamento da resposta sobre psicólogo

Quando o usuário aceita ver psicólogos, o agente executa:

```java
private void tratarRespostaPsicologo(...)
```

Se a resposta for `sim`:

```text
1. Consulta o AgentePsicologo
2. Salva aceite na memória
3. Retorna indicações demonstrativas
```

Consulta:

```java
msg.addReceiver(new AID("agentePsicologo", AID.ISLOCALNAME));
msg.setContent("perfil=" + perfil + ";risco=" + risco);
```

A sugestão de psicólogo é feita quando:

```java
private boolean deveSugerirPsicologo(String perfil, String risco)
```

Esse método retorna verdadeiro para:

```text
risco ATENCAO
risco RISCO
perfil ANSIEDADE
perfil DEPRESSAO
perfil MISTO
```

---

## 17. Tratamento da resposta sobre locais de atendimento

Quando o usuário aceita visualizar locais, o agente executa:

```java
private void tratarRespostaLocalAtendimento(...)
```

O agente extrai:

```text
cidade
uf
```

Valores padrão:

```text
Brasilia
DF
```

Consulta:

```java
msg.addReceiver(new AID("agenteLocalAtendimento", AID.ISLOCALNAME));
msg.setContent("cidade=" + cidade + ";uf=" + uf);
```

O `AgenteLocalAtendimento` então busca locais por estratégia híbrida:

```text
Google Places, se configurado
OpenStreetMap / Overpass
CNES / Dados Abertos SUS
Fallback local
```

---

## 18. Tratamento da resposta sobre monitoramento

Quando o usuário aceita visualizar a simulação de acompanhamento, o agente executa:

```java
private void tratarRespostaMonitoramento(...)
```

Se a resposta for `sim`, ele envia mensagem ao `AgenteMonitoramento`:

```java
msgMonitoramento.setContent("iniciar=true;perfil=" + perfil + ";modo=simulado");
```

Também salva na memória:

```text
tipo=monitoramento_simulado;valor=aceito
```

A resposta enviada ao paciente reforça o caráter acadêmico:

```text
Perfeito. Vou iniciar uma simulacao academica de acompanhamento semanal. Esses dados sao demonstrativos e nao representam avaliacao clinica.
```

---

## 19. Tratamento do relatório simulado

Quando o `AgenteRelatorio` envia o resultado da simulação, o `AgenteConversacional` identifica:

```text
tipo=relatorio_simulado
```

E executa:

```java
private void tratarRelatorioSimulado(String conteudo)
```

Esse método:

```text
1. Extrai o perfil
2. Extrai o relatório
3. Salva na memória que o relatório foi gerado
4. Encaminha o relatório ao AgentePaciente
```

Mensagem enviada ao paciente:

```text
Relatorio simulado gerado com sucesso.

[conteúdo do relatório]
```

---

## 20. Método de extração de valores

O agente usa o método `extrairValor()` para ler mensagens no formato `chave=valor`.

```java
private String extrairValor(String texto, String chave) {
    if (texto == null || texto.trim().isEmpty()) {
        return "";
    }

    String[] partes = texto.split(";");

    for (String parte : partes) {
        String[] kv = parte.split("=", 2);

        if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(chave)) {
            return kv[1].trim();
        }
    }

    return "";
}
```

Esse método permite comunicação simples entre agentes sem depender de objetos complexos.

---

## 21. Exemplo de execução validada

No teste executado, o fluxo apresentou:

```text
BUILD SUCCESS
Agente Conversacional iniciado
Perfil recebido: MISTO
Mensagem do usuario: Hoje estou muito preocupado e com dificuldade para relaxar.
Nivel de risco: BAIXO_RISCO
Provedor de IA configurado: NVIDIA_NIM
Resposta gerada
```

A resposta gerada utilizou linguagem segura:

```text
Sinais de estresse e sobrecarga emocional
apoio inicial
não substitui acompanhamento psicológico profissional
```

Isso indica que o prompt e o filtro de limpeza clínica funcionaram corretamente.

---

## 22. Pontos fortes do AgenteConversacional

Principais pontos positivos:

- centraliza a orquestração da conversa;
- usa memória para contexto;
- consulta segurança antes de chamar IA;
- evita resposta comum em caso de risco alto;
- utiliza provedor de IA desacoplado;
- possui fallback em caso de erro da IA;
- inclui limpeza de termos clínicos;
- mantém os avisos éticos no fluxo;
- integra conteúdo, psicólogos, locais e monitoramento;
- reforça o caráter acadêmico da plataforma.

---

## 23. Limitações

Limitações atuais:

1. O fluxo ainda é simulado pelo `AgentePaciente`.
2. As respostas `sim/nao` são automáticas no cenário de teste.
3. O agente depende de texto no formato `chave=valor`.
4. O controle de contexto é simples.
5. A limpeza de termos clínicos é baseada em substituições fixas.
6. O sistema não deve ser usado em ambiente real sem aprovação ética.
7. O agente não substitui análise profissional.

---

## 24. Considerações éticas

O `AgenteConversacional` atua em um domínio sensível: saúde mental.

Por isso, ele foi ajustado para:

- não realizar diagnóstico;
- não afirmar transtornos;
- não indicar tratamento;
- não prometer melhora;
- não simular acompanhamento clínico real;
- reforçar a busca por apoio profissional;
- tratar monitoramento como simulação acadêmica;
- manter respostas acolhedoras e cuidadosas.

Em uma versão real, seria necessário:

```text
aprovação do Comitê de Ética
termo de consentimento
supervisão profissional
política de privacidade
tratamento seguro de dados sensíveis
avaliação de risco por equipe especializada
```

---

## 25. Resumo para apresentação

O `AgenteConversacional` é o componente central da interação do sistema multiagente. Ele recebe a mensagem do usuário, consulta o agente de segurança, utiliza a memória para contextualizar a conversa, aciona um modelo de linguagem por meio do `ClienteLLM` e aplica uma limpeza de termos clínicos para manter a resposta segura. Além disso, ele coordena os módulos de conteúdo, psicólogo, locais de atendimento, monitoramento simulado e relatório. Sua atuação é limitada ao apoio inicial e à triagem emocional acadêmica, sem realizar diagnóstico ou substituir acompanhamento psicológico profissional.
