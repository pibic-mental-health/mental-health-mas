package br.com.pibic.agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteRelatorio extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Relatorio iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                ACLMessage mensagem = receive();

                if (mensagem != null) {

                    String conteudo = mensagem.getContent();

                    System.out.println("\n[RELATORIO] Dados recebidos:");
                    System.out.println(conteudo);

                    String dados = extrairValor(conteudo, "dados");

                    String relatorio = gerarRelatorio(dados);

                    System.out.println("\n[RELATORIO GERADO]");
                    System.out.println(relatorio);

                } else {
                    block();
                }
            }
        });
    }

    private String extrairValor(String texto, String chave) {
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

    private String gerarRelatorio(String dados) {

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

                    String chave = kv[0];
                    int valor = Integer.parseInt(kv[1]);

                    if (chave.equals("ansiedade")) ansiedade = valor;
                    if (chave.equals("humor")) humor = valor;
                    if (chave.equals("energia")) energia = valor;
                    if (chave.equals("sono")) sono = valor;
                }
            }

            somaAnsiedade += ansiedade;
            somaHumor += humor;
            somaEnergia += energia;
            somaSono += sono;

            totalDias++;
        }

        int mediaAnsiedade = somaAnsiedade / totalDias;
        int mediaHumor = somaHumor / totalDias;
        int mediaEnergia = somaEnergia / totalDias;
        int mediaSono = somaSono / totalDias;

        String interpretacao;

        if (mediaAnsiedade >= 3) {
            interpretacao = "Foi observado um nivel elevado de ansiedade durante a semana.";
        } else if (mediaHumor <= 1) {
            interpretacao = "Foi observado um nivel baixo de humor durante a semana.";
        } else {
            interpretacao = "Os dados indicam um estado emocional relativamente estavel.";
        }

        return "Resumo semanal:\n"
                + "- Media de ansiedade: " + mediaAnsiedade + "\n"
                + "- Media de humor: " + mediaHumor + "\n"
                + "- Media de energia: " + mediaEnergia + "\n"
                + "- Media de sono: " + mediaSono + "\n\n"
                + interpretacao + "\n"
                + "Recomendacao: Considere procurar apoio psicologico se necessario.";
    }
}