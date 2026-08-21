package br.com.pibic.memoria;

public interface MemoriaRepository {

    MemoriaUsuario buscarOuCriar(String usuarioId);

    void salvar(MemoriaUsuario memoria);

    void remover(String usuarioId);
}