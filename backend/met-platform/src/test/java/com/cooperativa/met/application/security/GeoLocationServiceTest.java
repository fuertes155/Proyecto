package com.cooperativa.met.application.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Geolocalización por IP usada por la detección de fraude (viaje imposible, VPN/hosting). */
class GeoLocationServiceTest {

    private GeoLocationService geoLocationService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        geoLocationService = new GeoLocationService();
        server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(geoLocationService, "restTemplate")).build();
    }

    @Test
    void shouldReturnCountryCoordinatesAndProxyFlags() {
        server.expect(requestTo("http://ip-api.com/json/190.85.10.4?fields=131379")).andRespond(withSuccess(
                "{\"status\":\"success\",\"countryCode\":\"CO\",\"lat\":6.2518,\"lon\":-75.5636,\"proxy\":true,\"hosting\":false}",
                MediaType.APPLICATION_JSON));

        GeoLocationService.GeoLocationResponse location = geoLocationService.getLocationInfo("190.85.10.4");

        assertEquals("CO", location.getCountryCode());
        assertEquals(6.2518, location.getLat());
        assertTrue(location.isProxy());
    }

    @Test
    void shouldNotCallTheApiForLocalhost() {
        GeoLocationService.GeoLocationResponse location = geoLocationService.getLocationInfo("127.0.0.1");

        assertEquals("success", location.getStatus());
        assertEquals("CO", location.getCountryCode());
        server.verify();
    }

    @Test
    void shouldFailOpenWhenTheProviderCannotLocateTheIp() {
        server.expect(requestTo("http://ip-api.com/json/10.0.0.1?fields=131379"))
                .andRespond(withSuccess("{\"status\":\"fail\",\"message\":\"private range\"}", MediaType.APPLICATION_JSON));

        GeoLocationService.GeoLocationResponse location = geoLocationService.getLocationInfo("10.0.0.1");

        assertEquals("fail", location.getStatus());
        assertNull(location.getCountryCode());
        assertFalse(location.isProxy());
    }

    @Test
    void shouldFailOpenWhenTheProviderIsDown() {
        server.expect(requestTo("http://ip-api.com/json/8.8.8.8?fields=131379")).andRespond(withServerError());

        assertEquals("fail", geoLocationService.getLocationInfo("8.8.8.8").getStatus());
    }
}
