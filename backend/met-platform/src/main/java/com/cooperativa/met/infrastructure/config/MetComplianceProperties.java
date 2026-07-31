package com.cooperativa.met.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "met.compliance")
public class MetComplianceProperties {

    private String entityCode = "COOP001";
    private String storagePath = "./data/regulatory-reports";

    private final Sarlaft sarlaft = new Sarlaft();

    @Getter
    @Setter
    public static class Sarlaft {
        /** URL pública del SDN.csv de OFAC (sin autenticación, sin contrato — dato público del Tesoro de EE.UU.). */
        private String ofacListUrl = "https://www.treasury.gov/ofac/downloads/sdn.csv";
        /** URL pública de la lista consolidada del Consejo de Seguridad de la ONU. */
        private String unListUrl = "https://scsanctions.un.org/resources/xml/en/consolidated.xml";
        /** Similitud mínima (0-1, trigramas) para considerar coincidencia en listas restrictivas. */
        private double restrictiveListMatchThreshold = 0.82;
        /** Monto (COP) por operación a partir del cual se considera inusual por sí solo. */
        private BigDecimal unusualAmountThreshold = new BigDecimal("10000000");
        /** Múltiplo del promedio histórico del usuario a partir del cual el monto se considera atípico. */
        private BigDecimal unusualAmountMultiplier = new BigDecimal("5");
        /** Cantidad de operaciones en 24h a partir de la cual se considera frecuencia inusual. */
        private int maxTransactionsPerDay = 10;
        /** Fracción del umbral (ej. 0.8 = 80%) desde la cual una operación cuenta como "justo debajo" para fraccionamiento. */
        private double structuringLowerRatio = 0.8;
        /** Operaciones "justo debajo del umbral" en 24h necesarias para marcar patrón de fraccionamiento. */
        private int structuringMinOccurrences = 3;
        /** Ventana (minutos) para considerar un retiro/transferencia como "entrada-salida rápida" tras un depósito. */
        private int rapidInOutWindowMinutes = 60;
    }
}
