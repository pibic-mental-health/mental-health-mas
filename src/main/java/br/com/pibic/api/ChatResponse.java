package br.com.pibic.api;

public class ChatResponse {

    private boolean sucesso;

    private String usuarioId;
    private String perfil;
    private String risco;
    private String protocolo;
    private String mensagem;

    private AcoesDisponiveis acoes;

    private String erro;

    private ChatResponse() {
    }

    public static ChatResponse sucesso(
            String usuarioId,
            String perfil,
            String risco,
            String protocolo,
            String mensagem,
            AcoesDisponiveis acoes) {

        ChatResponse response =
                new ChatResponse();

        response.sucesso = true;

        response.usuarioId =
                valorSeguro(usuarioId);

        response.perfil =
                valorSeguro(perfil);

        response.risco =
                valorSeguro(risco);

        response.protocolo =
                valorSeguro(protocolo);

        response.mensagem =
                mensagem == null
                        ? ""
                        : mensagem;

        response.acoes =
                acoes;

        response.erro =
                null;

        return response;
    }

    public static ChatResponse erro(
            String mensagemErro) {

        ChatResponse response =
                new ChatResponse();

        response.sucesso = false;

        response.usuarioId = "";
        response.perfil = "";
        response.risco = "";
        response.protocolo = "";
        response.mensagem = "";

        response.acoes =
                null;

        response.erro =
                mensagemErro == null
                        ? "Erro desconhecido."
                        : mensagemErro.trim();

        return response;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getPerfil() {
        return perfil;
    }

    public String getRisco() {
        return risco;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public AcoesDisponiveis getAcoes() {
        return acoes;
    }

    public String getErro() {
        return erro;
    }

    private static String valorSeguro(
            String valor) {

        return valor == null
                ? ""
                : valor.trim();
    }
}
