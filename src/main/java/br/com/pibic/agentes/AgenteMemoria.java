package br.com.pibic.agentes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import com.google.gson.Gson;

import br.com.pibic.api.TriagemHistoricoResponse;
import br.com.pibic.memoria.MemoriaRepository;
import br.com.pibic.memoria.MemoriaRepositoryEmMemoria;
import br.com.pibic.memoria.MemoriaUsuario;
import br.com.pibic.memoria.RegistroTriagemDass21;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteMemoria extends Agent {

    private static final String USUARIO_SIMULACAO =
            "SIMULACAO_LOCAL";

    private final Gson gson = new Gson();

    private MemoriaRepository repository;

    @Override
    protected void setup() {
        repository = new MemoriaRepositoryEmMemoria();

        System.out.println(
                "Agente Memoria iniciado: "
                + getLocalName()
        );

        System.out.println(
                "[MEMORIA] Repositorio atual: MEMORIA_LOCAL"
        );

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem == null) {
                    block();
                    return;
                }

                processarMensagem(mensagem);
            }
        });
    }

    private void processarMensagem(
            ACLMessage mensagem) {

        String conteudo =
                mensagem.getContent();

        System.out.println(
                "\n[MEMORIA] Conteudo recebido:"
        );

        System.out.println(conteudo);

        String tipo =
                extrairValor(
                        conteudo,
                        "tipo"
                );

        String valor =
                extrairValor(
                        conteudo,
                        "valor"
                );

        String usuarioId =
                normalizarUsuarioId(
                        extrairValor(
                                conteudo,
                                "usuarioId"
                        )
                );

        MemoriaUsuario memoria =
                repository.buscarOuCriar(
                        usuarioId
                );

        if (tipo.equalsIgnoreCase("nome")) {
            memoria.setNome(valor);
            repository.salvar(memoria);

            System.out.println(
                    "[MEMORIA] Nome salvo para usuarioId="
                    + usuarioId
            );
        }

        else if (tipo.equalsIgnoreCase("perfil")) {
            memoria.setPerfilAtual(valor);
            repository.salvar(memoria);

            System.out.println(
                    "[MEMORIA] Perfil legado salvo para usuarioId="
                    + usuarioId
                    + ": "
                    + valor
            );
        }

        else if (tipo.equalsIgnoreCase(
                "triagem_dass21")) {

            registrarTriagemDass21(
                    conteudo,
                    usuarioId,
                    memoria
            );
        }

        else if (tipo.equalsIgnoreCase(
                "consulta_triagens_dass21")) {

            responderHistoricoDass21(
                    mensagem,
                    memoria
            );
        }

        else if (tipo.equalsIgnoreCase("risco")) {
            memoria.setRiscoAtual(valor);
            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase(
                "protocolo_intervencao")) {

            memoria.setProtocoloIntervencaoAtual(
                    valor
            );

            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase("mensagem")) {
            memoria.adicionarHistorico(
                    "USUARIO",
                    valor
            );

            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase("resposta")) {
            memoria.adicionarHistorico(
                    "SISTEMA",
                    valor
            );

            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase("conteudo")) {
            memoria.setStatusConteudo(valor);
            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase("psicologo")) {
            memoria.setStatusPsicologo(valor);
            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase(
                "local_atendimento")) {

            memoria.setStatusLocalAtendimento(
                    valor
            );

            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase("monitoramento")
                || tipo.equalsIgnoreCase(
                        "monitoramento_simulado")) {

            memoria.setStatusMonitoramentoSimulado(
                    valor
            );

            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase(
                "relatorio_simulado")) {

            memoria.setStatusRelatorioSimulado(
                    valor
            );

            repository.salvar(memoria);
        }

        else if (tipo.equalsIgnoreCase("consulta")) {
            responderConsulta(
                    mensagem,
                    memoria
            );
        }

        else if (tipo.equalsIgnoreCase(
                "limpar_sessao")) {

            memoria.limparEstadoTemporario();
            repository.salvar(memoria);

            ACLMessage resposta =
                    mensagem.createReply();

            resposta.setPerformative(
                    ACLMessage.INFORM
            );

            resposta.setContent(
                    "usuarioId="
                    + usuarioId
                    + ";status=SESSAO_LIMPA"
            );

            send(resposta);
        }
    }

    private void registrarTriagemDass21(
            String conteudo,
            String usuarioId,
            MemoriaUsuario memoria) {

        String instrumento =
                extrairValor(
                        conteudo,
                        "instrumento"
                );

        String versaoInstrumento =
                decodificarBase64(
                        extrairValor(
                                conteudo,
                                "versaoBase64"
                        )
                );

        String versaoAlgoritmo =
                extrairValor(
                        conteudo,
                        "versaoAlgoritmo"
                );

        int[] respostas =
                parseRespostas(
                        extrairValor(
                                conteudo,
                                "respostas"
                        )
                );

        int subtotalDepressao =
                extrairInteiro(
                        conteudo,
                        "subtotalDepressao",
                        -1
                );

        int subtotalAnsiedade =
                extrairInteiro(
                        conteudo,
                        "subtotalAnsiedade",
                        -1
                );

        int subtotalEstresse =
                extrairInteiro(
                        conteudo,
                        "subtotalEstresse",
                        -1
                );

        int depressao =
                extrairInteiro(
                        conteudo,
                        "depressao",
                        -1
                );

        int ansiedade =
                extrairInteiro(
                        conteudo,
                        "ansiedade",
                        -1
                );

        int estresse =
                extrairInteiro(
                        conteudo,
                        "estresse",
                        -1
                );

        boolean valido =
                respostas.length == 21
                && subtotalDassValido(
                        subtotalDepressao
                )
                && subtotalDassValido(
                        subtotalAnsiedade
                )
                && subtotalDassValido(
                        subtotalEstresse
                )
                && scoreDassValido(depressao)
                && scoreDassValido(ansiedade)
                && scoreDassValido(estresse)
                && depressao
                    == subtotalDepressao * 2
                && ansiedade
                    == subtotalAnsiedade * 2
                && estresse
                    == subtotalEstresse * 2;

        if (!valido) {
            System.out.println(
                    "[MEMORIA] Triagem DASS-21 rejeitada "
                    + "por dados inconsistentes."
            );

            return;
        }

        memoria.registrarTriagemDass21(
                instrumento,
                versaoInstrumento,
                versaoAlgoritmo,
                respostas,
                subtotalDepressao,
                subtotalAnsiedade,
                subtotalEstresse,
                depressao,
                ansiedade,
                estresse
        );

        repository.salvar(memoria);

        System.out.println(
                "[MEMORIA] Triagem DASS-21 registrada para usuarioId="
                + usuarioId
                + " aplicacao="
                + memoria.getTriagensDass21().size()
                + " D="
                + depressao
                + " A="
                + ansiedade
                + " S="
                + estresse
        );
    }

    private void responderHistoricoDass21(
            ACLMessage mensagem,
            MemoriaUsuario memoria) {

        TriagemHistoricoResponse respostaApi =
                TriagemHistoricoResponse.sucesso(
                        memoria.getUsuarioId()
                );

        List<RegistroTriagemDass21> triagens =
                memoria.getTriagensDass21();

        for (int i = 0; i < triagens.size(); i++) {
            RegistroTriagemDass21 registro =
                    triagens.get(i);

            respostaApi.adicionarAplicacao(
                    new TriagemHistoricoResponse.AplicacaoDass21(
                            i + 1,
                            registro.getInstrumento(),
                            registro.getVersaoInstrumento(),
                            registro.getVersaoAlgoritmo(),
                            registro.getRealizadoEm()
                                    .toString(),
                            registro.getRespostas(),
                            registro.getSubtotalDepressao(),
                            registro.getSubtotalAnsiedade(),
                            registro.getSubtotalEstresse(),
                            registro.getScoreDepressao(),
                            registro.getScoreAnsiedade(),
                            registro.getScoreEstresse()
                    )
            );
        }

        ACLMessage resposta =
                mensagem.createReply();

        resposta.setPerformative(
                ACLMessage.INFORM
        );

        resposta.setContent(
                gson.toJson(respostaApi)
        );

        send(resposta);

        System.out.println(
                "[MEMORIA] Historico DASS-21 enviado para usuarioId="
                + memoria.getUsuarioId()
                + " quantidade="
                + triagens.size()
        );
    }

    private void responderConsulta(
            ACLMessage mensagem,
            MemoriaUsuario memoria) {

        ACLMessage resposta =
                mensagem.createReply();

        resposta.setPerformative(
                ACLMessage.INFORM
        );

        resposta.setContent(
                memoria.gerarResumo()
        );

        send(resposta);
    }

    private boolean scoreDassValido(
            int score) {

        return score >= 0
                && score <= 42
                && score % 2 == 0;
    }

    private boolean subtotalDassValido(
            int subtotal) {

        return subtotal >= 0
                && subtotal <= 21;
    }

    private int[] parseRespostas(
            String texto) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return new int[0];
        }

        String[] partes =
                texto.split(",");

        if (partes.length != 21) {
            return new int[0];
        }

        int[] respostas =
                new int[21];

        for (int i = 0; i < partes.length; i++) {
            try {
                respostas[i] =
                        Integer.parseInt(
                                partes[i].trim()
                        );

                if (respostas[i] < 0
                        || respostas[i] > 3) {

                    return new int[0];
                }

            } catch (Exception e) {
                return new int[0];
            }
        }

        return respostas;
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

    private int extrairInteiro(
            String texto,
            String chave,
            int padrao) {

        String valor =
                extrairValor(
                        texto,
                        chave
                );

        try {
            return Integer.parseInt(valor);
        } catch (Exception e) {
            return padrao;
        }
    }

    private String normalizarUsuarioId(
            String usuarioId) {

        if (usuarioId == null
                || usuarioId.trim().isEmpty()) {

            return USUARIO_SIMULACAO;
        }

        return usuarioId.trim();
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