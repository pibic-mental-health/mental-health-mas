package br.com.pibic.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.pibic.agentes.LocalAtendimentoResultado;

public class LocalAtendimentoResponse {

    private boolean sucesso;
    private String cidade;
    private String uf;
    private int quantidade;
    private List<LocalAtendimentoResultado> locais;
    private String erro;

    private LocalAtendimentoResponse() {

        locais =
                new ArrayList<LocalAtendimentoResultado>();
    }

    public static LocalAtendimentoResponse sucesso(
            String cidade,
            String uf,
            List<LocalAtendimentoResultado> locais) {

        LocalAtendimentoResponse response =
                new LocalAtendimentoResponse();

        response.sucesso = true;
        response.cidade =
                cidade == null
                        ? ""
                        : cidade.trim();

        response.uf =
                uf == null
                        ? ""
                        : uf.trim();

        if (locais != null) {
            response.locais.addAll(locais);
        }

        response.quantidade =
                response.locais.size();

        return response;
    }

    public static LocalAtendimentoResponse erro(
            String mensagem) {

        LocalAtendimentoResponse response =
                new LocalAtendimentoResponse();

        response.sucesso = false;
        response.erro =
                mensagem == null
                        ? "Erro desconhecido."
                        : mensagem.trim();

        return response;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public List<LocalAtendimentoResultado> getLocais() {

        return Collections.unmodifiableList(
                locais
        );
    }

    public String getErro() {
        return erro;
    }
}
