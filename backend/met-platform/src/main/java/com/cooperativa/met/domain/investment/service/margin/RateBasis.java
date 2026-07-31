package com.cooperativa.met.domain.investment.service.margin;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Base sobre la que se expresa una tasa de interés. Cambia cómo se convierte
 * a tasa mensual, lo cual cambia el interés generado por periodo.
 */
public enum RateBasis {

    /** Tasa Nominal Anual (TNA): se mensualiza dividiendo entre 12. */
    NOMINAL_ANNUAL {
        @Override
        public BigDecimal toMonthlyRate(BigDecimal rate) {
            return rate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        }
    },

    /** Tasa Efectiva Anual (EA): se mensualiza con raíz 12, (1+i)^(1/12) - 1. */
    EFFECTIVE_ANNUAL {
        @Override
        public BigDecimal toMonthlyRate(BigDecimal rate) {
            double monthly = Math.pow(1.0 + rate.doubleValue(), 1.0 / 12.0) - 1.0;
            return new BigDecimal(monthly, new MathContext(12)).setScale(10, RoundingMode.HALF_UP);
        }
    },

    /** La tasa ya viene expresada mensual, no requiere conversión. */
    MONTHLY {
        @Override
        public BigDecimal toMonthlyRate(BigDecimal rate) {
            return rate;
        }
    };

    /**
     * Convierte la tasa (en la base que representa este enum) a tasa mensual,
     * que es la unidad que usa el cronograma de amortización para calcular
     * el interés generado por cuota.
     */
    public abstract BigDecimal toMonthlyRate(BigDecimal rate);
}
