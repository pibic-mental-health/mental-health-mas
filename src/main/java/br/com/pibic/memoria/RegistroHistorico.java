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
                        ? LocalDateTime.now()
                        : registradoEm;
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

    /*
     * Aliases mantidos por compatibilidade/facilidade de uso.
     */
    public LocalDateTime getCriadoEm() {
        return registradoEm;
    }

    public LocalDateTime getDataHora() {
        return registradoEm;
    }
}
