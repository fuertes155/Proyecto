package com.cooperativa.met.domain.compliance.model;

/**
 * Resultado de una búsqueda difusa: la entrada de la lista que más se parece
 * al nombre consultado, junto con qué tan parecida es (0.0 - 1.0).
 */
public record RestrictiveListMatch(RestrictiveListEntry entry, double similarity) {
}
