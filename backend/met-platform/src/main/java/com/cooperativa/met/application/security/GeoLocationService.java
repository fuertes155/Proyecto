package com.cooperativa.met.application.security;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeoLocationService {

    private final RestTemplate restTemplate;
    // URL uses ip-api.com. It's free but limited to 45 requests per minute.
    // fields=status,message,countryCode,lat,lon,proxy,hosting
    private static final String API_URL = "http://ip-api.com/json/{ip}?fields=131379";

    public GeoLocationService() {
        this.restTemplate = new RestTemplate();
    }

    @Data
    public static class GeoLocationResponse {
        private String status;
        private String message;
        private String countryCode;
        private Double lat;
        private Double lon;
        private boolean proxy;
        private boolean hosting;
    }

    /**
     * Retrieves geolocation data for a given IP.
     * Caches the result to avoid hitting rate limits.
     * 
     * @param ip The IP address (IPv4 or IPv6)
     * @return GeoLocationResponse
     */
    @Cacheable(value = "geo_location", key = "#ip", unless = "#result == null || 'fail'.equals(#result.status)")
    public GeoLocationResponse getLocationInfo(String ip) {
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return localDevelopmentResponse();
        }

        try {
            log.debug("Llamando API de Geolocalizacion para IP: {}", ip);
            GeoLocationResponse response = restTemplate.getForObject(API_URL, GeoLocationResponse.class, ip);
            if (response != null && "success".equals(response.getStatus())) {
                return response;
            } else {
                log.warn("Fallo geolocalización para IP {}: {}", ip, response != null ? response.getMessage() : "null");
            }
        } catch (Exception e) {
            log.error("Error contactando el servicio de geolocalizacion para IP: {}", ip, e);
        }
        
        // Retornar un default permisivo si el servicio externo falla (fail-open)
        return defaultResponse();
    }

    private GeoLocationResponse localDevelopmentResponse() {
        GeoLocationResponse r = new GeoLocationResponse();
        r.setStatus("success");
        r.setCountryCode("CO");
        r.setLat(4.7110);
        r.setLon(-74.0721);
        r.setProxy(false);
        r.setHosting(false);
        return r;
    }

    private GeoLocationResponse defaultResponse() {
        GeoLocationResponse r = new GeoLocationResponse();
        r.setStatus("fail");
        r.setProxy(false);
        r.setHosting(false);
        return r;
    }
}
