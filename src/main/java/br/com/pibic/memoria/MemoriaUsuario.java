package br.com.pibic.memoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoriaUsuario {

    private static final int LIMITE_HISTORICO_CONVERSA = 20;
    private static final int LIMITE_TEXTO_HISTORICO = 1000;

    private final String usuarioId;

    private String nome = "";
    private String perfilAtual = "";

    private String riscoAtual = "";
    private String protocoloIntervencaoAtual = "";

    private String statusConteudo = "";
    private String statusPsicologo = "";
    private String statusLocalAtendimento = "";
    private String statusMonitoramentoSimulado = "";
    private String statusRelatorioSimulado = "";

    private final List<RegistroHistorico> historico =
            new ArrayList<RegistroHistorico>();

    /*
     * Todas as aplicações da DASS-21 realizadas durante a execução atual.
     *
     * Não sobrescrevemos aplicações anteriores.
     * Quando o PostgreSQL for integrado, o banco será a persistência definitiva
     * e cada aplicação continuará sendo um registro independente.
     */
    private final List<RegistroTriagemDass21> triagensDass21 =
            new ArrayList<RegistroTriagemDass21>();

    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public MemoriaUsuario(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = valorSeguro(nome);
        tocar();
    }

    public String getPerfilAtual() {
        return perfilAtual;
    }

    public void setPerfilAtual(String perfilAtual) {
        this.perfilAtual = valorSeguro(perfilAtual);
        tocar();
    }

    public String getRiscoAtual() {
        return riscoAtual;
    }

    public void setRiscoAtual(String riscoAtual) {
        this.riscoAtual = valorSeguro(riscoAtual);
        tocar();
    }

    public String getProtocoloIntervencaoAtual() {
        return protocoloIntervencaoAtual;
    }

    public void setProtocoloIntervencaoAtual(
            String protocoloIntervencaoAtual) {

        this.protocoloIntervencaoAtual =
                valorSeguro(protocoloIntervencaoAtual);

        tocar();
    }

    public void setStatusConteudo(String statusConteudo) {
        this.statusConteudo = valorSeguro(statusConteudo);
        tocar();
    }

    public void setStatusPsicologo(String statusPsicologo) {
        this.statusPsicologo = valorSeguro(statusPsicologo);
        tocar();
    }

    public void setStatusLocalAtendimento(
            String statusLocalAtendimento) {

        this.statusLocalAtendimento =
                valorSeguro(statusLocalAtendimento);

        tocar();
    }

    public void setStatusMonitoramentoSimulado(
            String statusMonitoramentoSimulado) {

        this.statusMonitoramentoSimulado =
                valorSeguro(statusMonitoramentoSimulado);

        tocar();
    }

    public void setStatusRelatorioSimulado(
            String statusRelatorioSimulado) {

        this.statusRelatorioSimulado =
                valorSeguro(statusRelatorioSimulado);

        tocar();
    }

    public void registrarTriagemDass21(
            String instrumento,
            String versaoInstrumento,
            String versaoAlgoritmo,
            int[] respostas,
            int subtotalDepressao,
            int subtotalAnsiedade,
            int subtotalEstresse,
            int scoreDepressao,
            int scoreAnsiedade,
            int scoreEstresse) {

        triagensDass21.add(
                new RegistroTriagemDass21(
                        instrumento,
                        versaoInstrumento,
                        versaoAlgoritmo,
                        respostas,
                        subtotalDepressao,
                        subtotalAnsiedade,
                        subtotalEstresse,
                        scoreDepressao,
                        scoreAnsiedade,
                        scoreEstresse,
                        LocalDateTime.now()
                )
        );

        tocar();
    }

    public RegistroTriagemDass21 getUltimaTriagemDass21() {
        if (triagensDass21.isEmpty()) {
            return null;
        }

        return triagensDass21.get(
                triagensDass21.size() - 1
        );
    }

    public List<RegistroTriagemDass21> getTriagensDass21() {
        return Collections.unmodifiableList(
                triagensDass21
        );
    }

    public void adicionarHistorico(
            String autor,
            String texto) {

        if (texto == null || texto.trim().isEmpty()) {
            return;
        }

        historico.add(
                new RegistroHistorico(
                        autor,
                        limitarTexto(texto),
                        LocalDateTime.now()
                )
        );

        while (historico.size()
                > LIMITE_HISTORICO_CONVERSA) {

            historico.remove(0);
        }

        tocar();
    }

    public void limparEstadoTemporario() {
        riscoAtual = "";
        protocoloIntervencaoAtual = "";
        historico.clear();
        tocar();
    }

    public String gerarResumo() {
        StringBuilder resumo = new StringBuilder();

        resumo.append("UsuarioId: ")
                .append(usuarioId)
                .append("\n");

        resumo.append("Nome: ")
                .append(valorOuNaoInformado(nome))
                .append("\n");

        /*
         * Mantido apenas por compatibilidade com o fluxo anterior.
         * Não é uma interpretação da DASS-21.
         */
        resumo.append("Perfil legado: ")
                .append(valorOuNaoInformado(perfilAtual))
                .append("\n");

        RegistroTriagemDass21 ultima =
                getUltimaTriagemDass21();

        if (ultima != null) {
            resumo.append("Ultima triagem DASS-21:\n");
            resumo.append("- Depressao: ")
                    .append(ultima.getScoreDepressao())
                    .append("\n");
            resumo.append("- Ansiedade: ")
                    .append(ultima.getScoreAnsiedade())
                    .append("\n");
            resumo.append("- Estresse: ")
                    .append(ultima.getScoreEstresse())
                    .append("\n");
        }

        resumo.append("Risco atual: ")
                .append(valorOuNaoInformado(riscoAtual))
                .append("\n");

        if (!protocoloIntervencaoAtual.isEmpty()) {
            resumo.append("Protocolo de intervencao atual: ")
                    .append(protocoloIntervencaoAtual)
                    .append("\n");
        }

        resumo.append("Historico recente:\n");

        if (historico.isEmpty()) {
            resumo.append("- Nenhuma interacao registrada.\n");
        } else {
            for (RegistroHistorico item : historico) {
                resumo.append("- ")
                        .append(item.getAutor())
                        .append(": ")
                        .append(item.getTexto())
                        .append("\n");
            }
        }

        return resumo.toString();
    }

    private void tocar() {
        atualizadoEm = LocalDateTime.now();
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String valorOuNaoInformado(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Nao informado";
        }

        return valor;
    }

    private String limitarTexto(String texto) {
        String textoLimpo = texto.trim();

        if (textoLimpo.length() <= LIMITE_TEXTO_HISTORICO) {
            return textoLimpo;
        }

        return textoLimpo.substring(
                0,
                LIMITE_TEXTO_HISTORICO
        ) + "...";
    }
}