package com.cooperativa.met.infrastructure.compliance;

import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.compliance.port.RestrictiveListProviderPort;
import com.cooperativa.met.domain.compliance.service.NameNormalizer;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.infrastructure.config.MetComplianceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descarga y parsea el SDN.csv (Specially Designated Nationals) de la OFAC —
 * lista pública del Tesoro de EE.UU., sin autenticación ni contrato requerido
 * (a diferencia de DataCrédito).
 *
 * Formato del archivo: CSV sin encabezado, columnas entre comillas dobles,
 * "-0-" como marcador de campo vacío. Solo nos interesa la columna 2 (nombre).
 * Se parsea línea por línea y se ignoran las que no calzan el formato en vez
 * de abortar todo el refresco por una fila corrupta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfacSdnListProvider implements RestrictiveListProviderPort {

    // Divide por comas que están fuera de comillas dobles.
    private static final Pattern CSV_SPLIT = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    private final MetComplianceProperties complianceProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ComplianceListType getListType() {
        return ComplianceListType.OFAC;
    }

    @Override
    public List<RestrictiveListEntry> fetchEntries() {
        String url = complianceProperties.getSarlaft().getOfacListUrl();
        // treasury.gov devuelve 403 a clientes sin User-Agent de navegador (bloquea el
        // "Java/x.x" por defecto de RestTemplate como si fuera tráfico de bot).
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        String csv = response.getBody();
        if (csv == null || csv.isBlank()) {
            throw new IllegalStateException("La lista OFAC llegó vacía desde " + url);
        }

        List<RestrictiveListEntry> entries = new ArrayList<>();
        Instant now = Instant.now();
        int skipped = 0;

        for (String line : csv.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            try {
                String[] cols = CSV_SPLIT.split(line, -1);
                if (cols.length < 2) {
                    skipped++;
                    continue;
                }
                String entNum = stripQuotes(cols[0]);
                String name = stripQuotes(cols[1]);
                if (name.isBlank() || "-0-".equals(name)) {
                    skipped++;
                    continue;
                }
                String normalized = NameNormalizer.normalize(name);
                if (normalized.isBlank()) {
                    skipped++;
                    continue;
                }
                entries.add(RestrictiveListEntry.builder()
                        .id(UUID.randomUUID())
                        .listType(ComplianceListType.OFAC)
                        .fullName(name)
                        .normalizedName(normalized)
                        .sourceRef(entNum)
                        .sourceUpdatedAt(now)
                        .createdAt(now)
                        .build());
            } catch (Exception e) {
                skipped++;
            }
        }

        if (skipped > 0) {
            log.warn("OFAC SDN: {} líneas ignoradas por formato inesperado de {} totales.", skipped, entries.size() + skipped);
        }
        if (entries.isEmpty()) {
            throw new IllegalStateException("No se pudo extraer ninguna entrada válida del SDN.csv de OFAC");
        }
        return entries;
    }

    private String stripQuotes(String value) {
        String trimmed = value.trim();
        Matcher m = Pattern.compile("^\"(.*)\"$").matcher(trimmed);
        return m.matches() ? m.group(1).trim() : trimmed;
    }
}
