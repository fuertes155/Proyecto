package com.cooperativa.met.infrastructure.config;

import com.cooperativa.met.domain.investment.service.margin.MarginModel;
import com.cooperativa.met.domain.investment.service.margin.RateBasis;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Parámetros del Motor de Distribución de Capital. Ver application.yml bajo
 * el prefijo met.capital-engine para la configuración por ambiente.
 *
 * Ninguno de estos valores debe hardcodearse en el código de negocio: el
 * modelo de margen, la base de tasa y los topes de riesgo son decisiones de
 * negocio que deben poder cambiar sin recompilar el motor.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "met.capital-engine")
public class CapitalEngineProperties {

    /** Modelo de cálculo de margen (A/B/C). Ver {@link MarginModel}. */
    private MarginModel marginModel = MarginModel.INTEREST_COMMISSION;

    /** Tasa de margen de la plataforma (ej: 0.02 = 2%). */
    private BigDecimal marginRate = new BigDecimal("0.02");

    /** Base en la que se expresan las tasas de interés de los préstamos. */
    private RateBasis rateBasis = RateBasis.NOMINAL_ANNUAL;

    /**
     * Porcentaje máximo del monto de un depósito que se puede concentrar en un
     * solo deudor (diversificación de riesgo). Ej: 0.20 = máximo 20% por deudor.
     */
    private BigDecimal concentrationCapPercentage = new BigDecimal("0.20");

    /**
     * Porcentaje del saldo pendiente (capital + interés) de una cuota en mora
     * sostenida que el Fondo de Garantías puede cubrir. Nunca 1.00 (100%):
     * el fondo no garantiza el retorno total del capital.
     */
    private BigDecimal guaranteeFundCoverageRatio = new BigDecimal("0.70");

    /** Días de mora a partir de los cuales se evalúa la cobertura del Fondo de Garantías. */
    private int guaranteeFundActivationDaysLate = 90;
}
