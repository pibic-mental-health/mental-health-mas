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

                    String resposta;

                    if (risco.equals("RISCO")) {
                        resposta = respostaSeguranca();
                    } else {
                        String memoria = consultarMemoria();
                        String prompt = montarPrompt(perfil, mensagemUsuario, risco, memoria);

                        System.out.println("[CONVERSACIONAL] Provedor de IA configurado: " + ClienteLLM.obterProvedor());
                        resposta = ClienteLLM.gerarResposta(prompt);

                        if (resposta == null || resposta.trim().isEmpty() || resposta.contains("Erro")) {
                            resposta = gerarRespostaFallback(perfil, mensagemUsuario, risco);
                        }

                        resposta += "\n\nImportante: esta plataforma realiza apenas apoio inicial e triagem emocional. Ela nao substitui acompanhamento psicologico profissional.";

                        resposta += "\n\nSe voce quiser, posso sugerir alguns conteudos de apoio, como meditacao, respiracao, musica ou textos educativos. Deseja receber sugestoes de conteudo? (sim/nao)";

                        if (deveSugerirPsicologo(perfil, risco)) {
                            resposta += "\n\nTambem posso selecionar alguns psicologos cadastrados que tenham relacao com o perfil identificado na triagem. Deseja ver essas indicacoes? (sim/nao)";
                        }

                        resposta += "\n\nTambem posso mostrar locais demonstrativos de atendimento, como UBS, CAPS ou servicos publicos de saude mental. Deseja visualizar locais de atendimento proximos? (sim/nao)";

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
            reply.setContent("Tudo bem. Posso sugerir conteudos de apoio depois, se voce quiser.");
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
            reply.setContent("Tudo bem. Voce pode pedir indicacoes de profissionais quando se sentir confortavel.");
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
            reply.setContent("Tudo bem. A busca por locais de atendimento pode ser feita depois, se necessario.");
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

            reply.setContent("Tudo bem. A simulacao de acompanhamento pode ser visualizada depois, se necessario.");
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

    private String montarPrompt(String perfil, String mensagemUsuario, String risco, String memoria) {
        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        return "Voce e um assistente empatico de apoio inicial em uma plataforma academica de triagem emocional.\n"
                + "A plataforma nao realiza diagnostico, tratamento ou acompanhamento psicologico real.\n"
                + "Ela apenas organiza informacoes iniciais e incentiva a busca por profissionais qualificados.\n\n"
                + "Informacoes do usuario:\n"
                + memoria + "\n\n"
                + "Mensagem atual do usuario: " + mensagemUsuario + "\n\n"
                + "Regras obrigatorias:\n"
                + "- Use o nome do usuario se estiver disponivel.\n"
                + "- Seja acolhedor, breve e humano.\n"
                + "- Nao afirme diagnosticos.\n"
                + "- Nao diga que esta tratando o usuario.\n"
                + "- Nao prometa acompanhamento clinico.\n"
                + "- Reforce que a plataforma nao substitui psicologo.\n"
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
            return "Percebo que voce esta passando por um momento delicado. Estou aqui para acolher sua mensagem, mas esta plataforma nao substitui apoio psicologico profissional.";
        }

        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        switch (perfil) {
            case "ANSIEDADE":
                return "Entendo que voce esta se sentindo preocupado(a). Posso te ouvir com calma, mas lembro que esta plataforma atua apenas como apoio inicial.";

            case "DEPRESSAO":
                return "Sinto muito que voce esteja passando por isso. Voce nao esta sozinho(a), e buscar apoio profissional pode ser importante.";

            case "MISTO":
                return "Percebo que existem sinais mistos no seu formulario. Podemos conversar com calma, mas a avaliacao adequada deve ser feita por um profissional.";

            default:
                return "Estou aqui para te ouvir e oferecer apoio inicial, sem substituir acompanhamento profissional.";
        }
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

    private String extrairValor(String texto, String chave) {
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