package com.cooperativa.met.infrastructure.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;

public class XssSanitizerDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value != null) {
            // Convierte caracteres HTML peligrosos (<, >, &, ", ') en entidades seguras (&lt;, &gt; etc)
            return HtmlUtils.htmlEscape(value);
        }
        return null;
    }
}
