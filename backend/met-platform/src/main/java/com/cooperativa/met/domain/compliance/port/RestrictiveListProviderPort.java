package com.cooperativa.met.domain.compliance.port;

import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.identity.model.ComplianceListType;

import java.util.List;

/**
 * Fuente externa de una lista restrictiva (OFAC, ONU, etc.). Cada
 * implementación sabe descargar y parsear el formato propio de su
 * proveedor; el resto del sistema solo ve {@link RestrictiveListEntry}.
 */
public interface RestrictiveListProviderPort {

    ComplianceListType getListType();

    /** Descarga y parsea la lista completa. Lanza excepción si falla la descarga/parseo. */
    List<RestrictiveListEntry> fetchEntries();
}
