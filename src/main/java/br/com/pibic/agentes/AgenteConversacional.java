package br.com.pibic.agentes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import br.com.pibic.api.AcoesDisponiveis;
import br.com.pibic.api.ChatResponse;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgenteConversacional extends Agent {

    private final Map<String, Boolean> contextoLocalPendente =
            new ConcurrentHashMap<String, Boolean>();

    private static final long TIMEOUT_SEGURANCA_MS = 40000L;
    private static final long TIMEOUT_INTERVENCAO_MS = 10000L;
    private static final long TIMEOUT_MEMORIA_MS = 10000L;
    private static final long TIMEOUT_CONTEUDO_MS = 15000L;
    private static final long TIMEOUT_PSICOLOGO_MS = 15000L;
    private static final long TIMEOUT_LOCAL_MS = 65000L;


    @Override
    protected void setup() {
        System.out.println("Agente Conversacional iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    String origem = extrairValor(conteudo, "origem");
                    boolean requisicaoApi = "API".equalsIgnoreCase(origem);
                    String usuarioId = extrairValor(conteudo, "usuarioId");

                    String tipo = extrairValor(conteudo, "tipo");

                    if (tipo.equalsIgnoreCase("relatorio_simulado")) {
                        tratarRelatorioSimulado(conteudo);
                        return;
                    }

                    String respostaMonitoramento = extrairValor(conteudo, "respostaMonitoramento");
                    String respostaConteudo = extrairValor(conteudo, "respostaConteudo");
                    String respostaPsicologo = extrairValor(conteudo, "respostaPsicologo");
                    String respostaLocalAtendimento = extrairValor(conteudo, "respostaLocalAtendimento");

                    if (!respostaMonitoramento.isEmpty()) {
                        tratarRespostaMonitoramento(mensagem, conteudo, respostaMonitoramento);
                        return;
                    }

                    if (!respostaConteudo.isEmpty()) {
                        tratarRespostaConteudo(mensagem, conteudo, respostaConteudo);
                        return;
                    }

                    if (!respostaPsicologo.isEmpty()) {
                        tratarRespostaPsicologo(mensagem, conteudo, respostaPsicologo);
                        return;
                    }

                    if (!respostaLocalAtendimento.isEmpty()) {
                        tratarRespostaLocalAtendimento(mensagem, conteudo, respostaLocalAtendimento);
                        return;
                    }

                    String perfil = extrairValor(conteudo, "perfil");
                    String mensagemUsuario = extrairValor(conteudo, "mensagem");

                    if (requisicaoApi) {
                        String mensagemBase64 = extrairValor(
                                conteudo,
                                "mensagemBase64"
                        );

                        if (mensagemBase64 != null
                                && !mensagemBase64.trim().isEmpty()) {
                            mensagemUsuario = decodificarBase64(mensagemBase64);
                        }
                    }

                    System.out.println("\n[CONVERSACIONAL] Conteudo recebido:");
                    System.out.println(conteudo);
                    System.out.println("[CONVERSACIONAL] Perfil recebido: " + perfil);
                    System.out.println("[CONVERSACIONAL] Mensagem do usuario: " + mensagemUsuario);

                    salvarNaMemoria(usuarioId, "perfil", perfil);

                    /*
                     * Recuperamos o contexto ANTES de registrar a mensagem atual.
                     * Assim o AgenteSeguranca recebe o histórico imediatamente
                     * anterior e consegue interpretar respostas curtas como
                     * "sim" sem confundir o assunto da conversa.
                     */
                    String memoriaContexto =
                            consultarMemoria(
                                    usuarioId
                            );

                    salvarNaMemoria(
                            usuarioId,
                            "mensagem",
                            mensagemUsuario
                    );

                    String risco =
                            consultarSeguranca(
                                    mensagemUsuario,
                                    memoriaContexto
                            );

                    salvarNaMemoria(usuarioId, "risco", risco);

                    System.out.println("[CONVERSACIONAL] Nivel de risco: " + risco);

                    String decisaoIntervencao = consultarIntervencao(usuarioId, risco, perfil, mensagemUsuario);
                    String protocolo = extrairValor(decisaoIntervencao, "protocolo");
                    String mensagemIntervencao = extrairValor(decisaoIntervencao, "mensagem");

                    boolean permitirIA = extrairBoolean(decisaoIntervencao, "permitirIA", true);
                    boolean permitirConteudo = extrairBoolean(decisaoIntervencao, "permitirConteudo", true);
                    boolean permitirPsicologo = extrairBoolean(decisaoIntervencao, "permitirPsicologo", true);
                    boolean permitirLocalAtendimento = extrairBoolean(decisaoIntervencao, "permitirLocalAtendimento", true);
                    boolean permitirMonitoramento = extrairBoolean(decisaoIntervencao, "permitirMonitoramento", true);
                    boolean exibirBotaoCVV = extrairBoolean(decisaoIntervencao, "exibirBotaoCVV", false);
                    String telefoneCVV = extrairValor(decisaoIntervencao, "telefoneCVV");

                    salvarNaMemoria(usuarioId, "protocolo_intervencao", protocolo);

                    System.out.println("[CONVERSACIONAL] Protocolo de intervencao: " + protocolo);

                    String resposta;

                    boolean saudacaoGenerica =
                            ehSaudacaoGenerica(
                                    mensagemUsuario
                            );

                    boolean encerramentoNeutro =
                            ehAgradecimentoOuEncerramento(
                                    mensagemUsuario
                            );

                    if (saudacaoGenerica
                            || encerramentoNeutro) {
                        /*
                         * Saudacoes e encerramentos curtos iniciam um
                         * turno neutro. Nao reutilizamos uma intencao antiga
                         * de busca de locais apenas porque ela existe no historico.
                         */
                        limparContextoLocalPendente(
                                usuarioId
                        );
                    }

                    if (encerramentoNeutro) {

                        System.out.println(
                                "[CONVERSACIONAL] Encerramento neutro detectado. "
                                + "Historico de locais nao sera retomado."
                        );
                    }

                    boolean acaoLocalAtendimento =
                            false;

                    if (!permitirIA || risco.equals("RISCO")) {

                        acaoLocalAtendimento =
                                permitirLocalAtendimento
                                && deveOferecerLocalAtendimento(
                                        usuarioId,
                                        mensagemUsuario,
                                        memoriaContexto
                                );

                        resposta = mensagemIntervencao;

                        if (resposta == null || resposta.trim().isEmpty()) {
                            resposta = respostaSeguranca();
                        }

                    } else {

                        acaoLocalAtendimento =
                                permitirLocalAtendimento
                                && deveOferecerLocalAtendimento(
                                        usuarioId,
                                        mensagemUsuario,
                                        memoriaContexto
                                );

                        System.out.println(
                                "[CONVERSACIONAL] Acao localAtendimento: "
                                + acaoLocalAtendimento
                        );

                        /*
                         * Informacoes factuais sobre estabelecimentos de saude
                         * NAO sao geradas pelo LLM.
                         *
                         * Quando a conversa indica que o usuario quer um local,
                         * o agente apenas orienta o uso da acao estruturada.
                         * Os nomes, enderecos, telefones, distancias e fontes
                         * sao obtidos exclusivamente pelo AgenteLocalAtendimento.
                         */
                        if (acaoLocalAtendimento) {

                            resposta =
                                    gerarRespostaBuscaLocal(
                                            mensagemUsuario
                                    );

                        } else if (saudacaoGenerica) {

                            resposta =
                                    gerarRespostaSaudacao();

                        } else if (encerramentoNeutro) {

                            resposta =
                                    gerarRespostaEncerramento();

                        } else {

                            String prompt =
                                    montarPrompt(
                                            perfil,
                                            mensagemUsuario,
                                            risco,
                                            memoriaContexto,
                                            protocolo
                                    );

                            System.out.println(
                                    "[CONVERSACIONAL] Provedor de IA configurado: "
                                    + ClienteLLM.obterProvedor()
                            );

                            resposta =
                                    ClienteLLM.gerarResposta(
                                            prompt
                                    );

                            resposta =
                                    limparRespostaClinica(
                                            resposta
                                    );

                            if (resposta == null
                                    || resposta.trim().isEmpty()
                                    || resposta.contains("Erro")) {

                                resposta =
                                        gerarRespostaFallback(
                                                perfil,
                                                mensagemUsuario,
                                                risco
                                        );
                            }

                            /*
                             * Se o próprio LLM ofereceu a busca de locais,
                             * guardamos essa intenção para interpretar uma
                             * resposta curta como "sim" no próximo turno.
                             *
                             * Isso NÃO libera dados factuais pelo LLM.
                             * Apenas preserva o estado conversacional.
                             */
                            if (!encerramentoNeutro
                                    && permitirLocalAtendimento
                                    && respostaOfereceBuscaLocal(
                                            resposta
                                    )) {

                                marcarContextoLocalPendente(
                                        usuarioId
                                );

                                System.out.println(
                                        "[CONVERSACIONAL] Oferta de busca de local detectada na resposta. "
                                        + "Contexto pendente marcado para usuarioId="
                                        + usuarioId
                                );
                            }
                        }

                        if (mensagemIntervencao != null
                                && !mensagemIntervencao.trim().isEmpty()
                                && risco.equals("ATENCAO")) {

                            resposta +=
                                    "\n\n"
                                    + mensagemIntervencao;
                        }
                    }

                    if (!saudacaoGenerica
                            && !encerramentoNeutro) {

                        resposta =
                                garantirAvisoPlataforma(
                                        resposta
                                );
                    }

                    resposta += montarBlocoApoioCVV(exibirBotaoCVV, telefoneCVV, risco, protocolo);

                    boolean acaoPsicologo =
                            permitirPsicologo
                            && deveSugerirPsicologo(perfil, risco);

                    /*
                     * No modo legado/simulacao, mantemos os convites em texto
                     * porque o AgentePaciente responde a essas perguntas.
                     *
                     * No modo API, os convites nao sao concatenados ao texto.
                     * A interface recebe as acoes como campos booleanos no JSON.
                     */
                    if (!requisicaoApi) {
                        if (permitirConteudo) {
                            resposta += "\n\nPosso sugerir conteudos educativos selecionados de uma base curada, com fontes institucionais e referencias documentadas, de acordo com o que voce relatou. Deseja receber sugestoes de conteudo? (sim/nao)";
                        }

                        if (acaoPsicologo) {
                            resposta += "\n\nTambem posso selecionar alguns psicologos cadastrados que tenham relacao com o perfil identificado na triagem. Deseja ver essas indicacoes? (sim/nao)";
                        }

                        if (permitirLocalAtendimento) {
                            resposta += "\n\nTambem posso mostrar locais demonstrativos de atendimento, como UBS, CAPS ou servicos publicos de saude mental. Deseja visualizar locais de atendimento proximos? (sim/nao)";
                        }

                        if (permitirMonitoramento) {
                            resposta += "\n\nPara fins academicos, tambem posso mostrar uma simulacao de acompanhamento semanal, demonstrando como um profissional poderia acompanhar indicadores emocionais com apoio do sistema. Deseja visualizar essa simulacao? (sim/nao)";
                        }
                    }

                    salvarNaMemoria(usuarioId, "resposta", resposta);

                    System.out.println("[CONVERSACIONAL] Resposta gerada:");
                    System.out.println(resposta);

                    ACLMessage reply = mensagem.createReply();
                    reply.setPerformative(ACLMessage.INFORM);

                    if (requisicaoApi) {
                        String telefoneCvvResposta = telefoneCVV;

                        if (telefoneCvvResposta == null
                                || telefoneCvvResposta.trim().isEmpty()) {
                            telefoneCvvResposta = "188";
                        }

                        AcoesDisponiveis acoes = new AcoesDisponiveis(
                                permitirConteudo,
                                acaoPsicologo,
                                acaoLocalAtendimento,
                                permitirMonitoramento,
                                exibirBotaoCVV,
                                exibirBotaoCVV
                                        ? telefoneCvvResposta
                                        : null
                        );

                        ChatResponse respostaApi = ChatResponse.sucesso(
                                usuarioId,
                                perfil,
                                risco,
                                protocolo,
                                resposta,
                                acoes
                        );

                        reply.setContent(
                                new Gson().toJson(respostaApi)
                        );

                    } else {
                        reply.setContent(resposta);
                    }

                    send(reply);
                } else {
                    block();
                }
            }
        });
    }

    private void tratarRelatorioSimulado(String conteudo) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String perfil = extrairValor(conteudo, "perfil");
        String relatorio = extrairValor(conteudo, "relatorio");

        System.out.println("\n[CONVERSACIONAL] Relatorio simulado recebido do AgenteRelatorio:");
        System.out.println(relatorio);

        salvarNaMemoria(usuarioId, "relatorio_simulado", "gerado");

        ACLMessage msgPaciente = new ACLMessage(ACLMessage.INFORM);
        msgPaciente.addReceiver(new AID("agentePaciente", AID.ISLOCALNAME));
        msgPaciente.setContent(
                "Relatorio simulado gerado com sucesso.\n\n"
                + relatorio
        );

        send(msgPaciente);

        System.out.println("[CONVERSACIONAL] Relatorio simulado enviado ao AgentePaciente");
    }

    private void tratarRespostaConteudo(ACLMessage mensagemOriginal, String conteudo, String respostaConteudo) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String perfil = extrairValor(conteudo, "perfil");
        String risco = extrairValor(conteudo, "risco");
        String mensagemUsuario = extrairValor(conteudo, "mensagem");

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaConteudo.equalsIgnoreCase("sim")) {

            /*
             * Falha segura: o AgenteConversacional nao deve assumir BAIXO_RISCO
             * quando a informacao de risco nao estiver presente.
             *
             * O valor abaixo nao corresponde a nenhum risco permitido na base
             * curada, portanto o AgenteConteudo nao selecionara material.
             */
            if (risco == null || risco.trim().isEmpty()) {
                risco = "RISCO_NAO_INFORMADO";
                System.out.println("[CONVERSACIONAL] Risco ausente na solicitacao de conteudo. Recomendacao sera bloqueada por seguranca.");
            }

            String sugestao = consultarConteudo(usuarioId, perfil, risco, mensagemUsuario);
            salvarNaMemoria(usuarioId, "conteudo", "aceito");
            reply.setContent(sugestao);
        } else {
            salvarNaMemoria(usuarioId, "conteudo", "recusado");
            reply.setContent("Tudo bem. Os conteudos educativos podem ser consultados em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaPsicologo(ACLMessage mensagemOriginal, String conteudo, String respostaPsicologo) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String perfil = extrairValor(conteudo, "perfil");
        String risco = extrairValor(conteudo, "risco");

        if (risco == null || risco.isEmpty()) {
            risco = "RISCO_NAO_INFORMADO";
        }

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaPsicologo.equalsIgnoreCase("sim")) {
            String psicologos = consultarPsicologo(usuarioId, perfil, risco);
            salvarNaMemoria(usuarioId, "psicologo", "aceito");
            reply.setContent(psicologos);
        } else {
            salvarNaMemoria(usuarioId, "psicologo", "recusado");
            reply.setContent("Tudo bem. As indicacoes de profissionais podem ser consultadas em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaLocalAtendimento(ACLMessage mensagemOriginal, String conteudo, String respostaLocalAtendimento) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String cidade = extrairValor(conteudo, "cidade");
        String uf = extrairValor(conteudo, "uf");

        if (cidade == null || cidade.isEmpty()) {
            cidade = "Brasilia";
        }

        if (uf == null || uf.isEmpty()) {
            uf = "DF";
        }

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaLocalAtendimento.equalsIgnoreCase("sim")) {
            String locais = consultarLocalAtendimento(usuarioId, cidade, uf);
            salvarNaMemoria(usuarioId, "local_atendimento", "aceito");
            reply.setContent(locais);
        } else {
            salvarNaMemoria(usuarioId, "local_atendimento", "recusado");
            reply.setContent("Tudo bem. A busca por locais de atendimento pode ser feita em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaMonitoramento(ACLMessage mensagemOriginal, String conteudo, String respostaMonitoramento) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String perfil = extrairValor(conteudo, "perfil");

        System.out.println("\n[CONVERSACIONAL] Resposta sobre simulacao de monitoramento recebida: " + respostaMonitoramento);

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaMonitoramento.equalsIgnoreCase("sim")) {
            ACLMessage msgMonitoramento = new ACLMessage(ACLMessage.INFORM);
            msgMonitoramento.addReceiver(new AID("agenteMonitoramento", AID.ISLOCALNAME));
            msgMonitoramento.setContent(
                    "iniciar=true;"
                    + "usuarioId=" + usuarioId + ";"
                    + "perfil=" + perfil + ";"
                    + "modo=simulado"
            );
            send(msgMonitoramento);

            salvarNaMemoria(usuarioId, "monitoramento_simulado", "aceito");

            reply.setContent("Perfeito. Vou iniciar uma simulacao academica de acompanhamento semanal. Esses dados sao demonstrativos e nao representam avaliacao clinica.");
        } else {
            salvarNaMemoria(usuarioId, "monitoramento_simulado", "recusado");

            reply.setContent("Tudo bem. A simulacao de acompanhamento pode ser visualizada em outro momento.");
        }

        send(reply);
    }

    private boolean deveSugerirPsicologo(String perfil, String risco) {
        if (risco.equals("ATENCAO") || risco.equals("RISCO")) {
            return true;
        }

        return perfil.equals("ANSIEDADE")
                || perfil.equals("DEPRESSAO")
                || perfil.equals("MISTO");
    }

    private String montarBlocoApoioCVV(boolean exibirBotaoCVV, String telefoneCVV, String risco, String protocolo) {
        if (!exibirBotaoCVV) {
            return "";
        }

        if (telefoneCVV == null || telefoneCVV.trim().isEmpty()) {
            telefoneCVV = "188";
        }

        if (risco.equals("RISCO") || protocolo.equals("PROTOCOLO_PRESERVACAO_DA_VIDA")) {
            return "\n\n[ACAO_RECOMENDADA] Ligar para o CVV pelo telefone " + telefoneCVV + "."
                    + "\n[ACAO_RECOMENDADA] Procurar um servico de emergencia ou alguem de confianca que possa estar com voce agora.";
        }

        if (risco.equals("ATENCAO") || protocolo.equals("PROTOCOLO_ENCAMINHAMENTO_SEGURO")) {
            return "\n\n[RECURSO_DE_APOIO] O CVV pelo telefone " + telefoneCVV + " pode ser usado como canal de escuta e apoio emocional."
                    + "\n[RECURSO_DE_APOIO] Conversar com alguem de confianca ou buscar um profissional de saude mental tambem pode ajudar neste momento.";
        }

        return "";
    }

    private String montarPrompt(String perfil, String mensagemUsuario, String risco, String memoria, String protocolo) {
        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        if (protocolo == null || protocolo.isEmpty()) {
            protocolo = "PROTOCOLO_APOIO_INICIAL";
        }

        return "Voce faz parte de um prototipo academico de triagem emocional inicial.\n"
                + "A plataforma nao realiza diagnostico, nao indica tratamento e nao substitui acompanhamento psicologico profissional.\n"
                + "Ela apenas organiza informacoes iniciais, acolhe a mensagem do usuario e incentiva a busca por profissionais qualificados quando fizer sentido.\n\n"
                + "Informacoes do usuario:\n"
                + memoria + "\n\n"
                + "Perfil identificado pela triagem academica: " + perfil + "\n"
                + "Nivel de risco identificado pelo agente de seguranca: " + risco + "\n"
                + "Protocolo definido pelo agente de intervencao: " + protocolo + "\n"
                + "Mensagem atual do usuario: " + mensagemUsuario + "\n\n"
                + "Regras obrigatorias:\n"
                + "- Use o nome do usuario se estiver disponivel.\n"
                + "- Seja acolhedor, simples, breve e humano.\n"
                + "- Responda prioritariamente a mensagem atual. Use o historico apenas para entender continuacoes.\n"
                + "- Se a mensagem atual for apenas uma saudacao como oi, ola ou bom dia, trate como um novo turno neutro e nao retome por conta propria pedidos antigos sobre locais, CAPS ou outros assuntos.\n"
                + "- Nao afirme diagnosticos.\n"
                + "- Nao diga que o usuario possui transtorno, doenca ou condicao clinica.\n"
                + "- Nao diga que esta tratando o usuario.\n"
                + "- Nao prometa melhora.\n"
                + "- Nao prometa acompanhamento clinico.\n"
                + "- Evite termos clinicos fortes como diagnostico, tratamento, transtorno, doenca ou sintomas.\n"
                + "- Prefira termos como sinais, momento de estresse, sobrecarga emocional, preocupacao, desconforto e apoio inicial.\n"
                + "- Reforce que a plataforma nao substitui acompanhamento psicologico profissional.\n"
                + "- Quando fizer sentido, incentive a busca por apoio profissional.\n"
                + "- NUNCA invente nomes de CAPS, UBS, clinicas, hospitais, profissionais ou outros estabelecimentos.\n"
                + "- NUNCA invente ou forneca por memoria do modelo endereco, telefone, horario, distancia ou disponibilidade de um estabelecimento.\n"
                + "- NUNCA afirme que um atendimento e gratuito sem que essa informacao tenha sido obtida por uma fonte estruturada do sistema.\n"
                + "- Se o usuario pedir um local de atendimento, endereco, telefone ou horario, diga que o aplicativo pode consultar locais reais pela funcao de locais proximos.\n"
                + "- Os dados factuais de locais devem vir exclusivamente do AgenteLocalAtendimento, nunca do conhecimento interno do modelo.\n"
                + "- Nao coloque a resposta inteira entre aspas.\n\n"
                + "Responda em portugues do Brasil com no maximo 150 palavras.";
    }

    private boolean deveOferecerLocalAtendimento(
            String usuarioId,
            String mensagemUsuario,
            String memoria) {

        String atual =
                normalizarTextoIntencao(
                        mensagemUsuario
                );

        String contexto =
                normalizarTextoIntencao(
                        recortarContextoRecente(
                                memoria,
                                1800
                        )
                );

        boolean pedidoDireto =
                possuiIntencaoLocal(atual)
                || possuiPedidoBuscaGeografica(atual);

        if (pedidoDireto) {

            /*
             * O pedido atual ja e suficiente para liberar a acao.
             * Nao deixamos a intencao pendente depois disso, para
             * evitar que um "ok" futuro mostre o mesmo card de novo.
             */
            limparContextoLocalPendente(
                    usuarioId
            );

            return true;
        }

        boolean continuacaoCurta =
                atual.equals("sim")
                || atual.equals("pode")
                || atual.equals("pode sim")
                || atual.equals("claro")
                || atual.equals("ok")
                || atual.equals("manda")
                || atual.equals("pode mandar")
                || atual.equals("procure")
                || atual.equals("procurar")
                || atual.equals("pode procurar")
                || atual.equals("busque")
                || atual.equals("buscar")
                || atual.equals("pode buscar")
                || atual.equals("pesquise")
                || atual.equals("pode pesquisar")
                || atual.equals("consulte")
                || atual.equals("pode consultar")
                || atual.equals("de todos")
                || atual.equals("todos")
                || atual.matches(
                        ".*\\b(df|sp|rj|mg|go|ba|to|sc|pr|rs|pe|ce|pa|am|es|mt|ms|ma|pb|rn|al|se|pi|ro|ac|rr|ap)\\b.*"
                );

        boolean contextoPendente =
                possuiContextoLocalPendente(
                        usuarioId
                );

        System.out.println(
                "[CONVERSACIONAL] Contexto local pendente: "
                + contextoPendente
        );

        /*
         * Para respostas curtas, nao basta existir alguma mencao
         * antiga a locais na memoria. A continuacao so vale quando:
         *
         * 1. ha uma oferta de busca pendente marcada pelo agente; ou
         * 2. o trecho recente contem uma oferta explicita do assistente.
         *
         * Isso impede que "ok" continue reabrindo o card indefinidamente.
         */
        boolean contextoOfereceuBusca =
                respostaOfereceBuscaLocal(
                        contexto
                );

        if (continuacaoCurta
                && (contextoPendente
                || contextoOfereceuBusca)) {

            limparContextoLocalPendente(
                    usuarioId
            );

            return true;
        }

        if (!atual.isEmpty()
                && !continuacaoCurta
                && atual.length() > 8) {

            limparContextoLocalPendente(
                    usuarioId
            );
        }

        return false;
    }

    private boolean possuiPedidoBuscaGeografica(
            String texto) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return false;
        }

        String valor =
                texto.trim();

        return valor.contains("verificar em ")
                || valor.contains("verifique em ")
                || valor.contains("verificar locais em ")
                || valor.contains("verificar os locais em ")
                || valor.contains("verifique locais em ")
                || valor.contains("verifique os locais em ")
                || valor.contains("procurar em ")
                || valor.contains("procure em ")
                || valor.contains("procurar locais em ")
                || valor.contains("procurar os locais em ")
                || valor.contains("procure locais em ")
                || valor.contains("procure os locais em ")
                || valor.contains("buscar em ")
                || valor.contains("busque em ")
                || valor.contains("buscar locais em ")
                || valor.contains("buscar os locais em ")
                || valor.contains("busque locais em ")
                || valor.contains("busque os locais em ")
                || valor.contains("pesquisar em ")
                || valor.contains("pesquise em ")
                || valor.contains("consultar em ")
                || valor.contains("consulte em ")
                || valor.contains("consultar locais em ")
                || valor.contains("consultar os locais em ")
                || valor.contains("consulte locais em ")
                || valor.contains("consulte os locais em ")
                || valor.contains("atendimento em ")
                || valor.contains("psicologo em ")
                || valor.contains("psicologa em ")
                || valor.contains("psiquiatra em ")
                || valor.contains("caps em ")
                || valor.contains("ubs em ");
    }

    private void marcarContextoLocalPendente(
            String usuarioId) {

        if (usuarioId == null
                || usuarioId.trim().isEmpty()) {

            return;
        }

        contextoLocalPendente.put(
                usuarioId.trim(),
                Boolean.TRUE
        );
    }

    private boolean possuiContextoLocalPendente(
            String usuarioId) {

        if (usuarioId == null
                || usuarioId.trim().isEmpty()) {

            return false;
        }

        Boolean valor =
                contextoLocalPendente.get(
                        usuarioId.trim()
                );

        return Boolean.TRUE.equals(
                valor
        );
    }

    private void limparContextoLocalPendente(
            String usuarioId) {

        if (usuarioId == null
                || usuarioId.trim().isEmpty()) {

            return;
        }

        contextoLocalPendente.remove(
                usuarioId.trim()
        );
    }

    private boolean possuiIntencaoLocal(
            String texto) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return false;
        }

        String valor =
                texto.trim();

        if (valor.equals("local")
                || valor.equals("loc")) {

            return true;
        }

        /*
         * Detector composicional para frases naturais.
         *
         * Exemplos que agora devem funcionar:
         * - "precisava saber de alguns locais que possuem atendimentos em Ceilandia"
         * - "locais em Ceilandia"
         * - "quero alguns locais para atendimento"
         */
        boolean mencionaLocal =
                valor.matches(
                        ".*\\blocais?\\b.*"
                );

        boolean mencionaAtendimento =
                valor.contains("atendimento")
                || valor.contains("atendimentos")
                || valor.contains("servico")
                || valor.contains("servicos")
                || valor.contains("saude mental")
                || valor.contains("apoio emocional")
                || valor.contains("caps")
                || valor.contains("ubs")
                || valor.contains("psicologo")
                || valor.contains("psicologa")
                || valor.contains("psiquiatra");

        boolean mencionaBusca =
                valor.contains("preciso")
                || valor.contains("precisava")
                || valor.contains("queria")
                || valor.contains("quero")
                || valor.contains("gostaria")
                || valor.contains("saber")
                || valor.contains("conhecer")
                || valor.contains("encontrar")
                || valor.contains("mostrar")
                || valor.contains("mostre")
                || valor.contains("retornar")
                || valor.contains("mande")
                || valor.contains("manda")
                || valor.contains("buscar")
                || valor.contains("busque")
                || valor.contains("procurar")
                || valor.contains("procure")
                || valor.contains("verificar")
                || valor.contains("verifique")
                || valor.contains("consultar")
                || valor.contains("consulte");

        boolean localComRegiao =
                valor.matches(
                        ".*\\blocais?\\s+(?:de\\s+atendimento\\s+)?em\\s+.+"
                );

        if (mencionaLocal
                && (mencionaAtendimento
                || mencionaBusca
                || localComRegiao)) {

            return true;
        }

        return valor.contains("local de atendimento")
                || valor.contains("locais de atendimento")
                || valor.contains("local proximo")
                || valor.contains("locais proximos")
                || valor.contains("mande os locais")
                || valor.contains("manda os locais")
                || valor.contains("me mande os locais")
                || valor.contains("mostrar os locais")
                || valor.contains("mostre os locais")
                || valor.contains("ver os locais")
                || valor.contains("quero os locais")
                || valor.contains("quero ver locais")
                || valor.contains("buscar locais")
                || valor.contains("buscar os locais")
                || valor.contains("busque locais")
                || valor.contains("busque os locais")
                || valor.contains("procurar locais")
                || valor.contains("procurar os locais")
                || valor.contains("procure locais")
                || valor.contains("procure os locais")
                || valor.contains("consultar locais")
                || valor.contains("consultar os locais")
                || valor.contains("consulte locais")
                || valor.contains("consulte os locais")
                || valor.contains("verificar locais")
                || valor.contains("verificar os locais")
                || valor.contains("verifique locais")
                || valor.contains("verifique os locais")
                || valor.contains("perto de mim")
                || valor.contains("onde posso")
                || valor.contains("onde encontro")
                || valor.contains("onde procurar")
                || valor.contains("atendimento gratuito")
                || valor.contains("atendimento de graca")
                || valor.contains("servico gratuito")
                || valor.contains("servico de saude mental")
                || valor.contains("caps")
                || valor.contains("ubs")
                || valor.contains("centro de atencao psicossocial")
                || valor.contains("endereco")
                || valor.contains("telefone")
                || valor.contains("telefones")
                || valor.contains("contato")
                || valor.contains("contatos")
                || valor.contains("horario")
                || valor.contains("psicologo perto")
                || valor.contains("psicologa perto")
                || valor.contains("psiquiatra perto")
                || valor.contains("pode mandar a loc")
                || valor.contains("manda a loc");
    }

    private boolean ehSaudacaoGenerica(
            String mensagemUsuario) {

        String texto =
                normalizarTextoIntencao(
                        mensagemUsuario
                );

        return texto.equals("oi")
                || texto.equals("ola")
                || texto.equals("oie")
                || texto.equals("bom dia")
                || texto.equals("boa tarde")
                || texto.equals("boa noite")
                || texto.equals("e ai")
                || texto.equals("eai");
    }

    private String gerarRespostaSaudacao() {

        return "Ola! Estou aqui para conversar com voce. "
                + "Como voce esta se sentindo hoje?";
    }

    private boolean ehAgradecimentoOuEncerramento(
            String mensagemUsuario) {

        String texto =
                normalizarTextoIntencao(
                        mensagemUsuario
                );

        return texto.equals("obrigado")
                || texto.equals("obrigada")
                || texto.equals("muito obrigado")
                || texto.equals("muito obrigada")
                || texto.equals("valeu")
                || texto.equals("vlw")
                || texto.equals("agradeco")
                || texto.equals("agradecido")
                || texto.equals("agradecida")
                || texto.equals("entendi obrigado")
                || texto.equals("entendi obrigada")
                || texto.equals("beleza obrigado")
                || texto.equals("beleza obrigada")
                || texto.equals("ta bom obrigado")
                || texto.equals("ta bom obrigada");
    }

    private String gerarRespostaEncerramento() {

        return "Por nada! Se quiser continuar a conversa ou precisar de outro apoio, estou por aqui.";
    }


    private String gerarRespostaBuscaLocal(
            String mensagemUsuario) {

        String texto =
                normalizarTextoIntencao(
                        mensagemUsuario
                );

        StringBuilder resposta =
                new StringBuilder();

        boolean pediuContato =
                texto.contains("telefone")
                || texto.contains("telefones")
                || texto.contains("contato")
                || texto.contains("contatos");

        if (pediuContato) {

            resposta.append(
                    "Os contatos disponiveis aparecem junto aos locais consultados. "
            );

            resposta.append(
                    "Toque em \\\"Ver locais proximos\\\" abaixo. "
                    + "Quando a fonte informar um telefone, o aplicativo exibira o numero e a opcao de ligar. "
                    + "Se a fonte nao trouxer esse dado, o sistema nao vai inventar um contato."
            );

        } else {

            resposta.append(
                    "Certo. A busca de locais reais de atendimento esta disponivel abaixo. "
            );

            resposta.append(
                    "Toque em \\\"Ver locais proximos\\\" para consultar as opcoes. "
                    + "Os nomes, enderecos, telefones, distancias e fontes serao obtidos pelas fontes conectadas ao sistema."
            );
        }

        if (texto.contains("gratuito")
                || texto.contains("de graca")) {

            resposta.append(
                    " Quando a forma de acesso ou gratuidade nao estiver confirmada pela fonte, o aplicativo vai orientar que essa informacao seja confirmada diretamente com o servico."
            );
        }

        return resposta.toString();
    }

    private String recortarContextoRecente(
            String contexto,
            int limiteCaracteres) {

        if (contexto == null) {
            return "";
        }

        String valor =
                contexto.trim();

        if (limiteCaracteres <= 0
                || valor.length() <= limiteCaracteres) {

            return valor;
        }

        return valor.substring(
                valor.length() - limiteCaracteres
        );
    }


    private String normalizarTextoIntencao(
            String texto) {

        if (texto == null) {
            return "";
        }

        String valor =
                java.text.Normalizer.normalize(
                        texto,
                        java.text.Normalizer.Form.NFD
                );

        valor =
                valor.replaceAll(
                        "[\\p{InCombiningDiacriticalMarks}]",
                        ""
                );

        valor =
                valor.toLowerCase()
                        .replace("\r", " ")
                        .replace("\n", " ")
                        .trim();

        while (valor.contains("  ")) {
            valor =
                    valor.replace(
                            "  ",
                            " "
                    );
        }

        return valor;
    }


    private boolean respostaOfereceBuscaLocal(
            String resposta) {

        String texto =
                normalizarTextoIntencao(
                        resposta
                );

        if (texto.isEmpty()) {
            return false;
        }

        boolean mencionaRecurso =
                texto.contains("locais proximos")
                || texto.contains("locais de atendimento")
                || texto.contains("consultar locais")
                || texto.contains("buscar locais")
                || texto.contains("funcao de locais proximos")
                || texto.contains("servicos de apoio emocional em")
                || texto.contains("servicos de saude mental em");

        boolean ofereceAcao =
                texto.contains("deseja que eu faca")
                || texto.contains("quer que eu")
                || texto.contains("posso consultar agora")
                || texto.contains("posso buscar agora")
                || texto.contains("posso procurar agora")
                || texto.contains("posso verificar agora");

        return mencionaRecurso
                && ofereceAcao;
    }

    private String garantirAvisoPlataforma(
            String resposta) {

        String aviso =
                "Importante: esta plataforma realiza apenas apoio inicial e triagem emocional. "
                + "Ela nao substitui acompanhamento psicologico profissional.";

        if (resposta == null
                || resposta.trim().isEmpty()) {

            return aviso;
        }

        String normalizado =
                normalizarTextoIntencao(
                        resposta
                );

        boolean jaPossuiAviso =
                normalizado.contains(
                        "esta plataforma realiza apenas apoio inicial e triagem emocional"
                )
                || (
                        normalizado.contains(
                                "nao substitui"
                        )
                        && normalizado.contains(
                                "acompanhamento psicologico"
                        )
                )
                || (
                        normalizado.contains(
                                "apoio inicial"
                        )
                        && normalizado.contains(
                                "nao substitui"
                        )
                );

        if (jaPossuiAviso) {
            return resposta.trim();
        }

        return resposta.trim()
                + "\n\n"
                + aviso;
    }


    private String respostaSeguranca() {
        return "Sinto muito que voce esteja passando por um momento tao dificil.\n"
                + "Esta plataforma nao substitui ajuda profissional. Procure imediatamente um profissional de saude, um servico de emergencia ou alguem de confianca que possa estar com voce agora.\n"
                + "Voce nao precisa lidar com isso sozinho(a).";
    }

    private String gerarRespostaFallback(String perfil, String mensagemUsuario, String risco) {
        if (risco.equals("ATENCAO")) {
            return "Percebo que voce esta passando por um momento delicado. Estou aqui para acolher sua mensagem, mas esta plataforma realiza apenas apoio inicial e nao substitui acompanhamento psicologico profissional.";
        }

        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        switch (perfil) {
            case "ANSIEDADE":
                return "Entendo que voce esta se sentindo preocupado(a). Posso te ouvir com calma, mas lembro que esta plataforma atua apenas como apoio inicial.";

            case "DEPRESSAO":
                return "Sinto muito que voce esteja passando por esse momento. Voce nao precisa lidar com isso sozinho(a), e buscar apoio profissional pode ser importante.";

            case "MISTO":
                return "Percebo que seu formulario trouxe diferentes sinais de preocupacao e sobrecarga emocional. Podemos conversar com calma, mas uma avaliacao adequada deve ser feita por um profissional.";

            default:
                return "Estou aqui para te ouvir e oferecer apoio inicial, sem substituir acompanhamento profissional.";
        }
    }

    private String limparRespostaClinica(String resposta) {
        if (resposta == null || resposta.trim().isEmpty()) {
            return "Estou aqui para te ouvir e organizar melhor o que voce esta sentindo. Esta plataforma realiza apenas apoio inicial e triagem emocional, sem substituir acompanhamento profissional.";
        }

        String texto = resposta.trim();

        texto = removerAspasExternas(texto);

        texto = texto.replace(
                "podem ser sintomas de um desequilíbrio emocional",
                "podem estar relacionados a um momento de estresse ou sobrecarga emocional"
        );

        texto = texto.replace(
                "podem ser sintomas de um desequilibrio emocional",
                "podem estar relacionados a um momento de estresse ou sobrecarga emocional"
        );

        texto = texto.replace(
                "sintomas de um desequilíbrio emocional",
                "sinais de um momento de estresse ou sobrecarga emocional"
        );

        texto = texto.replace(
                "sintomas de um desequilibrio emocional",
                "sinais de um momento de estresse ou sobrecarga emocional"
        );

        texto = texto.replace(
                "sintomas de ansiedade",
                "sinais de preocupacao ou sobrecarga"
        );

        texto = texto.replace(
                "sintomas de depressão",
                "sinais de tristeza ou desanimo"
        );

        texto = texto.replace(
                "sintomas de depressao",
                "sinais de tristeza ou desanimo"
        );

        texto = texto.replace(
                "transtorno de ansiedade",
                "momento de ansiedade ou preocupacao intensa"
        );

        texto = texto.replace(
                "transtorno depressivo",
                "momento de tristeza ou desanimo persistente"
        );

        texto = texto.replace(
                "doença mental",
                "questao de saude emocional"
        );

        texto = texto.replace(
                "doenca mental",
                "questao de saude emocional"
        );

        texto = texto.replace(
                "diagnosticar",
                "compreender melhor"
        );

        texto = texto.replace(
                "diagnóstico",
                "triagem inicial"
        );

        texto = texto.replace(
                "diagnostico",
                "triagem inicial"
        );

        texto = texto.replace(
                "tratamento",
                "apoio profissional"
        );

        texto = texto.replace(
                "tratar",
                "apoiar"
        );

        texto = texto.replace(
                "consulta de um psicólogo",
                "acompanhamento de um psicologo"
        );

        texto = texto.replace(
                "consulta de um psicologo",
                "acompanhamento de um psicologo"
        );

        texto = texto.replace(
                "consultoria de um profissional qualificado",
                "apoio de um profissional qualificado"
        );

        return texto;
    }

    private String removerAspasExternas(String texto) {
        if (texto == null) {
            return "";
        }

        String resultado = texto.trim();

        if (resultado.length() >= 2
                && resultado.startsWith("\"")
                && resultado.endsWith("\"")) {
            resultado = resultado.substring(1, resultado.length() - 1).trim();
        }

        return resultado;
    }

    private String consultarConteudo(String usuarioId, String perfil, String risco, String mensagemUsuario) {
        String conversationId = criarConversationId("CONTEUDO");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteConteudo", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "usuarioId=" + valorSeguroAcl(usuarioId)
                + ";perfil=" + perfil
                + ";risco=" + risco
                + ";mensagem=" + mensagemUsuario
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteConteudo",
                conversationId,
                TIMEOUT_CONTEUDO_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteConteudo. conversationId="
                + conversationId
        );

        return "Nao foi possivel consultar os conteudos validados no momento.";
    }

    private String consultarPsicologo(String usuarioId, String perfil, String risco) {
        String conversationId = criarConversationId("PSICOLOGO");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agentePsicologo", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "usuarioId=" + valorSeguroAcl(usuarioId)
                + ";perfil=" + perfil
                + ";risco=" + risco
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agentePsicologo",
                conversationId,
                TIMEOUT_PSICOLOGO_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgentePsicologo. conversationId="
                + conversationId
        );

        return "Nao foi possivel consultar profissionais no momento.";
    }

    private String consultarLocalAtendimento(String usuarioId, String cidade, String uf) {
        String conversationId = criarConversationId("LOCAL");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteLocalAtendimento", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "usuarioId=" + valorSeguroAcl(usuarioId)
                + ";cidade=" + cidade
                + ";uf=" + uf
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteLocalAtendimento",
                conversationId,
                TIMEOUT_LOCAL_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteLocalAtendimento. conversationId="
                + conversationId
        );

        return "Nao foi possivel consultar locais de atendimento no momento.";
    }

    private String consultarIntervencao(String usuarioId, String nivelRisco, String perfil, String mensagemUsuario) {
        String conversationId = criarConversationId("INTERVENCAO");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteIntervencao", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "usuarioId=" + valorSeguroAcl(usuarioId)
                + ";nivelRisco=" + nivelRisco
                + ";perfil=" + perfil
                + ";mensagem=" + mensagemUsuario
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteIntervencao",
                conversationId,
                TIMEOUT_INTERVENCAO_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteIntervencao. "
                + "Aplicando protocolo local de contingencia. conversationId="
                + conversationId
        );

        return gerarIntervencaoFallback(nivelRisco);
    }

    private void salvarNaMemoria(String usuarioId, String tipo, String valor) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));

        msg.setContent(
                "tipo=" + tipo
                + ";usuarioId=" + valorSeguroAcl(usuarioId)
                + ";valor=" + valor
        );

        send(msg);
    }

    private String consultarSeguranca(
            String mensagemUsuario,
            String memoriaContexto) {

        String conversationId =
                criarConversationId(
                        "SEGURANCA"
                );

        ACLMessage msg =
                new ACLMessage(
                        ACLMessage.REQUEST
                );

        msg.addReceiver(
                new AID(
                        "agenteSeguranca",
                        AID.ISLOCALNAME
                )
        );

        msg.setConversationId(
                conversationId
        );

        msg.setReplyWith(
                conversationId
        );

        String mensagemBase64 =
                Base64.getEncoder()
                        .encodeToString(
                                valorSeguroAcl(
                                        mensagemUsuario
                                )
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

        String contextoBase64 =
                Base64.getEncoder()
                        .encodeToString(
                                valorSeguroAcl(
                                        memoriaContexto
                                )
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

        msg.setContent(
                "formato=CONTEXTO_V1;"
                + "mensagemBase64="
                + mensagemBase64
                + ";contextoBase64="
                + contextoBase64
        );

        send(msg);

        ACLMessage resposta =
                aguardarResposta(
                        "agenteSeguranca",
                        conversationId,
                        TIMEOUT_SEGURANCA_MS
                );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteSeguranca. "
                + "Usando ATENCAO como contingencia conservadora. conversationId="
                + conversationId
        );

        return "ATENCAO";
    }

    private String consultarMemoria(String usuarioId) {
        String conversationId = criarConversationId("MEMORIA");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "tipo=consulta;"
                + "usuarioId=" + valorSeguroAcl(usuarioId)
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteMemoria",
                conversationId,
                TIMEOUT_MEMORIA_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteMemoria. "
                + "O fluxo continuara sem contexto historico. conversationId="
                + conversationId
        );

        return "";
    }

    private ACLMessage aguardarResposta(
            String agenteEsperado,
            String conversationId,
            long timeoutMs) {

        AID remetenteEsperado =
                new AID(agenteEsperado, AID.ISLOCALNAME);

        MessageTemplate templateConversation =
                MessageTemplate.MatchConversationId(conversationId);

        MessageTemplate templateRemetente =
                MessageTemplate.MatchSender(remetenteEsperado);

        MessageTemplate template =
                MessageTemplate.and(
                        templateConversation,
                        templateRemetente
                );

        return blockingReceive(template, timeoutMs);
    }

    private String criarConversationId(String etapa) {
        return "CONV_"
                + etapa
                + "_"
                + UUID.randomUUID().toString();
    }

    private String gerarIntervencaoFallback(String nivelRisco) {
        if ("RISCO".equalsIgnoreCase(nivelRisco)) {
            return "nivelRisco=RISCO;"
                    + "protocolo=PROTOCOLO_PRESERVACAO_DA_VIDA;"
                    + "permitirIA=false;"
                    + "permitirConteudo=false;"
                    + "permitirPsicologo=false;"
                    + "permitirLocalAtendimento=true;"
                    + "permitirMonitoramento=false;"
                    + "exibirBotaoCVV=true;"
                    + "telefoneCVV=188;"
                    + "mensagem=Busque apoio imediato e evite permanecer sozinho. "
                    + "Procure uma pessoa de confianca ou um servico de urgencia.";
        }

        if ("ATENCAO".equalsIgnoreCase(nivelRisco)) {
            return "nivelRisco=ATENCAO;"
                    + "protocolo=PROTOCOLO_ENCAMINHAMENTO_SEGURO;"
                    + "permitirIA=true;"
                    + "permitirConteudo=true;"
                    + "permitirPsicologo=true;"
                    + "permitirLocalAtendimento=true;"
                    + "permitirMonitoramento=false;"
                    + "exibirBotaoCVV=true;"
                    + "telefoneCVV=188;"
                    + "mensagem=Considere buscar apoio profissional ou conversar "
                    + "com uma pessoa de confianca.";
        }

        return "nivelRisco=BAIXO_RISCO;"
                + "protocolo=PROTOCOLO_APOIO_INICIAL;"
                + "permitirIA=true;"
                + "permitirConteudo=true;"
                + "permitirPsicologo=true;"
                + "permitirLocalAtendimento=true;"
                + "permitirMonitoramento=true;"
                + "exibirBotaoCVV=false;"
                + "telefoneCVV=188;"
                + "mensagem=Fluxo de apoio inicial autorizado.";
    }

    private String valorSeguroAcl(String valor) {
        if (valor == null) {
            return "";
        }

        return valor
                .replace(";", "")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    private String decodificarBase64(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "";
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(valor.trim());

            return new String(
                    bytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            System.out.println(
                    "[CONVERSACIONAL] Nao foi possivel decodificar a mensagem da API: "
                    + e.getMessage()
            );

            return "";
        }
    }

    private boolean extrairBoolean(String texto, String chave, boolean valorPadrao) {
        String valor = extrairValor(texto, chave);

        if (valor == null || valor.trim().isEmpty()) {
            return valorPadrao;
        }

        return valor.equalsIgnoreCase("true")
                || valor.equalsIgnoreCase("sim")
                || valor.equalsIgnoreCase("1");
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
}