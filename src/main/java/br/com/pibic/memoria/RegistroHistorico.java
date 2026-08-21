package br.com.pibic.memoria;

import java.time.LocalDateTime;

public class RegistroHistorico {

    private final String autor;
    private final String texto;
    private final LocalDateTime registradoEm;

    public RegistroHistorico(
            String autor,
            String texto,
            LocalDateTime registradoEm) {

        this.autor = autor;
        this.texto = texto;
        this.registradoEm = registradoEm;
    }

    public String getAutor() {
        return autor;
    }

    public String getTexto() {
        return texto;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }
}