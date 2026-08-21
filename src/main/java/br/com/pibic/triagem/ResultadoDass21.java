package br.com.pibic.triagem;

/**
 * Resultado numerico da DASS-21.
 *
 * Os escores sao dimensionais e nao representam diagnostico.
 */
public class ResultadoDass21 {

    private final int subtotalDepressao;
    private final int subtotalAnsiedade;
    private final int subtotalEstresse;

    private final int scoreDepressao;
    private final int scoreAnsiedade;
    private final int scoreEstresse;

    public ResultadoDass21(
            int subtotalDepressao,
            int subtotalAnsiedade,
            int subtotalEstresse,
            int scoreDepressao,
            int scoreAnsiedade,
            int scoreEstresse) {

        this.subtotalDepressao = subtotalDepressao;
        this.subtotalAnsiedade = subtotalAnsiedade;
        this.subtotalEstresse = subtotalEstresse;
        this.scoreDepressao = scoreDepressao;
        this.scoreAnsiedade = scoreAnsiedade;
        this.scoreEstresse = scoreEstresse;
    }

    public int getSubtotalDepressao() {
        return subtotalDepressao;
    }

    public int getSubtotalAnsiedade() {
        return subtotalAnsiedade;
    }

    public int getSubtotalEstresse() {
        return subtotalEstresse;
    }

    public int getScoreDepressao() {
        return scoreDepressao;
    }

    public int getScoreAnsiedade() {
        return scoreAnsiedade;
    }

    public int getScoreEstresse() {
        return scoreEstresse;
    }
}