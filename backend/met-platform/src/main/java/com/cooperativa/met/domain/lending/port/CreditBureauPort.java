package com.cooperativa.met.domain.lending.port;

import com.cooperativa.met.domain.lending.model.CreditScoreResult;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Puerto (interfaz) de dominio que abstrae la consulta a cualquier
 * central de riesgo crediticio (DataCrédito Experian, etc.).
 *
 * Los detalles de la implementación (HTTP, autenticación OAuth2, mTLS)
 * viven en la capa de infraestructura. El dominio solo habla de conceptos
 * del negocio.
 */
public interface CreditBureauPort {

    /**
     * Consulta el score crediticio de un usuario en la central de riesgo.
     *
     * @param userId       Identificador interno del usuario en MET.
     * @param nationalId   Número de cédula o documento de identidad.
     * @param firstName    Primer nombre del solicitante.
     * @param lastName     Primer apellido del solicitante.
     * @param dateOfBirth  Fecha de nacimiento (requerida para validación de identidad).
     * @return             {@link CreditScoreResult} con el score y la referencia de la consulta.
     */
    CreditScoreResult checkScore(
            UUID userId,
            String nationalId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth
    );
}
