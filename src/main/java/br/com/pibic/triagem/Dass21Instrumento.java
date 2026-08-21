package br.com.pibic.triagem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Definição computacional da DASS-21 - versão brasileira Vignola & Tucci.
 *
 * Fonte mestre dos textos:
 * "DASS - 21 Versão traduzida e validada para o português do Brasil"
 * Autores da tradução: Vignola, R.C.B. & Tucci, A.M.
 *
 * Regra de pontuação:
 * - cada item recebe 0, 1, 2 ou 3;
 * - somam-se os 7 itens de cada dimensão;
 * - na DASS-21 adulta, o subtotal de cada dimensão é multiplicado por 2.
 *
 * Este componente calcula escores dimensionais.
 * Ele NÃO realiza diagnóstico e NÃO aplica rótulos de gravidade.
 */
public final class Dass21Instrumento {

    public static final String INSTRUMENTO = "DASS-21";

    public static final String VERSAO =
            "Português do Brasil - Vignola & Tucci";

    public static final String PERIODO_REFERENCIA =
            "última semana";

    public static final String INSTRUCAO =
            "Por favor, leia cuidadosamente cada uma das afirmações abaixo "
            + "e circule o número apropriado 0,1,2 ou 3 que indique o quanto "
            + "ela se aplicou a você durante a última semana, conforme a "
            + "indicação a seguir:";

    private static final String[] OPCOES = new String[] {
        "Não se aplicou de maneira alguma",
        "Aplicou-se em algum grau, ou por pouco de tempo",
        "Aplicou-se em um grau considerável, ou por uma boa parte do tempo",
        "Aplicou-se muito, ou na maioria do tempo"
    };

    private static final String[] ITENS = new String[] {
        "Achei difícil me acalmar",
        "Senti minha boca seca",
        "Não consegui vivenciar nenhum sentimento positivo",
        "Tive dificuldade em respirar em alguns momentos (ex. respiração ofegante, falta de ar, sem ter feito nenhum esforço físico)",
        "Achei difícil ter iniciativa para fazer as coisas",
        "Tive a tendência de reagir de forma exagerada às situações",
        "Senti tremores (ex. nas mãos)",
        "Senti que estava sempre nervoso",
        "Preocupei-me com situações em que eu pudesse entrar em pânico e parecesse ridículo (a)",
        "Senti que não tinha nada a desejar",
        "Senti-me agitado",
        "Achei difícil relaxar",
        "Senti-me depressivo (a) e sem ânimo",
        "Fui intolerante com as coisas que me impediam de continuar o que eu estava fazendo",
        "Senti que ia entrar em pânico",
        "Não consegui me entusiasmar com nada",
        "Senti que não tinha valor como pessoa",
        "Senti que estava um pouco emotivo/sensível demais",
        "Sabia que meu coração estava alterado mesmo não tendo feito nenhum esforço físico (ex. aumento da frequência cardíaca, disritmia cardíaca)",
        "Senti medo sem motivo",
        "Senti que a vida não tinha sentido"
    };

    private static final int[] ITENS_DEPRESSAO =
            new int[] {3, 5, 10, 13, 16, 17, 21};

    private static final int[] ITENS_ANSIEDADE =
            new int[] {2, 4, 7, 9, 15, 19, 20};

    private static final int[] ITENS_ESTRESSE =
            new int[] {1, 6, 8, 11, 12, 14, 18};

    private Dass21Instrumento() {
    }

    public static ResultadoDass21 calcular(int[] respostas) {
        validarRespostas(respostas);

        int subtotalDepressao =
                somar(respostas, ITENS_DEPRESSAO);

        int subtotalAnsiedade =
                somar(respostas, ITENS_ANSIEDADE);

        int subtotalEstresse =
                somar(respostas, ITENS_ESTRESSE);

        return new ResultadoDass21(
                subtotalDepressao,
                subtotalAnsiedade,
                subtotalEstresse,
                subtotalDepressao * 2,
                subtotalAnsiedade * 2,
                subtotalEstresse * 2
        );
    }

    public static void validarRespostas(int[] respostas) {
        if (respostas == null) {
            throw new IllegalArgumentException(
                    "O campo respostas é obrigatório."
            );
        }

        if (respostas.length != 21) {
            throw new IllegalArgumentException(
                    "A DASS-21 exige exatamente 21 respostas."
            );
        }

        for (int i = 0; i < respostas.length; i++) {
            int valor = respostas[i];

            if (valor < 0 || valor > 3) {
                throw new IllegalArgumentException(
                        "A resposta do item "
                        + (i + 1)
                        + " deve estar entre 0 e 3."
                );
            }
        }
    }

    private static int somar(
            int[] respostas,
            int[] itens) {

        int total = 0;

        for (int item : itens) {
            total += respostas[item - 1];
        }

        return total;
    }

    public static List<String> obterItens() {
        List<String> itens =
                new ArrayList<String>();

        for (String item : ITENS) {
            itens.add(item);
        }

        return Collections.unmodifiableList(itens);
    }

    public static List<String> obterOpcoes() {
        List<String> opcoes =
                new ArrayList<String>();

        for (String opcao : OPCOES) {
            opcoes.add(opcao);
        }

        return Collections.unmodifiableList(opcoes);
    }
}