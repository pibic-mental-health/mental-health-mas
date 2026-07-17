package br.com.pibic.agentes;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteMonitoramento extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Monitoramento Simulado iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    if (conteudo.contains("iniciar=true")) {
                        String perfil = extrairValor(conteudo, "perfil");
                        String modo = extrairValor(conteudo, "modo");

                        if (perfil == null || perfil.isEmpty()) {
                            perfil = "GERAL";
                        }

                        iniciarMonitoramentoSimulado(perfil, modo);
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void iniciarMonitoramentoSimulado(String perfil, String modo) {
        System.out.println("\n[MONITORAMENTO SIMULADO] Iniciando simulacao academica de acompanhamento semanal...");
        System.out.println("[MONITORAMENTO SIMULADO] Perfil usado na simulacao: " + perfil);
        System.out.println("[MONITORAMENTO SIMULADO] Observacao: dados gerados artificialmente para demonstracao tecnica.");

        StringBuilder dadosSemana = new StringBuilder();
        Random random = new Random();

        for (int dia = 1; dia <= 7; dia++) {
            int ansiedade = random.nextInt(5);
            int humor = random.nextInt(5);
            int energia = random.nextInt(5);
            int sono = random.nextInt(5);

            String registro = "dia=" + dia
                    + ",ansiedade=" + ansiedade
                    + ",humor=" + humor
                    + ",energia=" + energia
                    + ",sono=" + sono;

            System.out.println("[MONITORAMENTO SIMULADO] " + registro);

            dadosSemana.append(registro).append(";");
        }

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteRelatorio", AID.ISLOCALNAME));
        msg.setContent("perfil=" + perfil + ";modo=simulado;dados=" + dadosSemana.toString());

        send(msg);

        System.out.println("\n[MONITORAMENTO SIMULADO] Dados simulados enviados para o AgenteRelatorio");
    }

    private String extrairValor(String texto, String chave) {
        String[] partes = texto.split(";");

        for (String parte : partes) {
            String[] chaveValor = parte.split("=", 2);

            if (chaveValor.length == 2 && chaveValor[0].trim().equalsIgnoreCase(chave)) {
                return chaveValor[1].trim();
            }
        }

        return "";
    }
}