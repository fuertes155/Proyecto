package com.cooperativa.met.infrastructure.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class XssSanitizerDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value != null) {
            // Convierte caracteres HTML peligrosos (<, >, &, ", ') en entidades seguras (&lt;, &gt; etc).
            // Se indica UTF-8 a propósito: sin ese parámetro HtmlUtils también reemplaza las letras
            // con tilde y la ñ ("Pabón" quedaba guardado como "Pab&oacute;n").
            return HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name());
        }
        return null;
    }
}
