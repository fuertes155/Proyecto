package com.cooperativa.met.infrastructure.compliance;

import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.compliance.port.RestrictiveListProviderPort;
import com.cooperativa.met.domain.compliance.service.NameNormalizer;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.infrastructure.config.MetComplianceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Descarga y parsea la Lista Consolidada del Consejo de Seguridad de la ONU
 * (individuos y entidades sancionados) — pública, sin autenticación.
 *
 * Estructura esperada: {@code <CONSOLIDATED_LIST><INDIVIDUALS><INDIVIDUAL>...}
 * con nombre repartido en FIRST_NAME/SECOND_NAME/THIRD_NAME/FOURTH_NAME, y
 * {@code <ENTITIES><ENTITY>} con el nombre completo en FIRST_NAME. El parseo
 * es tolerante: si una etiqueta no existe simplemente no aporta texto, en vez
 * de romper el registro completo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnConsolidatedListProvider implements RestrictiveListProviderPort {

    private final MetComplianceProperties complianceProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ComplianceListType getListType() {
        return ComplianceListType.ONU;
    }

    @Override
    public List<RestrictiveListEntry> fetchEntries() {
        String url = complianceProperties.getSarlaft().getUnListUrl();
        byte[] xmlBytes = restTemplate.getForObject(url, byte[].class);
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new IllegalStateException("La lista consolidada de la ONU llegó vacía desde " + url);
        }

        Document document = parseXml(xmlBytes);
        List<RestrictiveListEntry> entries = new ArrayList<>();
        Instant now = Instant.now();

        entries.addAll(parseGroup(document, "INDIVIDUAL", now));
        entries.addAll(parseGroup(document, "ENTITY", now));

        if (entries.isEmpty()) {
            throw new IllegalStateException("No se pudo extraer ninguna entrada válida de la lista consolidada de la ONU " +
                    "(posible cambio de esquema XML)");
        }
        return entries;
    }

    private List<RestrictiveListEntry> parseGroup(Document document, String tagName, Instant now) {
        List<RestrictiveListEntry> result = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element)) continue;

            String name = switch (tagName) {
                case "INDIVIDUAL" -> joinNonBlank(
                        childText(element, "FIRST_NAME"),
                        childText(element, "SECOND_NAME"),
                        childText(element, "THIRD_NAME"),
                        childText(element, "FOURTH_NAME"));
                default -> childText(element, "FIRST_NAME");
            };
            if (name.isBlank()) continue;

            String normalized = NameNormalizer.normalize(name);
            if (normalized.isBlank()) continue;

            String dataId = childText(element, "DATAID");

            result.add(RestrictiveListEntry.builder()
                    .id(UUID.randomUUID())
                    .listType(ComplianceListType.ONU)
                    .fullName(name)
                    .normalizedName(normalized)
                    .sourceRef(dataId.isBlank() ? null : dataId)
                    .sourceUpdatedAt(now)
                    .createdAt(now)
                    .build());
        }
        return result;
    }

    private String childText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children.getLength() == 0) return "";
        String text = children.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    private String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(part.trim());
            }
        }
        return sb.toString();
    }

    private Document parseXml(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Anti-XXE: es XML de un tercero externo, nunca se debe resolver DTD/entidades externas.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xmlBytes));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo parsear el XML de la lista consolidada de la ONU", e);
        }
    }
}
