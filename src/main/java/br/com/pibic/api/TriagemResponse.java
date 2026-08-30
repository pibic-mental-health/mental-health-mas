package br.com.pibic.api;

public class TriagemResponse {

    private boolean sucesso;
    private String usuarioId;
    private String instrumento;
    private String versao;

    private Integer scoreDepressao;
    private Integer scoreAnsiedade;
    private Integer scoreEstresse;

    private String mensagemUsuario;
    private String erro;

    public TriagemResponse() {
    }

    public static TriagemResponse sucesso(
            String usuarioId,
            String instrumento,
            String versao,
            int scoreDepressao,
            int scoreAnsiedade,
            int scoreEstresse) {

        TriagemResponse resposta = new TriagemResponse();

        resposta.sucesso = true;
        resposta.usuarioId = usuarioId;
        resposta.instrumento = instrumento;
        resposta.versao = versao;
        resposta.scoreDepressao = scoreDepressao;
        resposta.scoreAnsiedade = scoreAnsiedade;
        resposta.scoreEstresse = scoreEstresse;

        resposta.mensagemUsuario =
                "Obrigado por responder. Suas respostas foram registradas para acompanhar " +
                "suas aplicacoes da DASS-21. Este questionario nao realiza diagnostico.";

        return resposta;
    }

    public static TriagemResponse erro(String mensagemErro) {
        TriagemResponse resposta = new TriagemResponse();
        resposta.sucesso = false;
        resposta.erro = mensagemErro;
        return resposta;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getInstrumento() {
        return instrumento;
    }

    public String getVersao() {
        return versao;
    }

    public Integer getScoreDepressao() {
        return scoreDepressao;
    }

    public Integer getScoreAnsiedade() {
        return scoreAnsiedade;
    }

    public Integer getScoreEstresse() {
        return scoreEstresse;
    }

    public String getMensagemUsuario() {
        return mensagemUsuario;
    }

    public String getErro() {
        return erro;
    }
}
