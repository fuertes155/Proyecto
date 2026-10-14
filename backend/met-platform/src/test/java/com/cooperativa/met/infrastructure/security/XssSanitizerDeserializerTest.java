package com.cooperativa.met.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XssSanitizerDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new SimpleModule().addDeserializer(String.class, new XssSanitizerDeserializer()));

    @Test
    void shouldKeepAccentedLettersUntouched() throws Exception {
        // Un nombre en español no es un ataque: debe guardarse tal como se escribió.
        assertEquals("María Pabón Ñuñez", mapper.readValue("\"María Pabón Ñuñez\"", String.class));
    }

    @Test
    void shouldEscapeHtmlSpecialCharacters() throws Exception {
        assertEquals("&lt;b&gt;Tom &amp; Jerry&lt;/b&gt; &#39;x&#39; &quot;y&quot;",
                mapper.readValue("\"<b>Tom & Jerry</b> 'x' \\\"y\\\"\"", String.class));
    }
}
