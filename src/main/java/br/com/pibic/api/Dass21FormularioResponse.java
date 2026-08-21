package br.com.pibic.api;

import java.util.ArrayList;
import java.util.List;

import br.com.pibic.triagem.Dass21Instrumento;

/**
 * DTO usado pelo endpoint GET /api/triagem/dass21.
 * Retorna a versão brasileira da DASS-21 para a interface.
 */
public class Dass21FormularioResponse {

    private boolean sucesso;
    private String instrumento;
    private String versao;
    private String periodoReferencia;
    private String instrucao;
    private List<Opcao> opcoes;
    private List<Item> itens;

    public Dass21FormularioResponse() {
    }

    public static Dass21FormularioResponse criar() {
        Dass21FormularioResponse resposta =
                new Dass21FormularioResponse();

        resposta.sucesso = true;
        resposta.instrumento = Dass21Instrumento.INSTRUMENTO;
        resposta.versao = Dass21Instrumento.VERSAO;
        resposta.periodoReferencia =
                Dass21Instrumento.PERIODO_REFERENCIA;
        resposta.instrucao =
                Dass21Instrumento.INSTRUCAO;

        resposta.opcoes = new ArrayList<Opcao>();

        List<String> textosOpcoes =
                Dass21Instrumento.obterOpcoes();

        for (int i = 0; i < textosOpcoes.size(); i++) {
            resposta.opcoes.add(
                    new Opcao(i, textosOpcoes.get(i))
            );
        }

        resposta.itens = new ArrayList<Item>();

        List<String> textosItens =
                Dass21Instrumento.obterItens();

        for (int i = 0; i < textosItens.size(); i++) {
            resposta.itens.add(
                    new Item(i + 1, textosItens.get(i))
            );
        }

        return resposta;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public String getInstrumento() {
        return instrumento;
    }

    public String getVersao() {
        return versao;
    }

    public String getPeriodoReferencia() {
        return periodoReferencia;
    }

    public String getInstrucao() {
        return instrucao;
    }

    public List<Opcao> getOpcoes() {
        return opcoes;
    }

    public List<Item> getItens() {
        return itens;
    }

    public static class Opcao {

        private int valor;
        private String texto;

        public Opcao(int valor, String texto) {
            this.valor = valor;
            this.texto = texto;
        }

        public int getValor() {
            return valor;
        }

        public String getTexto() {
            return texto;
        }
    }

    public static class Item {

        private int numero;
        private String texto;

        public Item(int numero, String texto) {
            this.numero = numero;
            this.texto = texto;
        }

        public int getNumero() {
            return numero;
        }

        public String getTexto() {
            return texto;
        }
    }
}