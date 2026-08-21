package br.com.pibic.agentes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.Gson;

import br.com.pibic.api.TriagemResponse;
import br.com.pibic.triagem.Dass21Instrumento;
import br.com.pibic.triagem.ResultadoDass21;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteTriagemFormulario extends Agent {

    private static final String VERSAO_ALGORITMO =
            "DASS21_SCORING_V1";

    private final Gson gson = new Gson();

    @Override
    protected void setup() {
        System.out.println(
                "Agente Triagem Formulario iniciado: "
                + getLocalName()
        );

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem == null) {
                    block();
                    return;
                }

                processarTriagem(mensagem);
            }
        });
    }

    private void processarTriagem(
            ACLMessage mensagem) {

        String formulario =
                mensagem.getContent();

        System.out.println(
                "\n[TRIAGEM] Requisicao recebida:"
        );

        System.out.println(formulario);

        String tipo =
                extrairValor(
                        formulario,
                        "tipo"
                );

        if (!"triagem_dass21"
                .equalsIgnoreCase(tipo)) {

            responderErro(
                    mensagem,
                    "O AgenteTriagemFormulario agora utiliza "
                    + "a DASS-21. Tipo esperado: triagem_dass21."
            );

            return;
        }

        String usuarioId =
                extrairValor(
                        formulario,
                        "usuarioId"
                );

        if (usuarioId.isEmpty()) {
            responderErro(
                    mensagem,
                    "usuarioId nao informado."
            );

            return;
        }

        String nome =
                decodificarBase64(
                        extrairValor(
                                formulario,
                                "nomeBase64"
                        )
                );

        String respostasTexto =
                extrairValor(
                        formulario,
                        "respostas"
                );

        try {
            int[] respostas =
                    parseRespostas(
                            respostasTexto
                    );

            ResultadoDass21 resultado =
                    Dass21Instrumento.calcular(
                            respostas
                    );

            if (!nome.isEmpty()) {
                salvarNaMemoria(
                        usuarioId,
                        "nome",
                        nome
                );
            }

            registrarTriagemNaMemoria(
                    usuarioId,
                    respostas,
                    resultado
            );

            System.out.println(
                    "[TRIAGEM] DASS-21 calculada para usuarioId="
                    + usuarioId
            );

            System.out.println(
                    "[TRIAGEM] Score Depressao: "
                    + resultado.getScoreDepressao()
            );

            System.out.println(
                    "[TRIAGEM] Score Ansiedade: "
                    + resultado.getScoreAnsiedade()
            );

            System.out.println(
                    "[TRIAGEM] Score Estresse: "
                    + resultado.getScoreEstresse()
            );

            TriagemResponse respostaApi =
                    TriagemResponse.sucesso(
                            usuarioId,
                            Dass21Instrumento.INSTRUMENTO,
                            Dass21Instrumento.VERSAO,
                            resultado.getScoreDepressao(),
                            resultado.getScoreAnsiedade(),
                            resultado.getScoreEstresse()
                    );

            ACLMessage resposta =
                    mensagem.createReply();

            resposta.setPerformative(
                    ACLMessage.INFORM
            );

            resposta.setContent(
                    gson.toJson(respostaApi)
            );

            send(resposta);

        } catch (IllegalArgumentException e) {
            responderErro(
                    mensagem,
                    e.getMessage()
            );
        }
    }

    private int[] parseRespostas(
            String respostasTexto) {

        if (respostasTexto == null
                || respostasTexto.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "As respostas da DASS-21 sao obrigatorias."
            );
        }

        String[] partes =
                respostasTexto.split(",");

        if (partes.length != 21) {
            throw new IllegalArgumentException(
                    "A DASS-21 exige exatamente 21 respostas."
            );
        }

        int[] respostas =
                new int[21];

        for (int i = 0; i < partes.length; i++) {
            try {
                respostas[i] =
                        Integer.parseInt(
                                partes[i].trim()
                        );
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "A resposta do item "
                        + (i + 1)
                        + " nao e numerica."
                );
            }
        }

        Dass21Instrumento.validarRespostas(
                respostas
        );

        return respostas;
    }

    private void registrarTriagemNaMemoria(
            String usuarioId,
            int[] respostas,
            ResultadoDass21 resultado) {

        ACLMessage msg =
                new ACLMessage(
                        ACLMessage.INFORM
                );

        msg.addReceiver(
                new AID(
                        "agenteMemoria",
                        AID.ISLOCALNAME
                )
        );

        String versaoBase64 =
                Base64.getEncoder()
                .encodeToString(
                        Dass21Instrumento.VERSAO
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        msg.setContent(
                "tipo=triagem_dass21;"
                + "usuarioId="
                + valorSeguroAcl(usuarioId)
                + ";instrumento="
                + Dass21Instrumento.INSTRUMENTO
                + ";versaoBase64="
                + versaoBase64
                + ";versaoAlgoritmo="
                + VERSAO_ALGORITMO
                + ";respostas="
                + serializarRespostas(respostas)
                + ";subtotalDepressao="
                + resultado.getSubtotalDepressao()
                + ";subtotalAnsiedade="
                + resultado.getSubtotalAnsiedade()
                + ";subtotalEstresse="
                + resultado.getSubtotalEstresse()
                + ";depressao="
                + resultado.getScoreDepressao()
                + ";ansiedade="
                + resultado.getScoreAnsiedade()
                + ";estresse="
                + resultado.getScoreEstresse()
        );

        send(msg);
    }

    private String serializarRespostas(
            int[] respostas) {

        StringBuilder sb =
                new StringBuilder();

        for (int i = 0; i < respostas.length; i++) {
            if (i > 0) {
                sb.append(",");
            }

            sb.append(respostas[i]);
        }

        return sb.toString();
    }

    private void salvarNaMemoria(
            String usuarioId,
            String tipo,
            String valor) {

        ACLMessage msg =
                new ACLMessage(
                        ACLMessage.INFORM
                );

        msg.addReceiver(
                new AID(
                        "agenteMemoria",
                        AID.ISLOCALNAME
                )
        );

        msg.setContent(
                "tipo="
                + tipo
                + ";usuarioId="
                + valorSeguroAcl(usuarioId)
                + ";valor="
                + valor
        );

        send(msg);
    }

    private void responderErro(
            ACLMessage mensagem,
            String erro) {

        TriagemResponse respostaApi =
                TriagemResponse.erro(erro);

        ACLMessage resposta =
                mensagem.createReply();

        resposta.setPerformative(
                ACLMessage.FAILURE
        );

        resposta.setContent(
                gson.toJson(respostaApi)
        );

        send(resposta);
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
                    .decode(valor);

            return new String(
                    bytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            return "";
        }
    }

    private String valorSeguroAcl(
            String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace(";", "")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
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
                    parte.split("=", 2);

            if (kv.length == 2
                    && kv[0].trim()
                    .equalsIgnoreCase(chave)) {

                return kv[1].trim();
            }
        }

        return "";
    }
}