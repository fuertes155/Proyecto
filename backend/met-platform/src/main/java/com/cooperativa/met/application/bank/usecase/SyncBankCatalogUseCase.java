package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayBank;
import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PseFinancialInstitution;
import com.cooperativa.met.domain.bank.port.PseGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Sincroniza el catálogo interno {@code banks} contra dos fuentes reales
 * de Wompi — dos productos distintos, cada uno con su propio namespace de
 * códigos de banco:
 * <ul>
 *   <li>"Pagos a Terceros" (payout) vía {@link PayoutGatewayPort} → llena
 *       {@code wompiBankId}/{@code supportsPayout}.</li>
 *   <li>Checkout/Transacciones (PSE) vía {@link PseGatewayPort} → llena
 *       {@code wompiPseCode}/{@code supportsPse}.</li>
 * </ul>
 * Se ejecuta bajo demanda desde el panel admin — nunca inventamos códigos
 * de banco del proveedor, solo reflejamos lo que cada API reporta.
 *
 * El emparejamiento con el catálogo interno se hace por nombre normalizado
 * (sin tildes, mayúsculas, sin espacios). Si un banco del proveedor no
 * tiene equivalente local, se crea una fila nueva automáticamente para no
 * perder cobertura — queda con "active=true" y puede desactivarse
 * manualmente si no aplica.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncBankCatalogUseCase {

    private final BankRepositoryPort bankRepository;
    private final PayoutGatewayPort payoutGatewayPort;
    private final PseGatewayPort pseGatewayPort;

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]");

    /** Largo de la llave primaria {@code banks.code} (VARCHAR(20), ver V30). */
    private static final int MAX_CODE_LENGTH = 20;

    @Transactional
    public int execute() {
        int updated = syncPayoutBanks();
        updated += syncPseBanks();
        log.info("Sincronización de catálogo de bancos completada: {} filas actualizadas", updated);
        return updated;
    }

    /**
     * Solo el catálogo PSE. Lo usa el arranque en sandbox, donde los payouts siguen
     * simulados y Wompi devuelve sus bancos de prueba ("1" aprueba, "2" rechaza).
     */
    @Transactional
    public int executePseOnly() {
        int updated = syncPseBanks();
        log.info("Sincronización de bancos PSE completada: {} filas actualizadas", updated);
        return updated;
    }

    private int syncPayoutBanks() {
        int updated = 0;
        for (PayoutGatewayBank gatewayBank : payoutGatewayPort.fetchSupportedBanks()) {
            Bank match = findOrCreateByName(gatewayBank.name());
            bankRepository.save(match.withPayoutMapping(gatewayBank.providerBankId(), gatewayBank.supportsPayout()));
            updated++;
        }
        return updated;
    }

    private int syncPseBanks() {
        int updated = 0;
        for (PseFinancialInstitution institution : pseGatewayPort.fetchFinancialInstitutions()) {
            Bank match = findOrCreateByName(institution.name());
            bankRepository.save(match.withPseMapping(institution.code()));
            updated++;
        }
        return updated;
    }

    private Bank findOrCreateByName(String providerName) {
        String normalizedName = normalize(providerName);
        List<Bank> localBanks = bankRepository.findAllActive();

        Bank match = localBanks.stream()
                .filter(b -> normalize(b.getName()).equals(normalizedName)
                        || normalize(b.getName()).contains(normalizedName)
                        || normalizedName.contains(normalize(b.getName())))
                .findFirst()
                .orElse(null);

        if (match != null) {
            return match;
        }

        log.info("Banco nuevo reportado por el proveedor sin equivalente local: {}", providerName);
        return Bank.builder()
                .code(newBankCode(normalizedName))
                .name(providerName)
                .active(true)
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Código para un banco nuevo: el nombre normalizado recortado a 20 caracteres
     * (p. ej. "BANCOCOOPERATIVOCOOPCENTRAL" no cabe), con sufijo numérico si ese
     * código ya existe — si no, el save sobrescribiría la fila de otro banco.
     */
    private String newBankCode(String normalizedName) {
        String base = normalizedName.isEmpty() ? "BANCO" : normalizedName;
        if (base.length() > MAX_CODE_LENGTH) {
            base = base.substring(0, MAX_CODE_LENGTH);
        }
        String candidate = base;
        for (int i = 2; bankRepository.findByCode(candidate).isPresent(); i++) {
            String suffix = String.valueOf(i);
            candidate = base.substring(0, Math.min(base.length(), MAX_CODE_LENGTH - suffix.length())) + suffix;
        }
        return candidate;
    }

    private String normalize(String name) {
        String withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NON_ALNUM.matcher(withoutAccents.toUpperCase()).replaceAll("");
    }
}
