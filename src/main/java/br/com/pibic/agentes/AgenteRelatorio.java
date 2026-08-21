package br.com.pibic.agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteRelatorio extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Relatorio Simulado iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                ACLMessage mensagem = receive();

                if (mensagem != null) {

                    String conteudo = mensagem.getContent();

                    System.out.println("\n[RELATORIO SIMULADO] Dados recebidos:");
                    System.out.println(conteudo);

                    String perfil = extrairValor(conteudo, "perfil");
                    String dados = extrairDados(conteudo);

                    String relatorio = gerarRelatorioSimulado(perfil, dados);

                    System.out.println("\n[RELATORIO SIMULADO GERADO]");
                    System.out.println(relatorio);

                    enviarRelatorioParaConversacional(perfil, relatorio);

                } else {
                    block();
                }
            }
        });
    }

    private void enviarRelatorioParaConversacional(String perfil, String relatorio) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("agenteConversacional", AID.ISLOCALNAME));
        msg.setContent("tipo=relatorio_simulado;perfil=" + perfil + ";relatorio=" + relatorio);

        send(msg);

        System.out.println("\n[RELATORIO SIMULADO] Relatorio enviado para o AgenteConversacional");
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

    private String extrairDados(String texto) {
        String[] partes = texto.split(";");

        StringBuilder dados = new StringBuilder();

        boolean coletar = false;

        for (String parte : partes) {

            if (parte.startsWith("dados=")) {
                coletar = true;
                dados.append(parte.replace("dados=", ""));
                continue;
            }

            if (coletar) {
                dados.append(";").append(parte);
            }
        }

        return dados.toString();
    }

    private String gerarRelatorioSimulado(String perfil, String dados) {

        String[] dias = dados.split(";");

        int somaAnsiedade = 0;
        int somaHumor = 0;
        int somaEnergia = 0;
        int somaSono = 0;

        int totalDias = 0;

        for (String dia : dias) {

            if (dia.trim().isEmpty()) continue;

            String[] campos = dia.split(",");

            int ansiedade = 0;
            int humor = 0;
            int energia = 0;
            int sono = 0;

            for (String campo : campos) {

                String[] kv = campo.split("=");

                if (kv.length == 2) {
                    String chave = kv[0].trim();

                    try {
                        int valor = Integer.parseInt(kv[1].trim());

                        if (chave.equals("ansiedade")) ansiedade = valor;
                        if (chave.equals("humor")) humor = valor;
                        if (chave.equals("energia")) energia = valor;
                        if (chave.equals("sono")) sono = valor;

                    } catch (NumberFormatException e) {
                        System.out.println("[RELATORIO SIMULADO] Valor numerico invalido ignorado: " + campo);
                    }
                }
            }

            somaAnsiedade += ansiedade;
            somaHumor += humor;
            somaEnergia += energia;
            somaSono += sono;

            totalDias++;
        }

        if (totalDias == 0) {
            return "Nao foi possivel gerar relatorio simulado: dados insuficientes.";
        }

        double mediaAnsiedade = (double) somaAnsiedade / totalDias;
        double mediaHumor = (double) somaHumor / totalDias;
        double mediaEnergia = (double) somaEnergia / totalDias;
        double mediaSono = (double) somaSono / totalDias;

        String interpretacao;

        if (mediaAnsiedade >= 3) {
            interpretacao = "Na simulacao, foi observado um nivel elevado de ansiedade ao longo da semana.";
        } else if (mediaHumor <= 1) {
            interpretacao = "Na simulacao, foi observado um nivel baixo de humor ao longo da semana.";
        } else {
            interpretacao = "Na simulacao, os indicadores sugerem estabilidade emocional relativa.";
        }

        return "SIMULACAO DE RELATORIO SEMANAL\n"
                + "Perfil usado na simulacao: " + perfil + "\n"
                + "Este relatorio possui finalidade academica e demonstrativa.\n"
                + "Nao representa avaliacao clinica, diagnostico ou acompanhamento psicologico real.\n\n"
                + "- Media simulada de ansiedade: " + formatarMedia(mediaAnsiedade) + "\n"
                + "- Media simulada de humor: " + formatarMedia(mediaHumor) + "\n"
                + "- Media simulada de energia: " + formatarMedia(mediaEnergia) + "\n"
                + "- Media simulada de sono: " + formatarMedia(mediaSono) + "\n\n"
                + interpretacao + "\n"
                + "Observacao: em um uso real, esta etapa dependeria de aprovacao etica e supervisao profissional.";
    }

    private String formatarMedia(double valor) {
        return String.format(java.util.Locale.ROOT, "%.2f", valor);
    }

}