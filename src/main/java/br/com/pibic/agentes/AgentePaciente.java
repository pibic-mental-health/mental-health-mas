package br.com.pibic.agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class AgentePaciente extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Paciente iniciado: " + getLocalName());

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {

                String formulario =
                        "nome=Maria;"
                      + "preocupacao=4;"
                      + "nervosismo=4;"
                      + "relaxamento=3;"
                      + "sono=2;"
                      + "tristeza=2;"
                      + "energia=1;"
                      + "interesse=1;"
                      + "isolamento=2;";

                System.out.println("\n[PACIENTE] Respondendo formulario:");
                System.out.println(formulario);

                ACLMessage msgTriagem = new ACLMessage(ACLMessage.REQUEST);
                msgTriagem.addReceiver(new AID("agenteTriagem", AID.ISLOCALNAME));
                msgTriagem.setContent(formulario);
                send(msgTriagem);

                ACLMessage respostaTriagem = blockingReceive();

                if (respostaTriagem != null) {
                    String perfil = respostaTriagem.getContent();

                    System.out.println("[PACIENTE] Perfil recebido da triagem: " + perfil);

                    String mensagemChat = "Hoje estou muito preocupado e com dificuldade para relaxar.";

                    String conteudoChat = "perfil=" + perfil
                            + ";mensagem=" + mensagemChat;

                    ACLMessage msgChat = new ACLMessage(ACLMessage.REQUEST);
                    msgChat.addReceiver(new AID("agenteConversacional", AID.ISLOCALNAME));
                    msgChat.setContent(conteudoChat);

                    System.out.println("\n[PACIENTE] Enviando mensagem ao chat:");
                    System.out.println(mensagemChat);

                    send(msgChat);

                    ACLMessage respostaChat = blockingReceive();

                    if (respostaChat != null) {
                        String textoRespostaChat = respostaChat.getContent();

                        System.out.println("\n[PACIENTE] Resposta recebida do chat:");
                        System.out.println(textoRespostaChat);

                        if (textoRespostaChat.toLowerCase().contains("sugestoes de conteudo")
                                || textoRespostaChat.toLowerCase().contains("conteudos de apoio")) {
                            responderConteudo(perfil, mensagemChat);
                        }

                        if (textoRespostaChat.toLowerCase().contains("psicologos cadastrados")
                                || textoRespostaChat.toLowerCase().contains("indicacoes")) {
                            responderPsicologo(perfil);
                        }

                        if (textoRespostaChat.toLowerCase().contains("locais demonstrativos de atendimento")
                                || textoRespostaChat.toLowerCase().contains("locais de atendimento proximos")) {
                            responderLocalAtendimento();
                        }

                        if (textoRespostaChat.toLowerCase().contains("simulacao de acompanhamento")
                                || textoRespostaChat.toLowerCase().contains("simulacao academica")) {
                            responderMonitoramentoSimulado(perfil);
                        }
                    }
                }
            }
        });
    }

    private void responderConteudo(String perfil, String mensagemChat) {
        String respostaConteudo = "sim";

        System.out.println("\n[PACIENTE] Respondendo convite de conteudo:");
        System.out.println(respostaConteudo);

        ACLMessage msgConteudo = new ACLMessage(ACLMessage.REQUEST);
        msgConteudo.addReceiver(new AID("agenteConversacional", AID.ISLOCALNAME));
        msgConteudo.setContent("perfil=" + perfil
                + ";mensagem=" + mensagemChat
                + ";respostaConteudo=" + respostaConteudo);

        send(msgConteudo);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            System.out.println("\n[PACIENTE] Conteudo recebido:");
            System.out.println(resposta.getContent());
        }
    }

    private void responderPsicologo(String perfil) {
        String respostaPsicologo = "sim";

        System.out.println("\n[PACIENTE] Respondendo convite de psicologo:");
        System.out.println(respostaPsicologo);

        ACLMessage msgPsicologo = new ACLMessage(ACLMessage.REQUEST);
        msgPsicologo.addReceiver(new AID("agenteConversacional", AID.ISLOCALNAME));
        msgPsicologo.setContent("perfil=" + perfil
                + ";risco=BAIXO_RISCO"
                + ";respostaPsicologo=" + respostaPsicologo);

        send(msgPsicologo);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            System.out.println("\n[PACIENTE] Indicacoes de psicologos recebidas:");
            System.out.println(resposta.getContent());
        }
    }

    private void responderLocalAtendimento() {
        String respostaLocalAtendimento = "sim";

        System.out.println("\n[PACIENTE] Respondendo convite de locais de atendimento:");
        System.out.println(respostaLocalAtendimento);

        ACLMessage msgLocal = new ACLMessage(ACLMessage.REQUEST);
        msgLocal.addReceiver(new AID("agenteConversacional", AID.ISLOCALNAME));
        msgLocal.setContent("cidade=Brasilia;uf=DF;respostaLocalAtendimento=" + respostaLocalAtendimento);

        send(msgLocal);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            System.out.println("\n[PACIENTE] Locais de atendimento recebidos:");
            System.out.println(resposta.getContent());
        }
    }

    private void responderMonitoramentoSimulado(String perfil) {
        String respostaMonitoramento = "sim";

        System.out.println("\n[PACIENTE] Respondendo convite de simulacao de acompanhamento:");
        System.out.println(respostaMonitoramento);

        ACLMessage msgMonitoramento = new ACLMessage(ACLMessage.REQUEST);
        msgMonitoramento.addReceiver(new AID("agenteConversacional", AID.ISLOCALNAME));
        msgMonitoramento.setContent("perfil=" + perfil
                + ";respostaMonitoramento=" + respostaMonitoramento);

        send(msgMonitoramento);

        ACLMessage confirmacaoMonitoramento = blockingReceive();

        if (confirmacaoMonitoramento != null) {
            System.out.println("\n[PACIENTE] Confirmacao da simulacao recebida:");
            System.out.println(confirmacaoMonitoramento.getContent());
        }

        ACLMessage relatorioSimulado = blockingReceive();

        if (relatorioSimulado != null) {
            System.out.println("\n[PACIENTE] Relatorio simulado recebido:");
            System.out.println(relatorioSimulado.getContent());
        }
    }
}