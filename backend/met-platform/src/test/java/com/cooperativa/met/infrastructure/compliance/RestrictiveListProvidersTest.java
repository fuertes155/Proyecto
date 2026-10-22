package com.cooperativa.met.infrastructure.compliance;

import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.compliance.service.NameNormalizer;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.infrastructure.config.MetComplianceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Descarga y lectura de las listas restrictivas OFAC (CSV) y ONU (XML), sin salir a internet. */
class RestrictiveListProvidersTest {

    private final MetComplianceProperties properties = new MetComplianceProperties();

    @Test
    void shouldParseTheOfacCsvIncludingNamesWithCommas() {
        OfacSdnListProvider provider = new OfacSdnListProvider(properties);
        server(provider)
                .expect(requestTo(properties.getSarlaft().getOfacListUrl()))
                .andExpect(header(HttpHeaders.USER_AGENT, org.hamcrest.Matchers.startsWith("Mozilla/5.0")))
                .andRespond(withSuccess("""
                        36,"AEROCARIBBEAN AIRLINES",-0- ,"CUBA",-0-
                        173,"ANGLO-CARIBBEAN CO., LTD.",-0- ,"CUBA",-0-

                        999,"-0-",-0-
                        solo-una-columna
                        2674,"ESCOBAR GAVIRIA, Pablo Emilio","individual","SDNT",-0-
                        """, MediaType.TEXT_PLAIN));

        List<RestrictiveListEntry> entries = provider.fetchEntries();

        assertEquals(ComplianceListType.OFAC, provider.getListType());
        assertEquals(List.of("AEROCARIBBEAN AIRLINES", "ANGLO-CARIBBEAN CO., LTD.", "ESCOBAR GAVIRIA, Pablo Emilio"),
                entries.stream().map(RestrictiveListEntry::getFullName).toList());
        assertEquals("ESCOBAR GAVIRIA PABLO EMILIO", entries.get(2).getNormalizedName());
        assertEquals("2674", entries.get(2).getSourceRef());
    }

    @Test
    void shouldFailInsteadOfWipingTheListWhenOfacSendsNothingUsable() {
        OfacSdnListProvider empty = new OfacSdnListProvider(properties);
        server(empty).expect(requestTo(properties.getSarlaft().getOfacListUrl())).andRespond(withSuccess(" ", MediaType.TEXT_PLAIN));
        OfacSdnListProvider garbage = new OfacSdnListProvider(properties);
        server(garbage).expect(requestTo(properties.getSarlaft().getOfacListUrl())).andRespond(withSuccess("<html>mantenimiento</html>", MediaType.TEXT_HTML));

        assertThrows(IllegalStateException.class, empty::fetchEntries);
        assertThrows(IllegalStateException.class, garbage::fetchEntries);
    }

    @Test
    void shouldParseIndividualsAndEntitiesFromTheUnList() {
        UnConsolidatedListProvider provider = new UnConsolidatedListProvider(properties);
        server(provider).expect(requestTo(properties.getSarlaft().getUnListUrl())).andRespond(withSuccess(xml("""
                <CONSOLIDATED_LIST>
                  <INDIVIDUALS>
                    <INDIVIDUAL><DATAID>6908555</DATAID><FIRST_NAME>Ri</FIRST_NAME><SECOND_NAME>Won</SECOND_NAME><THIRD_NAME>Ho</THIRD_NAME></INDIVIDUAL>
                    <INDIVIDUAL><DATAID>1</DATAID><FIRST_NAME>  </FIRST_NAME></INDIVIDUAL>
                  </INDIVIDUALS>
                  <ENTITIES>
                    <ENTITY><FIRST_NAME>Al-Qaida</FIRST_NAME></ENTITY>
                  </ENTITIES>
                </CONSOLIDATED_LIST>
                """), MediaType.APPLICATION_XML));

        List<RestrictiveListEntry> entries = provider.fetchEntries();

        assertEquals(ComplianceListType.ONU, provider.getListType());
        assertEquals(List.of("Ri Won Ho", "Al-Qaida"), entries.stream().map(RestrictiveListEntry::getFullName).toList());
        assertEquals("6908555", entries.get(0).getSourceRef());
        assertNull(entries.get(1).getSourceRef());
        assertEquals("AL QAIDA", entries.get(1).getNormalizedName());
    }

    @Test
    void shouldRefuseXmlWithExternalEntitiesInsteadOfReadingLocalFiles() {
        // Ataque XXE: si el parser resolviera la entidad, el "nombre" sería el contenido de un archivo del servidor.
        UnConsolidatedListProvider provider = new UnConsolidatedListProvider(properties);
        server(provider).expect(requestTo(properties.getSarlaft().getUnListUrl())).andRespond(withSuccess(xml("""
                <?xml version="1.0"?>
                <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
                <CONSOLIDATED_LIST><ENTITIES><ENTITY><FIRST_NAME>&xxe;</FIRST_NAME></ENTITY></ENTITIES></CONSOLIDATED_LIST>
                """), MediaType.APPLICATION_XML));

        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::fetchEntries);

        assertTrue(ex.getMessage().contains("parsear"), ex.getMessage());
    }

    @Test
    void shouldFailWhenTheUnSchemaChangesAndNothingCanBeRead() {
        UnConsolidatedListProvider provider = new UnConsolidatedListProvider(properties);
        server(provider).expect(requestTo(properties.getSarlaft().getUnListUrl()))
                .andRespond(withSuccess(xml("<NEW_FORMAT><PERSON name=\"x\"/></NEW_FORMAT>"), MediaType.APPLICATION_XML));

        assertThrows(IllegalStateException.class, provider::fetchEntries);
    }

    @Test
    void shouldNormalizeNamesSoAccentsAndPunctuationDoNotHideAMatch() {
        assertEquals("MARIA JOSE PEREZ NUNEZ", NameNormalizer.normalize("  María José Pérez-Núñez. "));
        assertEquals("", NameNormalizer.normalize(null));
        assertEquals("", NameNormalizer.normalize("--- ..."));
    }

    private static MockRestServiceServer server(Object provider) {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(provider, "restTemplate");
        return MockRestServiceServer.bindTo(restTemplate).build();
    }

    private static byte[] xml(String content) {
        return content.strip().getBytes(StandardCharsets.UTF_8);
    }
}
