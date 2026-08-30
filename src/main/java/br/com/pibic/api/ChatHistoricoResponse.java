package br.com.pibic.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatHistoricoResponse {

    private boolean sucesso;
    private String usuarioId;
    private List<Mensagem> mensagens;
    private String erro;

    private ChatHistoricoResponse() {
        this.mensagens =
                new ArrayList<Mensagem>();
    }

    public static ChatHistoricoResponse sucesso(
            String usuarioId) {

        ChatHistoricoResponse response =
                new ChatHistoricoResponse();

        response.sucesso = true;
        response.usuarioId =
                usuarioId == null
                        ? ""
                        : usuarioId.trim();

        response.erro = null;

        return response;
    }

    public static ChatHistoricoResponse erro(
            String mensagemErro) {

        ChatHistoricoResponse response =
                new ChatHistoricoResponse();

        response.sucesso = false;
        response.usuarioId = "";
        response.erro =
                mensagemErro == null
                        ? "Erro desconhecido."
                        : mensagemErro.trim();

        return response;
    }

    public void adicionarMensagem(
            Mensagem mensagem) {

        if (mensagem == null) {
            return;
        }

        mensagens.add(mensagem);
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public List<Mensagem> getMensagens() {
        return Collections.unmodifiableList(
                mensagens
        );
    }

    public String getErro() {
        return erro;
    }

    public static class Mensagem {

        private int ordem;
        private String autor;
        private String texto;
        private String registradoEm;

        public Mensagem(
                int ordem,
                String autor,
                String texto,
                String registradoEm) {

            this.ordem = ordem;
            this.autor =
                    autor == null
                            ? ""
                            : autor.trim();

            this.texto =
                    texto == null
                            ? ""
                            : texto;

            this.registradoEm =
                    registradoEm == null
                            ? ""
                            : registradoEm.trim();
        }

        public int getOrdem() {
            return ordem;
        }

        public String getAutor() {
            return autor;
        }

        public String getTexto() {
            return texto;
        }

        public String getRegistradoEm() {
            return registradoEm;
        }
    }
}
