package com.cooperativa.met.domain.compliance.service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normaliza nombres antes de compararlos contra listas restrictivas: mayúsculas,
 * sin tildes/diacríticos, sin puntuación, espacios colapsados. Así "María José
 * Pérez" y "MARIA JOSE PEREZ" comparan igual, y el trigram matching (que ya
 * tolera errores menores) no tiene que absorber también estas diferencias triviales.
 */
public final class NameNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9 ]");
    private static final Pattern MULTI_SPACE = Pattern.compile(" +");

    private NameNormalizer() {
    }

    public static String normalize(String rawName) {
        if (rawName == null) return "";
        String upper = rawName.toUpperCase().trim();
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(upper, Normalizer.Form.NFD)).replaceAll("");
        String alphanumericOnly = NON_ALPHANUMERIC.matcher(withoutDiacritics).replaceAll(" ");
        return MULTI_SPACE.matcher(alphanumericOnly).replaceAll(" ").trim();
    }
}
