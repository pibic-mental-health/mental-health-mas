# Documentação Técnica — Melhorias no AgenteSeguranca

**Projeto:** `mental-health-mas`  
**Módulo:** `AgenteSeguranca.java`  
**Pacote:** `br.com.pibic.agentes`  
**Objetivo:** evoluir o agente de uma lógica simples por palavras-chave para uma arquitetura híbrida de classificação de risco e preservação da vida.

---

## 1. Visão geral

O `AgenteSeguranca` é responsável por analisar a mensagem recebida do usuário e classificar o nível de risco antes que o `AgenteConversacional` gere qualquer resposta.

Na versão inicial, o agente utilizava apenas verificações simples com `contains()`, buscando termos específicos no texto. Essa abordagem funcionava como prova de conceito, mas era limitada, pois mensagens emocionais podem aparecer com diferentes formas de escrita, gírias, erros de digitação, indiretas e contextos ambíguos.

A nova versão transforma o `AgenteSeguranca` em um **classificador híbrido**, combinando:

```text
1. Regras críticas conservadoras
2. Classificação contextual por IA
3. Fallback por pontuação
```

Essa evolução torna o sistema mais robusto, mais seguro e mais adequado para uma arquitetura de preservação da vida.

---

## 2. Problema da abordagem anterior

A versão anterior seguia uma lógica baseada apenas em termos fixos:

```java
if (texto.contains("...")) {
    return "RISCO";
}
```

Essa estratégia possui limitações importantes:

```text
- baixa capacidade de generalização;
- dificuldade para interpretar mensagens indiretas;
- dependência de frases exatas;
- pouca flexibilidade para variações de linguagem;
- ausência de nível de confiança;
- ausência de justificativa técnica;
- dificuldade de defesa acadêmica em um domínio sensível.
```

Por isso, a nova versão foi estruturada para analisar a mensagem em camadas.

---

## 3. Nova arquitetura do AgenteSeguranca

A nova arquitetura do agente segue o modelo:

```text
Mensagem do usuário
    ↓
Normalização do texto
    ↓
Camada 1 — Regras críticas conservadoras
    ↓
Camada 2 — Classificador por IA
    ↓
Camada 3 — Fallback por pontuação
    ↓
Retorno do nível de risco
```

O retorno para o `AgenteConversacional` permanece compatível com o fluxo atual:

```text
BAIXO_RISCO
ATENCAO
RISCO
```

Porém, internamente, o agente agora trabalha com uma estrutura mais completa:

```text
nivelRisco
confianca
metodo
categoria
justificativa
```

---

## 4. Níveis de risco

O agente trabalha com três níveis principais.

### 4.1 BAIXO_RISCO

Representa mensagens de preocupação, estresse, tristeza ou desconforto emocional sem indicação de urgência.

Fluxo permitido:

```text
- IA conversacional autorizada
- conteúdos educativos autorizados
- psicólogos demonstrativos autorizados
- locais de atendimento autorizados
- monitoramento simulado autorizado
```

### 4.2 ATENCAO

Representa sofrimento emocional mais intenso, pedido de ajuda, sensação de sobrecarga ou sinais que merecem resposta mais cuidadosa.

Fluxo recomendado:

```text
- IA conversacional pode ser usada com cautela
- resposta mais direta e acolhedora
- incentivo a apoio profissional
- locais de atendimento priorizados
- botão ou ação de apoio pode ser exibido
- monitoramento real não deve ser apresentado
```

### 4.3 RISCO

Representa situação em que o sistema deve priorizar preservação da vida e apoio imediato.

Fluxo recomendado:

```text
- não chamar resposta comum da IA
- não oferecer conteúdo educativo como primeira resposta
- não iniciar monitoramento
- acionar protocolo de preservação da vida
- indicar apoio imediato
- permitir exibição de locais de atendimento
- registrar evento na memória
```

---

## 5. Camada 1 — Regras críticas conservadoras

A primeira camada do agente é uma barreira de segurança.

Ela identifica sinais de urgência que não devem depender exclusivamente da IA ou de um provedor externo.

Essa camada é executada antes da classificação por IA.

Objetivo:

```text
Garantir que mensagens com indicação de urgência sejam classificadas rapidamente como RISCO.
```

Vantagens:

```text
- resposta imediata;
- não depende de internet;
- não depende de API externa;
- evita falha silenciosa da IA em situações críticas;
- prioriza segurança do usuário.
```

Essa camada é intencionalmente conservadora. Em um domínio sensível, é melhor encaminhar uma situação duvidosa para um protocolo de segurança do que tratá-la como uma conversa comum.

---

## 6. Camada 2 — Classificação por IA

A segunda camada utiliza o `ClienteLLM`, que atualmente permite integração com o provedor configurado no projeto.

Fluxo:

```text
AgenteSeguranca
    ↓
ClienteLLM
    ↓
Provedor de IA configurado
    ↓
Resposta estruturada
    ↓
Interpretação pelo AgenteSeguranca
```

O objetivo da IA nesse ponto não é conversar com o usuário, mas apenas classificar a mensagem.

O prompt deixa isso claro:

```text
- não responder como terapeuta;
- não dar conselhos;
- não diagnosticar;
- não descrever detalhes sensíveis;
- retornar apenas uma classificação estruturada.
```

Formato esperado da resposta da IA:

```text
nivelRisco=BAIXO_RISCO|ATENCAO|RISCO;
confianca=0.00;
categoria=texto_curto;
justificativa=texto_curto
```

Essa estrutura permite que o agente tenha uma classificação mais explicável e auditável.

---

## 7. Camada 3 — Fallback por pontuação

Caso a IA falhe, esteja indisponível, retorne erro ou gere uma resposta inválida, o agente usa um fallback por pontuação.

Essa camada avalia sinais gerais de sofrimento emocional e calcula uma pontuação preventiva.

Exemplo de decisão:

```text
0 pontos → BAIXO_RISCO
1 ponto  → ATENCAO preventiva
2+ pontos → ATENCAO
```

O fallback garante que o sistema continue funcionando mesmo sem IA.

Vantagens:

```text
- mantém o sistema operacional em caso de falha externa;
- reduz dependência total da API;
- permite testes locais;
- melhora a robustez da arquitetura;
- facilita explicação acadêmica.
```

---

## 8. Normalização do texto

A nova versão normaliza a mensagem antes da análise.

O método `normalizar()` executa:

```text
- remoção de acentos;
- conversão para minúsculas;
- remoção de espaços extras.
```

Isso melhora a comparação de termos e reduz falhas causadas por acentuação ou variações simples de escrita.

Exemplo conceitual:

```text
"Não consigo lidar com isso"
→ "nao consigo lidar com isso"
```

---

## 9. Estrutura ResultadoAnalise

A nova versão inclui uma classe interna chamada `ResultadoAnalise`.

Ela armazena:

```text
nivelRisco
confianca
metodo
categoria
justificativa
```

Exemplo conceitual:

```text
nivelRisco=ATENCAO
confianca=0.70
metodo=fallback_pontuacao
categoria=sofrimento_emocional_relevante
justificativa=Foram identificados multiplos sinais de sofrimento emocional.
```

Essa estrutura é importante porque permite evoluir o agente futuramente para retornar dados mais completos ao `AgenteIntervencao`.

---

## 10. Compatibilidade com o fluxo atual

Mesmo com a análise interna mais completa, o agente ainda responde ao `AgenteConversacional` apenas com:

```text
BAIXO_RISCO
ATENCAO
RISCO
```

Isso foi feito para não quebrar o sistema atual.

O código mantém:

```java
resposta.setContent(resultado.nivelRisco);
```

Os detalhes adicionais aparecem no log:

```text
[SEGURANCA] Nivel de risco
[SEGURANCA] Confianca
[SEGURANCA] Metodo
[SEGURANCA] Categoria
[SEGURANCA] Justificativa
```

Essa decisão permite evolução incremental da arquitetura.

---

## 11. Integração com AgenteIntervencao

O `AgenteSeguranca` não decide sozinho o que fazer com o usuário.

Ele apenas classifica o risco.

A decisão de conduta fica com o `AgenteIntervencao`.

Fluxo atualizado:

```text
Mensagem do usuário
    ↓
AgenteSeguranca
    ↓
nível de risco
    ↓
AgenteIntervencao
    ↓
protocolo
    ↓
AgenteConversacional
```

Essa separação é importante porque cria uma arquitetura mais ética e organizada:

```text
classificar risco ≠ intervir
```

O `AgenteSeguranca` classifica.  
O `AgenteIntervencao` decide o protocolo.  
O `AgenteConversacional` executa apenas o que foi permitido.

---

## 12. Relação com o Protocolo de Preservação da Vida

A melhoria do `AgenteSeguranca` fortalece o protocolo de preservação da vida.

Quando o agente retorna `RISCO`, o sistema deve:

```text
- bloquear resposta comum da IA;
- evitar conteúdos genéricos como primeira resposta;
- não iniciar monitoramento simulado;
- apresentar orientação de apoio imediato;
- permitir busca por locais de atendimento;
- destacar ação de contato de apoio, como telefone configurado no protótipo;
- registrar o evento na memória.
```

Esse comportamento reduz o risco de o sistema responder de maneira inadequada em situações sensíveis.

---

## 13. Benefícios das melhorias

As principais melhorias foram:

```text
1. Substituição da lógica simples por palavras-chave por uma arquitetura híbrida.
2. Criação de camadas de segurança.
3. Uso de IA apenas como classificador, não como terapeuta.
4. Inclusão de fallback por pontuação.
5. Normalização textual.
6. Inclusão de confiança, método, categoria e justificativa.
7. Compatibilidade com o fluxo já implementado.
8. Integração conceitual com o AgenteIntervencao.
9. Melhor defesa acadêmica do sistema.
10. Maior foco na preservação da vida.
```

---

## 14. Limitações da nova versão

Apesar das melhorias, o agente ainda possui limitações:

```text
- não substitui avaliação profissional;
- não deve ser usado em ambiente real sem validação ética;
- a IA pode errar classificações;
- o fallback por pontuação ainda é simplificado;
- as regras críticas precisam ser revisadas por profissionais;
- o sistema não deve coletar dados reais sem aprovação do Comitê de Ética;
- o retorno ainda é simples para manter compatibilidade.
```

Em uma versão futura, seria adequado envolver profissionais da área para validar categorias, mensagens, protocolos e limites do sistema.

---

## 15. Possíveis evoluções futuras

Evoluções recomendadas:

```text
1. Criar ClienteClassificadorRisco separado do ClienteLLM.
2. Retornar JSON estruturado para o AgenteIntervencao.
3. Salvar histórico de risco no AgenteMemoria.
4. Adicionar testes automatizados com mensagens simuladas.
5. Criar matriz de risco validada por especialistas.
6. Incluir logs auditáveis sem armazenar dados sensíveis desnecessários.
7. Adicionar modo offline com regras mais completas.
8. Implementar interface com botão de ligação em ambiente web ou mobile.
```

---

## 16. Resumo para apresentação

A melhoria do `AgenteSeguranca` evolui o sistema de uma abordagem simples por palavras-chave para um classificador híbrido de risco. A nova versão combina regras críticas conservadoras, classificação contextual por IA e fallback por pontuação. Com isso, o agente passa a ser mais robusto, explicável e seguro. Ele continua retornando apenas `BAIXO_RISCO`, `ATENCAO` ou `RISCO` para manter compatibilidade com o restante do sistema, mas registra internamente confiança, método, categoria e justificativa. Essa evolução fortalece o protocolo de preservação da vida e separa corretamente a classificação de risco da decisão de intervenção.
