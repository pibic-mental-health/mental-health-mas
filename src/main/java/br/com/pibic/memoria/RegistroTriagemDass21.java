package br.com.pibic.memoria;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Uma aplicação individual da DASS-21.
 *
 * Mantemos as 21 respostas originais e os valores intermediários para que
 * o cálculo seja integralmente reproduzível no futuro.
 *
 * Nenhum campo desta classe representa diagnóstico clínico.
 */
public class RegistroTriagemDass21 {

    private final String instrumento;
    private final String versaoInstrumento;
    private final String versaoAlgoritmo;

    private final int[] respostas;

    private final int subtotalDepressao;
    private final int subtotalAnsiedade;
    private final int subtotalEstresse;

    private final int scoreDepressao;
    private final int scoreAnsiedade;
    private final int scoreEstresse;

    private final LocalDateTime realizadoEm;

    public RegistroTriagemDass21(
            String instrumento,
            String versaoInstrumento,
            String versaoAlgoritmo,
            int[] respostas,
            int subtotalDepressao,
            int subtotalAnsiedade,
            int subtotalEstresse,
            int scoreDepressao,
            int scoreAnsiedade,
            int scoreEstresse,
            LocalDateTime realizadoEm) {

        this.instrumento = instrumento;
        this.versaoInstrumento = versaoInstrumento;
        this.versaoAlgoritmo = versaoAlgoritmo;

        this.respostas = respostas == null
                ? new int[0]
                : Arrays.copyOf(respostas, respostas.length);

        this.subtotalDepressao = subtotalDepressao;
        this.subtotalAnsiedade = subtotalAnsiedade;
        this.subtotalEstresse = subtotalEstresse;

        this.scoreDepressao = scoreDepressao;
        this.scoreAnsiedade = scoreAnsiedade;
        this.scoreEstresse = scoreEstresse;

        this.realizadoEm = realizadoEm;
    }

    public String getInstrumento() {
        return instrumento;
    }

    public String getVersaoInstrumento() {
        return versaoInstrumento;
    }

    public String getVersaoAlgoritmo() {
        return versaoAlgoritmo;
    }

    public int[] getRespostas() {
        return Arrays.copyOf(respostas, respostas.length);
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

    public LocalDateTime getRealizadoEm() {
        return realizadoEm;
    }
}