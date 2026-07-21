package br.com.pibic.agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteConversacional extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Conversacional iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

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

                    System.out.println("\n[CONVERSACIONAL] Conteudo recebido:");
                    System.out.println(conteudo);
                    System.out.println("[CONVERSACIONAL] Perfil recebido: " + perfil);
                    System.out.println("[CONVERSACIONAL] Mensagem do usuario: " + mensagemUsuario);

                    salvarNaMemoria("perfil", perfil);
                    salvarNaMemoria("mensagem", mensagemUsuario);

                    String risco = consultarSeguranca(mensagemUsuario);
                    salvarNaMemoria("risco", risco);

                    System.out.println("[CONVERSACIONAL] Nivel de risco: " + risco);

                    String decisaoIntervencao = consultarIntervencao(risco, perfil, mensagemUsuario);
                    String protocolo = extrairValor(decisaoIntervencao, "protocolo");
                    String mensagemIntervencao = extrairValor(decisaoIntervencao, "mensagem");

                    boolean permitirIA = extrairBoolean(decisaoIntervencao, "permitirIA", true);
                    boolean permitirConteudo = extrairBoolean(decisaoIntervencao, "permitirConteudo", true);
                    boolean permitirPsicologo = extrairBoolean(decisaoIntervencao, "permitirPsicologo", true);
                    boolean permitirLocalAtendimento = extrairBoolean(decisaoIntervencao, "permitirLocalAtendimento", true);
                    boolean permitirMonitoramento = extrairBoolean(decisaoIntervencao, "permitirMonitoramento", true);
                    boolean exibirBotaoCVV = extrairBoolean(decisaoIntervencao, "exibirBotaoCVV", false);
                    String telefoneCVV = extrairValor(decisaoIntervencao, "telefoneCVV");

                    salvarNaMemoria("protocolo_intervencao", protocolo);

                    System.out.println("[CONVERSACIONAL] Protocolo de intervencao: " + protocolo);

                    String resposta;

                    if (!permitirIA || risco.equals("RISCO")) {
                        resposta = mensagemIntervencao;

                        if (resposta == null || resposta.trim().isEmpty()) {
                            resposta = respostaSeguranca();
                        }
                    } else {
                        String memoria = consultarMemoria();
                        String prompt = montarPrompt(perfil, mensagemUsuario, risco, memoria, protocolo);

                        System.out.println("[CONVERSACIONAL] Provedor de IA configurado: " + ClienteLLM.obterProvedor());
                        resposta = ClienteLLM.gerarResposta(prompt);
                        resposta = limparRespostaClinica(resposta);

                        if (resposta == null || resposta.trim().isEmpty() || resposta.contains("Erro")) {
                            resposta = gerarRespostaFallback(perfil, mensagemUsuario, risco);
                        }

                        if (mensagemIntervencao != null
                                && !mensagemIntervencao.trim().isEmpty()
                                && risco.equals("ATENCAO")) {
                            resposta += "\n\n" + mensagemIntervencao;
                        }
                    }

                    resposta += "\n\nImportante: esta plataforma realiza apenas apoio inicial e triagem emocional. Ela nao substitui acompanhamento psicologico profissional.";

                    if (exibirBotaoCVV) {
                        if (telefoneCVV == null || telefoneCVV.trim().isEmpty()) {
                            telefoneCVV = "188";
                        }

                        resposta += "\n\n[ACAO_RECOMENDADA] Ligar para o CVV pelo telefone " + telefoneCVV + ".";
                        resposta += "\n[ACAO_RECOMENDADA] Procurar um servico de emergencia ou alguem de confianca que possa estar com voce agora.";
                    }

                    if (permitirConteudo) {
                        resposta += "\n\nPosso sugerir alguns conteudos de apoio, como meditacao, respiracao, musica ou textos educativos. Deseja receber sugestoes de conteudo? (sim/nao)";
                    }

                    if (permitirPsicologo && deveSugerirPsicologo(perfil, risco)) {
                        resposta += "\n\nTambem posso selecionar alguns psicologos cadastrados que tenham relacao com o perfil identificado na triagem. Deseja ver essas indicacoes? (sim/nao)";
                    }

                    if (permitirLocalAtendimento) {
                        resposta += "\n\nTambem posso mostrar locais demonstrativos de atendimento, como UBS, CAPS ou servicos publicos de saude mental. Deseja visualizar locais de atendimento proximos? (sim/nao)";
                    }

                    if (permitirMonitoramento) {
                        resposta += "\n\nPara fins academicos, tambem posso mostrar uma simulacao de acompanhamento semanal, demonstrando como um profissional poderia acompanhar indicadores emocionais com apoio do sistema. Deseja visualizar essa simulacao? (sim/nao)";
                    }

                    salvarNaMemoria("resposta", resposta);

                    System.out.println("[CONVERSACIONAL] Resposta gerada:");
                    System.out.println(resposta);

                    ACLMessage reply = mensagem.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(resposta);

                    send(reply);
                } else {
                    block();
                }
            }
        });
    }

    private void tratarRelatorioSimulado(String conteudo) {
        String perfil = extrairValor(conteudo, "perfil");
        String relatorio = extrairValor(conteudo, "relatorio");

        System.out.println("\n[CONVERSACIONAL] Relatorio simulado recebido do AgenteRelatorio:");
        System.out.println(relatorio);

        salvarNaMemoria("relatorio_simulado", "gerado");

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
        String perfil = extrairValor(conteudo, "perfil");
        String mensagemUsuario = extrairValor(conteudo, "mensagem");

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaConteudo.equalsIgnoreCase("sim")) {
            String sugestao = consultarConteudo(perfil, mensagemUsuario);
            salvarNaMemoria("conteudo", "aceito");
            reply.setContent(sugestao);
        } else {
            salvarNaMemoria("conteudo", "recusado");
            reply.setContent("Tudo bem. Os conteudos de apoio podem ser acessados em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaPsicologo(ACLMessage mensagemOriginal, String conteudo, String respostaPsicologo) {
        String perfil = extrairValor(conteudo, "perfil");
        String risco = extrairValor(conteudo, "risco");

        if (risco == null || risco.isEmpty()) {
            risco = "BAIXO_RISCO";
        }

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaPsicologo.equalsIgnoreCase("sim")) {
            String psicologos = consultarPsicologo(perfil, risco);
            salvarNaMemoria("psicologo", "aceito");
            reply.setContent(psicologos);
        } else {
            salvarNaMemoria("psicologo", "recusado");
            reply.setContent("Tudo bem. As indicacoes de profissionais podem ser consultadas em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaLocalAtendimento(ACLMessage mensagemOriginal, String conteudo, String respostaLocalAtendimento) {
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
            String locais = consultarLocalAtendimento(cidade, uf);
            salvarNaMemoria("local_atendimento", "aceito");
            reply.setContent(locais);
        } else {
            salvarNaMemoria("local_atendimento", "recusado");
            reply.setContent("Tudo bem. A busca por locais de atendimento pode ser feita em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaMonitoramento(ACLMessage mensagemOriginal, String conteudo, String respostaMonitoramento) {
        String perfil = extrairValor(conteudo, "perfil");

        System.out.println("\n[CONVERSACIONAL] Resposta sobre simulacao de monitoramento recebida: " + respostaMonitoramento);

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaMonitoramento.equalsIgnoreCase("sim")) {
            ACLMessage msgMonitoramento = new ACLMessage(ACLMessage.INFORM);
            msgMonitoramento.addReceiver(new AID("agenteMonitoramento", AID.ISLOCALNAME));
            msgMonitoramento.setContent("iniciar=true;perfil=" + perfil + ";modo=simulado");
            send(msgMonitoramento);

            salvarNaMemoria("monitoramento_simulado", "aceito");

            reply.setContent("Perfeito. Vou iniciar uma simulacao academica de acompanhamento semanal. Esses dados sao demonstrativos e nao representam avaliacao clinica.");
        } else {
            salvarNaMemoria("monitoramento_simulado", "recusado");

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
                + "- Nao afirme diagnosticos.\n"
                + "- Nao diga que o usuario possui transtorno, doenca ou condicao clinica.\n"
                + "- Nao diga que esta tratando o usuario.\n"
                + "- Nao prometa melhora.\n"
                + "- Nao prometa acompanhamento clinico.\n"
                + "- Evite termos clinicos fortes como diagnostico, tratamento, transtorno, doenca ou sintomas.\n"
                + "- Prefira termos como sinais, momento de estresse, sobrecarga emocional, preocupacao, desconforto e apoio inicial.\n"
                + "- Reforce que a plataforma nao substitui acompanhamento psicologico profissional.\n"
                + "- Quando fizer sentido, incentive a busca por apoio profissional.\n\n"
                + "Responda em portugues do Brasil com no maximo 150 palavras.";
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

        String texto = resposta;

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

    private String consultarConteudo(String perfil, String mensagemUsuario) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteConteudo", AID.ISLOCALNAME));
        msg.setContent("perfil=" + perfil + ";mensagem=" + mensagemUsuario);

        send(msg);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            return resposta.getContent();
        }

        return "";
    }

    private String consultarPsicologo(String perfil, String risco) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agentePsicologo", AID.ISLOCALNAME));
        msg.setContent("perfil=" + perfil + ";risco=" + risco);

        send(msg);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            return resposta.getContent();
        }

        return "";
    }

    private String consultarLocalAtendimento(String cidade, String uf) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteLocalAtendimento", AID.ISLOCALNAME));
        msg.setContent("cidade=" + cidade + ";uf=" + uf);

        send(msg);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            return resposta.getContent();
        }

        return "Nao foi possivel consultar locais de atendimento no momento.";
    }

    private String consultarIntervencao(String nivelRisco, String perfil, String mensagemUsuario) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteIntervencao", AID.ISLOCALNAME));
        msg.setContent("nivelRisco=" + nivelRisco + ";perfil=" + perfil + ";mensagem=" + mensagemUsuario);

        send(msg);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            return resposta.getContent();
        }

        return "nivelRisco=" + nivelRisco + ";"
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

    private void salvarNaMemoria(String tipo, String valor) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));
        msg.setContent("tipo=" + tipo + ";valor=" + valor);
        send(msg);
    }

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