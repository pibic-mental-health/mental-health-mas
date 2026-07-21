package br.com.pibic.agentes;

import java.util.ArrayList;
import java.util.List;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteMemoria extends Agent {

    private static final int LIMITE_HISTORICO = 10;
    private static final int LIMITE_TEXTO_HISTORICO = 500;

    private String nome = "";
    private String perfil = "";
    private String risco = "";
    private String protocoloIntervencao = "";

    private String statusConteudo = "";
    private String statusPsicologo = "";
    private String statusLocalAtendimento = "";
    private String statusMonitoramentoSimulado = "";
    private String statusRelatorioSimulado = "";

    private List<String> historico = new ArrayList<String>();

    @Override
    protected void setup() {
        System.out.println("Agente Memoria iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    System.out.println("\n[MEMORIA] Conteudo recebido:");
                    System.out.println(conteudo);

                    String tipo = extrairValor(conteudo, "tipo");
                    String valor = extrairValor(conteudo, "valor");

                    if (tipo.equalsIgnoreCase("nome")) {
                        nome = valor;
                        System.out.println("[MEMORIA] Nome salvo: " + nome);
                    }

                    else if (tipo.equalsIgnoreCase("perfil")) {
                        perfil = valor;
                        System.out.println("[MEMORIA] Perfil salvo: " + perfil);
                    }

                    else if (tipo.equalsIgnoreCase("risco")) {
                        risco = valor;
                        System.out.println("[MEMORIA] Risco salvo: " + risco);
                    }

                    else if (tipo.equalsIgnoreCase("protocolo_intervencao")) {
                        protocoloIntervencao = valor;
                        System.out.println("[MEMORIA] Protocolo de intervencao salvo: " + protocoloIntervencao);
                    }

                    else if (tipo.equalsIgnoreCase("mensagem")) {
                        adicionarAoHistorico("Usuario: " + valor);
                        System.out.println("[MEMORIA] Mensagem armazenada");
                    }

                    else if (tipo.equalsIgnoreCase("resposta")) {
                        adicionarAoHistorico("IA: " + valor);
                        System.out.println("[MEMORIA] Resposta armazenada");
                    }

                    else if (tipo.equalsIgnoreCase("conteudo")) {
                        statusConteudo = valor;
                        System.out.println("[MEMORIA] Preferencia de conteudo salva: " + statusConteudo);
                    }

                    else if (tipo.equalsIgnoreCase("psicologo")) {
                        statusPsicologo = valor;
                        System.out.println("[MEMORIA] Preferencia de psicologo salva: " + statusPsicologo);
                    }

                    else if (tipo.equalsIgnoreCase("local_atendimento")) {
                        statusLocalAtendimento = valor;
                        System.out.println("[MEMORIA] Preferencia de locais de atendimento salva: " + statusLocalAtendimento);
                    }

                    else if (tipo.equalsIgnoreCase("monitoramento")) {
                        statusMonitoramentoSimulado = valor;
                        System.out.println("[MEMORIA] Monitoramento salvo: " + statusMonitoramentoSimulado);
                    }

                    else if (tipo.equalsIgnoreCase("monitoramento_simulado")) {
                        statusMonitoramentoSimulado = valor;
                        System.out.println("[MEMORIA] Simulacao de monitoramento salva: " + statusMonitoramentoSimulado);
                    }

                    else if (tipo.equalsIgnoreCase("relatorio_simulado")) {
                        statusRelatorioSimulado = valor;
                        System.out.println("[MEMORIA] Relatorio simulado salvo: " + statusRelatorioSimulado);
                    }

                    else if (tipo.equalsIgnoreCase("consulta")) {
                        String resumo = gerarResumo();

                        ACLMessage resposta = mensagem.createReply();
                        resposta.setPerformative(ACLMessage.INFORM);
                        resposta.setContent(resumo);
                        send(resposta);

                        System.out.println("[MEMORIA] Resumo enviado ao solicitante:");
                        System.out.println(resumo);
                    }

                    else {
                        System.out.println("[MEMORIA] Tipo nao reconhecido: " + tipo);
                    }

                } else {
                    block();
                }
            }
        });
    }

    private String gerarResumo() {
        StringBuilder resumo = new StringBuilder();

        resumo.append("Nome: ").append(valorOuNaoInformado(nome)).append("\n");
        resumo.append("Perfil: ").append(valorOuNaoInformado(perfil)).append("\n");
        resumo.append("Risco: ").append(valorOuNaoInformado(risco)).append("\n");

        if (!protocoloIntervencao.isEmpty()) {
            resumo.append("Protocolo de intervencao: ").append(protocoloIntervencao).append("\n");
        }

        if (!statusConteudo.isEmpty()) {
            resumo.append("Conteudo: ").append(statusConteudo).append("\n");
        }

        if (!statusPsicologo.isEmpty()) {
            resumo.append("Psicologo: ").append(statusPsicologo).append("\n");
        }

        if (!statusLocalAtendimento.isEmpty()) {
            resumo.append("Locais de atendimento: ").append(statusLocalAtendimento).append("\n");
        }

        if (!statusMonitoramentoSimulado.isEmpty()) {
            resumo.append("Monitoramento simulado: ").append(statusMonitoramentoSimulado).append("\n");
        }

        if (!statusRelatorioSimulado.isEmpty()) {
            resumo.append("Relatorio simulado: ").append(statusRelatorioSimulado).append("\n");
        }

        resumo.append("Historico:\n");

        if (historico.isEmpty()) {
            resumo.append("- Nenhuma interacao registrada.\n");
        } else {
            for (String item : historico) {
                resumo.append("- ").append(item).append("\n");
            }
        }

        return resumo.toString();
    }

    private void adicionarAoHistorico(String item) {
        if (item == null || item.trim().isEmpty()) {
            return;
        }

        historico.add(limitarTexto(item));

        while (historico.size() > LIMITE_HISTORICO) {
            historico.remove(0);
        }
    }

    private String limitarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        String textoLimpo = texto.trim();

        if (textoLimpo.length() <= LIMITE_TEXTO_HISTORICO) {
            return textoLimpo;
        }

        return textoLimpo.substring(0, LIMITE_TEXTO_HISTORICO) + "...";
    }

    private String valorOuNaoInformado(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Nao informado";
        }

        return valor;
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