package br.com.pibic.agentes;

import java.text.Normalizer;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteSeguranca extends Agent {

    private static final String BAIXO_RISCO = "BAIXO_RISCO";
    private static final String ATENCAO = "ATENCAO";
    private static final String RISCO = "RISCO";

    @Override
    protected void setup() {
        System.out.println("Agente Seguranca iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String textoOriginal = mensagem.getContent();
                    ResultadoAnalise resultado = analisarRisco(textoOriginal);

                    System.out.println("\n[SEGURANCA] Texto analisado:");
                    System.out.println(normalizar(textoOriginal));
                    System.out.println("[SEGURANCA] Nivel de risco: " + resultado.nivelRisco);
                    System.out.println("[SEGURANCA] Confianca: " + resultado.confianca);
                    System.out.println("[SEGURANCA] Metodo: " + resultado.metodo);
                    System.out.println("[SEGURANCA] Categoria: " + resultado.categoria);
                    System.out.println("[SEGURANCA] Justificativa: " + resultado.justificativa);

                    ACLMessage resposta = mensagem.createReply();
                    resposta.setPerformative(ACLMessage.INFORM);

                    /*
                     * Mantemos o retorno simples para preservar compatibilidade com o
                     * AgenteConversacional atual, que espera apenas:
                     * BAIXO_RISCO, ATENCAO ou RISCO.
                     *
                     * Os detalhes ficam registrados no log do AgenteSeguranca.
                     */
                    resposta.setContent(resultado.nivelRisco);

                    send(resposta);
                } else {
                    block();
                }
            }
        });
    }

    private ResultadoAnalise analisarRisco(String textoOriginal) {
        String textoNormalizado = normalizar(textoOriginal);

        if (textoNormalizado.trim().isEmpty()) {
            return new ResultadoAnalise(
                    BAIXO_RISCO,
                    1.0,
                    "entrada_vazia",
                    "sem_conteudo",
                    "Mensagem vazia ou sem conteudo suficiente para analise."
            );
        }

        /*
         * Camada 1:
         * Regras conservadoras para sinais de urgencia.
         * Esta camada vem antes da IA para evitar que uma mensagem critica dependa
         * exclusivamente do provedor externo.
         */
        ResultadoAnalise resultadoRegrasCriticas = analisarRegrasCriticas(textoNormalizado);

        if (resultadoRegrasCriticas != null) {
            return resultadoRegrasCriticas;
        }

        /*
         * Camada 2:
         * Classificacao por IA.
         * A IA avalia contexto e linguagem indireta, algo que regras simples
         * nao conseguem capturar bem.
         */
        ResultadoAnalise resultadoIA = analisarComIA(textoOriginal);

        if (resultadoIA != null && resultadoIA.nivelRisco != null && !resultadoIA.nivelRisco.trim().isEmpty()) {
            return resultadoIA;
        }

        /*
         * Camada 3:
         * Fallback por pontuacao.
         * Usado quando a IA falha, esta sem chave, retorna erro ou responde em
         * formato invalido.
         */
        return analisarPorPontuacao(textoNormalizado);
    }

    private ResultadoAnalise analisarRegrasCriticas(String texto) {
        if (contem(texto, "risco imediato")
                || contem(texto, "emergencia")
                || contem(texto, "urgente")
                || contem(texto, "perigo")
                || contem(texto, "nao estou seguro")
                || contem(texto, "nao me sinto seguro")
                || contem(texto, "preciso de ajuda agora")
                || contem(texto, "preciso de ajuda imediata")
                || contem(texto, "alguem precisa me ajudar agora")
                || contem(texto, "posso me machucar")
                || contem(texto, "medo de fazer algo comigo")) {

            return new ResultadoAnalise(
                    RISCO,
                    1.0,
                    "regras_criticas",
                    "possivel_risco_imediato",
                    "Foram identificados sinais de urgencia ou necessidade de apoio imediato."
            );
        }

        return null;
    }

    private ResultadoAnalise analisarComIA(String textoOriginal) {
        String prompt = montarPromptClassificacao(textoOriginal);

        try {
            System.out.println("[SEGURANCA] Acionando classificador por IA: " + ClienteLLM.obterProvedor());

            String respostaIA = ClienteLLM.gerarResposta(prompt);

            if (respostaIA == null || respostaIA.trim().isEmpty() || respostaIA.contains("Erro")) {
                System.out.println("[SEGURANCA] IA indisponivel ou retornou erro. Usando fallback por regras.");
                return null;
            }

            System.out.println("[SEGURANCA] Resposta bruta da IA:");
            System.out.println(respostaIA);

            ResultadoAnalise resultado = interpretarRespostaIA(respostaIA);

            if (!nivelValido(resultado.nivelRisco)) {
                System.out.println("[SEGURANCA] Nivel retornado pela IA invalido. Usando fallback por regras.");
                return null;
            }

            resultado.metodo = "ia_llm";
            return resultado;

        } catch (Exception e) {
            System.out.println("[SEGURANCA] Falha ao acionar IA: " + e.getMessage());
            return null;
        }
    }

    private String montarPromptClassificacao(String textoOriginal) {
        return "Voce e um classificador de seguranca para um prototipo academico de triagem emocional.\n"
                + "Sua tarefa e classificar a mensagem do usuario em apenas um dos tres niveis:\n\n"

                + "BAIXO_RISCO: preocupacao, estresse, ansiedade, tristeza ou dificuldade emocional sem sinal de urgencia imediata.\n"
                + "ATENCAO: sofrimento emocional intenso, desesperanca, isolamento, pedido de ajuda ou dificuldade importante de lidar com o momento, sem indicacao clara de perigo imediato.\n"
                + "RISCO: indicacao de perigo imediato, inseguranca atual, pedido de ajuda imediata, emergencia ou risco de dano a si ou a outra pessoa.\n\n"

                + "Regras obrigatorias:\n"
                + "- Nao responda como terapeuta.\n"
                + "- Nao de conselhos.\n"
                + "- Nao faca diagnostico.\n"
                + "- Nao descreva detalhes sensiveis.\n"
                + "- Seja conservador: em caso de duvida entre ATENCAO e RISCO, escolha RISCO.\n"
                + "- Em caso de duvida entre BAIXO_RISCO e ATENCAO, escolha ATENCAO.\n"
                + "- Responda somente no formato chave=valor separado por ponto e virgula.\n\n"

                + "Formato obrigatorio da resposta:\n"
                + "nivelRisco=BAIXO_RISCO|ATENCAO|RISCO;confianca=0.00;categoria=texto_curto;justificativa=texto_curto\n\n"

                + "Mensagem do usuario:\n"
                + textoOriginal;
    }

    private ResultadoAnalise interpretarRespostaIA(String respostaIA) {
        String nivelRisco = extrairValor(respostaIA, "nivelRisco");
        String confiancaTexto = extrairValor(respostaIA, "confianca");
        String categoria = extrairValor(respostaIA, "categoria");
        String justificativa = extrairValor(respostaIA, "justificativa");

        double confianca = converterConfianca(confiancaTexto);

        if (nivelRisco == null || nivelRisco.trim().isEmpty()) {
            nivelRisco = BAIXO_RISCO;
        }

        nivelRisco = nivelRisco.trim().toUpperCase();

        if (categoria == null || categoria.trim().isEmpty()) {
            categoria = "nao_informada";
        }

        if (justificativa == null || justificativa.trim().isEmpty()) {
            justificativa = "Classificacao gerada por IA sem justificativa detalhada.";
        }

        return new ResultadoAnalise(
                nivelRisco,
                confianca,
                "ia_llm",
                categoria,
                justificativa
        );
    }

    private ResultadoAnalise analisarPorPontuacao(String texto) {
        int pontosAtencao = 0;

        if (contem(texto, "nao aguento mais")) {
            pontosAtencao++;
        }

        if (contem(texto, "muito mal")) {
            pontosAtencao++;
        }

        if (contem(texto, "sem esperanca")) {
            pontosAtencao++;
        }

        if (contem(texto, "cansado de tudo")) {
            pontosAtencao++;
        }

        if (contem(texto, "sozinho")
                || contem(texto, "sozinha")
                || contem(texto, "isolado")
                || contem(texto, "isolada")) {
            pontosAtencao++;
        }

        if (contem(texto, "triste demais")
                || contem(texto, "muita tristeza")
                || contem(texto, "desanimo forte")) {
            pontosAtencao++;
        }

        if (contem(texto, "ansiedade muito forte")
                || contem(texto, "ansiedade intensa")
                || contem(texto, "panico")
                || contem(texto, "desespero")) {
            pontosAtencao++;
        }

        if (contem(texto, "nao consigo dormir")
                || contem(texto, "sem dormir")
                || contem(texto, "muitos dias sem dormir")) {
            pontosAtencao++;
        }

        if (contem(texto, "nao consigo comer")
                || contem(texto, "sem conseguir comer")) {
            pontosAtencao++;
        }

        if (contem(texto, "preciso de ajuda")
                || contem(texto, "quero ajuda")
                || contem(texto, "nao sei o que fazer")) {
            pontosAtencao++;
        }

        if (pontosAtencao >= 2) {
            return new ResultadoAnalise(
                    ATENCAO,
                    0.70,
                    "fallback_pontuacao",
                    "sofrimento_emocional_relevante",
                    "Foram identificados multiplos sinais de sofrimento emocional."
            );
        }

        if (pontosAtencao == 1) {
            return new ResultadoAnalise(
                    ATENCAO,
                    0.55,
                    "fallback_pontuacao",
                    "sinal_unico_de_atencao",
                    "Foi identificado um sinal que merece atencao preventiva."
            );
        }

        return new ResultadoAnalise(
                BAIXO_RISCO,
                0.80,
                "fallback_pontuacao",
                "apoio_inicial",
                "Nao foram identificados sinais de urgencia ou sofrimento intenso."
        );
    }

    private boolean nivelValido(String nivel) {
        if (nivel == null) {
            return false;
        }

        String valor = nivel.trim().toUpperCase();

        return valor.equals(BAIXO_RISCO)
                || valor.equals(ATENCAO)
                || valor.equals(RISCO);
    }

    private double converterConfianca(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0.0;
        }

        try {
            String valor = texto.trim().replace(",", ".");
            double numero = Double.parseDouble(valor);

            if (numero < 0.0) {
                return 0.0;
            }

            if (numero > 1.0) {
                return 1.0;
            }

            return numero;
        } catch (Exception e) {
            return 0.0;
        }
    }

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

    private boolean contem(String texto, String termo) {
        return texto.contains(normalizar(termo));
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        String textoNormalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        textoNormalizado = textoNormalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        return textoNormalizado.toLowerCase().trim();
    }

    private static class ResultadoAnalise {
        String nivelRisco;
        double confianca;
        String metodo;
        String categoria;
        String justificativa;

        ResultadoAnalise(String nivelRisco, double confianca, String metodo, String categoria, String justificativa) {
            this.nivelRisco = nivelRisco;
            this.confianca = confianca;
            this.metodo = metodo;
            this.categoria = categoria;
            this.justificativa = justificativa;
        }
    }
}