package br.com.pibic.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class JsonLoader {

    public static String load(String filename) {
        InputStream is = JsonLoader.class
                .getClassLoader()
                .getResourceAsStream(filename);

        if (is == null) {
            System.out.println("[ERRO] Arquivo nao encontrado: " + filename);
            return "";
        }

        Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name());
        return scanner.useDelimiter("\\A").next();
    }
}