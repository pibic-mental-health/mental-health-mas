package br.com.pibic.agentes;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteSeguranca extends Agent {

    private static final String BAIXO_RISCO = "BAIXO_RISCO";
    private static final String ATENCAO = "ATENCAO";
    private static final String RISCO = "RISCO";

    private static final int LIMITE_CONTEXTO_SEGURANCA =
            2200;

    @Override
    protected void setup() {

        System.out.println(
                "Agente Seguranca iniciado: "
                + getLocalName()
        );

        addBehaviour(
                new CyclicBehaviour() {

                    @Override
                    public void action() {

                        ACLMessage mensagem =
                                receive();

                        if (mensagem == null) {
                            block();
                            return;
                        }

                        EntradaSeguranca entrada =
                                interpretarEntrada(
                                        mensagem.getContent()
                                );

                        ResultadoAnalise resultado =
                                analisarRisco(
                                        entrada.mensagem,
                                        entrada.contexto
                                );

                        System.out.println(
                                "\n[SEGURANCA] Texto analisado:"
                        );

                        System.out.println(
                                normalizar(
                                        entrada.mensagem
                                )
                        );

                        System.out.println(
                                "[SEGURANCA] Contexto recente recebido: "
                                + (
                                        entrada.contexto
                                                .trim()
                                                .isEmpty()
                                                ? "NAO"
                                                : "SIM"
                                )
                        );

                        System.out.println(
                                "[SEGURANCA] Nivel de risco: "
                                + resultado.nivelRisco
                        );

                        System.out.println(
                                "[SEGURANCA] Confianca: "
                                + resultado.confianca
                        );

                        System.out.println(
                                "[SEGURANCA] Metodo: "
                                + resultado.metodo
                        );

                        System.out.println(
                                "[SEGURANCA] Categoria: "
                                + resultado.categoria
                        );

                        System.out.println(
                                "[SEGURANCA] Justificativa: "
                                + resultado.justificativa
                        );

                        ACLMessage resposta =
                                mensagem.createReply();

                        resposta.setPerformative(
                                ACLMessage.INFORM
                        );

                        resposta.setContent(
                                resultado.nivelRisco
                        );

                        send(resposta);
                    }
                }
        );
    }

    private EntradaSeguranca interpretarEntrada(
            String conteudo) {

        if (conteudo == null) {

            return new EntradaSeguranca(
                    "",
                    ""
            );
        }

        String formato =
                extrairValor(
                        conteudo,
                        "formato"
                );

        if (!"CONTEXTO_V1".equalsIgnoreCase(
                formato)) {

            /*
             * Compatibilidade com chamadas antigas:
             * o conteúdo inteiro continua sendo tratado
             * como a mensagem do usuário.
             */
            return new EntradaSeguranca(
                    conteudo,
                    ""
            );
        }

        String mensagemBase64 =
                extrairValor(
                        conteudo,
                        "mensagemBase64"
                );

        String contextoBase64 =
                extrairValor(
                        conteudo,
                        "contextoBase64"
                );

        return new EntradaSeguranca(
                decodificarBase64(
                        mensagemBase64
                ),
                recortarFinal(
                        decodificarBase64(
                                contextoBase64
                        ),
                        LIMITE_CONTEXTO_SEGURANCA
                )
        );
    }

    private ResultadoAnalise analisarRisco(
            String textoOriginal,
            String contextoOriginal) {

        String textoNormalizado =
                normalizar(
                        textoOriginal
                );

        String contextoNormalizado =
                normalizar(
                        contextoOriginal
                );

        if (textoNormalizado.isEmpty()) {

            return new ResultadoAnalise(
                    BAIXO_RISCO,
                    1.0,
                    "entrada_vazia",
                    "sem_conteudo",
                    "Mensagem vazia ou sem conteudo suficiente para analise."
            );
        }

        /*
         * Camada 1 - sinais críticos explícitos na mensagem atual.
         */
        ResultadoAnalise critico =
                analisarRegrasCriticas(
                        textoNormalizado
                );

        if (critico != null) {
            return critico;
        }

        /*
         * Camada 2 - confirmação curta contextual.
         *
         * "sim" sozinho NÃO deve ser interpretado como risco.
         * Só vira RISCO quando o contexto recente contém uma
         * pergunta inequívoca sobre perigo ou autoagressão.
         */
        ResultadoAnalise confirmacao =
                analisarConfirmacaoContextual(
                        textoNormalizado,
                        contextoNormalizado
                );

        if (confirmacao != null) {
            return confirmacao;
        }

        /*
         * Camada 3 - IA recebe mensagem + contexto recente.
         */
        ResultadoAnalise resultadoIA =
                analisarComIA(
                        textoOriginal,
                        contextoOriginal
                );

        if (resultadoIA != null
                && nivelValido(
                        resultadoIA.nivelRisco
                )) {

            return resultadoIA;
        }

        /*
         * Camada 4 - fallback determinístico.
         */
        return analisarPorPontuacao(
                textoNormalizado
        );
    }

    private ResultadoAnalise analisarConfirmacaoContextual(
            String texto,
            String contexto) {

        if (!ehRespostaCurtaAfirmativa(
                texto)) {

            return null;
        }

        if (contextoIndicaPerguntaDeRisco(
                contexto)) {

            return new ResultadoAnalise(
                    RISCO,
                    0.98,
                    "regra_contextual",
                    "confirmacao_de_risco_imediato",
                    "A resposta curta confirma uma pergunta recente e explicita sobre perigo imediato ou possibilidade de se machucar."
            );
        }

        if (contextoIndicaAssuntoNaoCritico(
                contexto)) {

            return new ResultadoAnalise(
                    BAIXO_RISCO,
                    0.98,
                    "regra_contextual",
                    "confirmacao_de_assunto_nao_critico",
                    "A resposta curta confirma um assunto recente sem indicacao de perigo imediato."
            );
        }

        /*
         * Sem contexto suficiente, não inventamos uma pergunta
         * de risco que não foi fornecida.
         */
        return new ResultadoAnalise(
                BAIXO_RISCO,
                0.90,
                "regra_contextual",
                "confirmacao_sem_contexto_de_risco",
                "Resposta curta afirmativa sem contexto recente que indique perigo imediato."
        );
    }

    private boolean contextoIndicaPerguntaDeRisco(
            String contexto) {

        if (contexto == null
                || contexto.isEmpty()) {

            return false;
        }

        String recente =
                recortarFinal(
                        contexto,
                        1200
                );

        return recente.contains(
                "esta em perigo agora")
                || recente.contains(
                "voce esta em perigo")
                || recente.contains(
                "esta seguro agora")
                || recente.contains(
                "voce esta seguro")
                || recente.contains(
                "pensa em se machucar")
                || recente.contains(
                "pensando em se machucar")
                || recente.contains(
                "pretende se machucar")
                || recente.contains(
                "pode se machucar")
                || recente.contains(
                "risco de se machucar")
                || recente.contains(
                "intencao de se machucar")
                || recente.contains(
                "fazer algo contra voce")
                || recente.contains(
                "medo de fazer algo consigo")
                || recente.contains(
                "precisa de ajuda agora")
                || recente.contains(
                "risco imediato");
    }

    private boolean contextoIndicaAssuntoNaoCritico(
            String contexto) {

        if (contexto == null
                || contexto.isEmpty()) {

            return false;
        }

        String recente =
                recortarFinal(
                        contexto,
                        1200
                );

        return recente.contains(
                "locais proximos")
                || recente.contains(
                "locais de atendimento")
                || recente.contains(
                "samambaia")
                || recente.contains(
                "caps")
                || recente.contains(
                "ubs")
                || recente.contains(
                "consultar")
                || recente.contains(
                "buscar")
                || recente.contains(
                "procure")
                || recente.contains(
                "dass")
                || recente.contains(
                "triagem")
                || recente.contains(
                "monitoramento")
                || recente.contains(
                "conteudo")
                || recente.contains(
                "psicologo");
    }

    private ResultadoAnalise analisarRegrasCriticas(
            String texto) {

        if (contem(texto, "risco imediato")
                || contem(texto, "emergencia")
                || contem(texto, "urgente")
                || contem(texto, "nao estou seguro")
                || contem(texto, "nao me sinto seguro")
                || contem(texto, "preciso de ajuda agora")
                || contem(texto, "preciso de ajuda imediata")
                || contem(texto, "alguem precisa me ajudar agora")
                || contem(texto, "posso me machucar")
                || contem(texto, "vou me machucar")
                || contem(texto, "quero me machucar")
                || contem(texto, "medo de fazer algo comigo")) {

            return new ResultadoAnalise(
                    RISCO,
                    1.0,
                    "regras_criticas",
                    "possivel_risco_imediato",
                    "Foram identificados sinais explicitos de urgencia ou necessidade de apoio imediato."
            );
        }

        return null;
    }

    private ResultadoAnalise analisarComIA(
            String textoOriginal,
            String contextoOriginal) {

        String prompt =
                montarPromptClassificacao(
                        textoOriginal,
                        contextoOriginal
                );

        try {

            System.out.println(
                    "[SEGURANCA] Acionando classificador por IA: "
                    + ClienteLLM.obterProvedor()
            );

            String respostaIA =
                    ClienteLLM.gerarResposta(
                            prompt
                    );

            if (respostaIA == null
                    || respostaIA.trim().isEmpty()
                    || respostaIA.contains("Erro")) {

                System.out.println(
                        "[SEGURANCA] IA indisponivel ou retornou erro. Usando fallback por regras."
                );

                return null;
            }

            System.out.println(
                    "[SEGURANCA] Resposta bruta da IA:"
            );

            System.out.println(
                    respostaIA
            );

            ResultadoAnalise resultado =
                    interpretarRespostaIA(
                            respostaIA
                    );

            if (!nivelValido(
                    resultado.nivelRisco)) {

                System.out.println(
                        "[SEGURANCA] Nivel retornado pela IA invalido. Usando fallback por regras."
                );

                return null;
            }

            resultado.metodo =
                    "ia_llm_contextual";

            return resultado;

        } catch (Exception e) {

            System.out.println(
                    "[SEGURANCA] Falha ao acionar IA: "
                    + e.getMessage()
            );

            return null;
        }
    }

    private String montarPromptClassificacao(
            String textoOriginal,
            String contextoOriginal) {

        String contexto =
                recortarFinal(
                        contextoOriginal,
                        LIMITE_CONTEXTO_SEGURANCA
                );

        return "Voce e um classificador de seguranca para um prototipo academico de apoio emocional.\\n"
                + "Classifique a MENSAGEM ATUAL considerando apenas o CONTEXTO RECENTE fornecido.\\n\\n"

                + "BAIXO_RISCO: saudacao, conversa comum, pedido de informacao, busca de locais, preocupacao, estresse, ansiedade ou tristeza sem sinal de urgencia imediata.\\n"
                + "ATENCAO: sofrimento emocional intenso, desesperanca, isolamento ou dificuldade importante de lidar com o momento, sem indicacao clara de perigo imediato.\\n"
                + "RISCO: perigo imediato, inseguranca atual ou indicacao clara de possibilidade de dano a si ou a outra pessoa.\\n\\n"

                + "Regras obrigatorias:\\n"
                + "- Nao invente contexto ausente.\\n"
                + "- A palavra 'sim' isolada nao significa risco por si so.\\n"
                + "- Se 'sim' responder a uma pergunta sobre buscar local, CAPS, psicologo ou outro recurso, classifique BAIXO_RISCO.\\n"
                + "- Se 'sim' responder claramente a uma pergunta recente sobre perigo imediato ou possibilidade de se machucar, classifique RISCO.\\n"
                + "- Pedido por CAPS, UBS, psicologo, endereco ou locais de atendimento nao e sinal de risco por si so.\\n"
                + "- Nao responda como terapeuta e nao faca diagnostico.\\n"
                + "- Responda somente no formato chave=valor separado por ponto e virgula.\\n\\n"

                + "CONTEXTO RECENTE:\\n"
                + (
                        contexto == null
                                || contexto.trim().isEmpty()
                                ? "(sem contexto recente)"
                                : contexto
                )
                + "\\n\\n"

                + "MENSAGEM ATUAL:\\n"
                + textoOriginal
                + "\\n\\n"

                + "Formato obrigatorio:\\n"
                + "nivelRisco=BAIXO_RISCO|ATENCAO|RISCO;confianca=0.00;categoria=texto_curto;justificativa=texto_curto";
    }

    private ResultadoAnalise interpretarRespostaIA(
            String respostaIA) {

        String nivelRisco =
                extrairValor(
                        respostaIA,
                        "nivelRisco"
                );

        String confiancaTexto =
                extrairValor(
                        respostaIA,
                        "confianca"
                );

        String categoria =
                extrairValor(
                        respostaIA,
                        "categoria"
                );

        String justificativa =
                extrairValor(
                        respostaIA,
                        "justificativa"
                );

        double confianca =
                converterConfianca(
                        confiancaTexto
                );

        if (nivelRisco == null
                || nivelRisco.trim().isEmpty()) {

            nivelRisco =
                    BAIXO_RISCO;
        }

        nivelRisco =
                nivelRisco.trim()
                        .toUpperCase();

        if (categoria == null
                || categoria.trim().isEmpty()) {

            categoria =
                    "nao_informada";
        }

        if (justificativa == null
                || justificativa.trim().isEmpty()) {

            justificativa =
                    "Classificacao gerada por IA sem justificativa detalhada.";
        }

        return new ResultadoAnalise(
                nivelRisco,
                confianca,
                "ia_llm_contextual",
                categoria,
                justificativa
        );
    }

    private ResultadoAnalise analisarPorPontuacao(
            String texto) {

        int pontosAtencao =
                0;

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

    private boolean ehRespostaCurtaAfirmativa(
            String texto) {

        return texto.equals("sim")
                || texto.equals("s")
                || texto.equals("claro")
                || texto.equals("pode")
                || texto.equals("pode sim")
                || texto.equals("ok")
                || texto.equals("pode ser")
                || texto.equals("isso")
                || texto.equals("isso mesmo");
    }

    private boolean nivelValido(
            String nivel) {

        if (nivel == null) {
            return false;
        }

        String valor =
                nivel.trim()
                        .toUpperCase();

        return valor.equals(
                BAIXO_RISCO)
                || valor.equals(
                ATENCAO)
                || valor.equals(
                RISCO);
    }

    private double converterConfianca(
            String texto) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return 0.0;
        }

        try {

            String valor =
                    texto.trim()
                            .replace(
                                    ",",
                                    "."
                            );

            double numero =
                    Double.parseDouble(
                            valor
                    );

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

    private String extrairValor(
            String texto,
            String chave) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return "";
        }

        String[] partes =
                texto.split(";");

        for (String parte : partes) {

            String[] kv =
                    parte.split(
                            "=",
                            2
                    );

            if (kv.length == 2
                    && kv[0].trim()
                    .equalsIgnoreCase(
                            chave
                    )) {

                return kv[1].trim();
            }
        }

        return "";
    }

    private String decodificarBase64(
            String valor) {

        if (valor == null
                || valor.trim().isEmpty()) {

            return "";
        }

        try {

            byte[] bytes =
                    Base64.getDecoder()
                            .decode(
                                    valor.trim()
                            );

            return new String(
                    bytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            return "";
        }
    }

    private String recortarFinal(
            String texto,
            int limite) {

        if (texto == null) {
            return "";
        }

        String valor =
                texto.trim();

        if (limite <= 0
                || valor.length() <= limite) {

            return valor;
        }

        return valor.substring(
                valor.length() - limite
        );
    }

    private boolean contem(
            String texto,
            String termo) {

        return texto.contains(
                normalizar(
                        termo
                )
        );
    }

    private String normalizar(
            String texto) {

        if (texto == null) {
            return "";
        }

        String normalizado =
                Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                );

        normalizado =
                normalizado.replaceAll(
                        "[\\p{InCombiningDiacriticalMarks}]",
                        ""
                );

        normalizado =
                normalizado.toLowerCase()
                        .replace("\r", " ")
                        .replace("\n", " ")
                        .trim();

        while (normalizado.contains(
                "  ")) {

            normalizado =
                    normalizado.replace(
                            "  ",
                            " "
                    );
        }

        return normalizado;
    }

    private static class EntradaSeguranca {

        final String mensagem;
        final String contexto;

        EntradaSeguranca(
                String mensagem,
                String contexto) {

            this.mensagem =
                    mensagem == null
                            ? ""
                            : mensagem;

            this.contexto =
                    contexto == null
                            ? ""
                            : contexto;
        }
    }

    private static class ResultadoAnalise {

        String nivelRisco;
        double confianca;
        String metodo;
        String categoria;
        String justificativa;

        ResultadoAnalise(
                String nivelRisco,
                double confianca,
                String metodo,
                String categoria,
                String justificativa) {

            this.nivelRisco =
                    nivelRisco;

            this.confianca =
                    confianca;

            this.metodo =
                    metodo;

            this.categoria =
                    categoria;

            this.justificativa =
                    justificativa;
        }
    }
}
