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

    public ChatResponse() {
    }

    public static ChatResponse sucesso(
            String usuarioId,
            String perfil,
            String risco,
            String protocolo,
            String mensagem,
            AcoesDisponiveis acoes) {

        ChatResponse resposta = new ChatResponse();
        resposta.sucesso = true;
        resposta.usuarioId = usuarioId;
        resposta.perfil = perfil;
        resposta.risco = risco;
        resposta.protocolo = protocolo;
        resposta.mensagem = mensagem;
        resposta.acoes = acoes;
        return resposta;
    }

    public static ChatResponse erro(String mensagemErro) {
        ChatResponse resposta = new ChatResponse();
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
}