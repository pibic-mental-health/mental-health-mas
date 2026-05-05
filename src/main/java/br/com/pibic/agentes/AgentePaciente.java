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

                        if (textoRespostaChat.toLowerCase().contains("acompanhamento diario")) {
                            String respostaMonitoramento = "sim";

                            System.out.println("\n[PACIENTE] Respondendo convite de monitoramento:");
                            System.out.println(respostaMonitoramento);

                            ACLMessage msgMonitoramento = new ACLMessage(ACLMessage.REQUEST);
                            msgMonitoramento.addReceiver(new AID("agenteConversacional", AID.ISLOCALNAME));
                            msgMonitoramento.setContent("perfil=" + perfil + ";respostaMonitoramento=" + respostaMonitoramento);

                            send(msgMonitoramento);

                            ACLMessage confirmacaoMonitoramento = blockingReceive();

                            if (confirmacaoMonitoramento != null) {
                                System.out.println("\n[PACIENTE] Confirmacao recebida:");
                                System.out.println(confirmacaoMonitoramento.getContent());
                            }
                        }
                    }
                }
            }
        });
    }
}