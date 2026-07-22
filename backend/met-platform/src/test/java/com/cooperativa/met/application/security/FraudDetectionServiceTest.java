package com.cooperativa.met.application.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import com.cooperativa.met.application.security.GeoLocationService.GeoLocationResponse;
import com.cooperativa.met.domain.common.exception.FraudDetectionException;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private GeoLocationService geoLocationService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private UUID userId;
    private String colombiaIp;
    private String europeIp;
    private String proxyIp;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        colombiaIp = "190.0.0.1";
        europeIp = "80.0.0.1";
        proxyIp = "104.0.0.1";

        // Mocks for redis operations when accessed
        // Avoid lenient stubbing by only stubbing when needed in tests
    }

    @Test
    void checkIpReputation_throwsException_whenIpIsProxy() {
        GeoLocationResponse response = new GeoLocationResponse();
        response.setStatus("success");
        response.setProxy(true);
        when(geoLocationService.getLocationInfo(proxyIp)).thenReturn(response);

        FraudDetectionException exception = assertThrows(FraudDetectionException.class, 
            () -> fraudDetectionService.checkIpReputation(proxyIp));
        assertEquals("FRAUD_VPN_DETECTED", exception.getCode());
    }

    @Test
    void checkIpReputation_passes_whenIpIsClean() {
        GeoLocationResponse response = new GeoLocationResponse();
        response.setStatus("success");
        response.setProxy(false);
        when(geoLocationService.getLocationInfo(colombiaIp)).thenReturn(response);

        assertDoesNotThrow(() -> fraudDetectionService.checkIpReputation(colombiaIp));
    }

    @Test
    void checkImpossibleTravel_throwsException_whenSpeedExceedsLimit() {
        // Mock Last Location (Bogota, Colombia) - recorded 1 hour ago
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        String lastLocData = "4.7110,-74.0721," + oneHourAgo;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login_loc:" + userId)).thenReturn(lastLocData);

        // Mock Current Location (Madrid, Spain) - distance is ~8000 km
        GeoLocationResponse currentGeo = new GeoLocationResponse();
        currentGeo.setStatus("success");
        currentGeo.setLat(40.4168);
        currentGeo.setLon(-3.7038);
        when(geoLocationService.getLocationInfo(europeIp)).thenReturn(currentGeo);

        // 8000 km in 1 hour is 8000 km/h, exceeds 1000 km/h
        FraudDetectionException exception = assertThrows(FraudDetectionException.class, 
            () -> fraudDetectionService.checkImpossibleTravel(userId, europeIp));
        assertEquals("FRAUD_IMPOSSIBLE_TRAVEL", exception.getCode());
    }

    @Test
    void checkImpossibleTravel_passes_whenTravelIsPossible() {
        // Mock Last Location (Bogota, Colombia) - recorded 10 hours ago
        long tenHoursAgo = System.currentTimeMillis() - (10 * 60 * 60 * 1000);
        String lastLocData = "4.7110,-74.0721," + tenHoursAgo;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login_loc:" + userId)).thenReturn(lastLocData);

        // Mock Current Location (Medellin, Colombia)
        GeoLocationResponse currentGeo = new GeoLocationResponse();
        currentGeo.setStatus("success");
        currentGeo.setLat(6.2442);
        currentGeo.setLon(-75.5812);
        when(geoLocationService.getLocationInfo(colombiaIp)).thenReturn(currentGeo);

        // Distance ~ 250 km. 250 km in 10 hours is 25 km/h, valid.
        assertDoesNotThrow(() -> fraudDetectionService.checkImpossibleTravel(userId, colombiaIp));
    }

    @Test
    void checkTransferVelocity_throwsException_whenThresholdExceeded() {
        UUID destAccountId = UUID.randomUUID();
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(eq("transfer_vel:" + userId), eq(destAccountId.toString()))).thenReturn(1L);
        when(redisTemplate.getExpire("transfer_vel:" + userId)).thenReturn(50L);
        
        // Mock size = 6 (Threshold is 5)
        when(setOperations.size("transfer_vel:" + userId)).thenReturn(6L);

        FraudDetectionException exception = assertThrows(FraudDetectionException.class, 
            () -> fraudDetectionService.checkTransferVelocity(userId, destAccountId));
        assertEquals("FRAUD_VELOCITY_EXCEEDED", exception.getCode());
    }
}
