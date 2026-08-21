package br.com.pibic.memoria;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemoriaRepositoryEmMemoria
        implements MemoriaRepository {

    private final ConcurrentMap<String, MemoriaUsuario> memorias =
            new ConcurrentHashMap<String, MemoriaUsuario>();

    @Override
    public MemoriaUsuario buscarOuCriar(String usuarioId) {
        MemoriaUsuario memoria = memorias.get(usuarioId);

        if (memoria != null) {
            return memoria;
        }

        MemoriaUsuario nova = new MemoriaUsuario(usuarioId);

        MemoriaUsuario existente =
                memorias.putIfAbsent(usuarioId, nova);

        return existente != null ? existente : nova;
    }

    @Override
    public void salvar(MemoriaUsuario memoria) {
        if (memoria == null) {
            return;
        }

        memorias.put(
                memoria.getUsuarioId(),
                memoria
        );
    }

    @Override
    public void remover(String usuarioId) {
        if (usuarioId == null) {
            return;
        }

        memorias.remove(usuarioId);
    }
}