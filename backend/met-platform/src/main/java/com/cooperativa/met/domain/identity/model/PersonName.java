package com.cooperativa.met.domain.identity.model;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Nombres y apellidos obtenidos del nombre completo que el usuario escribe en un solo
 * campo al registrarse. {@code users} los guarda por separado, así que hay que partirlo.
 *
 * <p>La partición es una heurística para nombres colombianos, donde lo habitual es tener
 * dos apellidos: con tres o más palabras las dos últimas son los apellidos; con dos, nombre
 * y apellido. Las partículas ("de", "del", "de la"...) se quedan pegadas a la palabra que
 * acompañan, de modo que "María del Carmen" o "De la Cruz" cuentan como una sola.
 *
 * <p>No es exacta ("Juan Carlos Pérez" queda con nombre "Juan" y apellidos "Carlos Pérez"),
 * pero el nombre completo siempre se conserva íntegro entre los dos campos; la identidad
 * real se valida después contra el documento en el registro biométrico.
 */
public record PersonName(String firstName, String lastName) {

    private static final Set<String> PARTICLES = Set.of(
            "de", "del", "la", "las", "los", "da", "das", "do", "dos", "di", "san", "santa", "van", "von");

    public static PersonName fromFullName(String fullName) {
        List<String> words = groupParticles(fullName);
        if (words.size() < 2) {
            throw new BusinessRuleException("INVALID_FULL_NAME",
                    "Escribe tu nombre completo: al menos un nombre y un apellido");
        }
        int surnames = words.size() >= 3 ? 2 : 1;
        int split = words.size() - surnames;
        return new PersonName(
                String.join(" ", words.subList(0, split)),
                String.join(" ", words.subList(split, words.size())));
    }

    private static List<String> groupParticles(String fullName) {
        List<String> words = new ArrayList<>();
        if (fullName == null) {
            return words;
        }
        StringBuilder particles = new StringBuilder();
        for (String token : fullName.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (PARTICLES.contains(token.toLowerCase(Locale.ROOT))) {
                particles.append(token).append(' ');
            } else {
                words.add(particles + token);
                particles.setLength(0);
            }
        }
        if (particles.length() > 0 && !words.isEmpty()) {
            // Partícula suelta al final ("Juan de"): se deja pegada a la última palabra.
            int last = words.size() - 1;
            words.set(last, words.get(last) + " " + particles.toString().trim());
        }
        return words;
    }
}
