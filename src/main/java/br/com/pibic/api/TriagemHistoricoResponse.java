package br.com.pibic.api;

import java.util.ArrayList;
import java.util.List;

public class TriagemHistoricoResponse {

    private boolean sucesso;
    private String usuarioId;
    private int quantidadeAplicacoes;
    private List<AplicacaoDass21> aplicacoes;
    private String erro;

    public TriagemHistoricoResponse() {
    }

    public static TriagemHistoricoResponse sucesso(
            String usuarioId) {

        TriagemHistoricoResponse resposta =
                new TriagemHistoricoResponse();

        resposta.sucesso = true;
        resposta.usuarioId = usuarioId;
        resposta.aplicacoes =
                new ArrayList<AplicacaoDass21>();
        resposta.quantidadeAplicacoes = 0;

        return resposta;
    }

    public static TriagemHistoricoResponse erro(
            String mensagemErro) {

        TriagemHistoricoResponse resposta =
                new TriagemHistoricoResponse();

        resposta.sucesso = false;
        resposta.erro = mensagemErro;

        return resposta;
    }

    public void adicionarAplicacao(
            AplicacaoDass21 aplicacao) {

        if (aplicacoes == null) {
            aplicacoes =
                    new ArrayList<AplicacaoDass21>();
        }

        aplicacoes.add(aplicacao);
        quantidadeAplicacoes = aplicacoes.size();
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public int getQuantidadeAplicacoes() {
        return quantidadeAplicacoes;
    }

    public List<AplicacaoDass21> getAplicacoes() {
        return aplicacoes;
    }

    public String getErro() {
        return erro;
    }

    public static class AplicacaoDass21 {

        private int numeroAplicacao;

        private String instrumento;
        private String versaoInstrumento;
        private String versaoAlgoritmo;

        private String realizadoEm;

        private int[] respostas;

        private int subtotalDepressao;
        private int subtotalAnsiedade;
        private int subtotalEstresse;

        private int scoreDepressao;
        private int scoreAnsiedade;
        private int scoreEstresse;

        public AplicacaoDass21(
                int numeroAplicacao,
                String instrumento,
                String versaoInstrumento,
                String versaoAlgoritmo,
                String realizadoEm,
                int[] respostas,
                int subtotalDepressao,
                int subtotalAnsiedade,
                int subtotalEstresse,
                int scoreDepressao,
                int scoreAnsiedade,
                int scoreEstresse) {

            this.numeroAplicacao =
                    numeroAplicacao;

            this.instrumento =
                    instrumento;

            this.versaoInstrumento =
                    versaoInstrumento;

            this.versaoAlgoritmo =
                    versaoAlgoritmo;

            this.realizadoEm =
                    realizadoEm;

            this.respostas =
                    respostas;

            this.subtotalDepressao =
                    subtotalDepressao;

            this.subtotalAnsiedade =
                    subtotalAnsiedade;

            this.subtotalEstresse =
                    subtotalEstresse;

            this.scoreDepressao =
                    scoreDepressao;

            this.scoreAnsiedade =
                    scoreAnsiedade;

            this.scoreEstresse =
                    scoreEstresse;
        }
    }
}