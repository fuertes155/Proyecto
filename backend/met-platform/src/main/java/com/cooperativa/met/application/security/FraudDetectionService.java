package com.cooperativa.met.application.security;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.cooperativa.met.domain.common.exception.FraudDetectionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final GeoLocationService geoLocationService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOGIN_LOC_KEY_PREFIX = "login_loc:";
    private static final String TRANSFER_VELOCITY_KEY_PREFIX = "transfer_vel:";
    
    // Configuración heurística
    private static final int MAX_VELOCITY_ACCOUNTS = 5;
    private static final long VELOCITY_WINDOW_SECONDS = 60; // 1 minuto
    private static final double MAX_TRAVEL_SPEED_KMH = 1000.0; // Velocidad avión comercial aprox.

    /**
     * Regla 1: Bloqueo de VPN/Tor/Proxies
     */
    public void checkIpReputation(String ip) {
        GeoLocationService.GeoLocationResponse geo = geoLocationService.getLocationInfo(ip);
        if (geo != null && (geo.isProxy() || geo.isHosting())) {
            log.warn("Login bloqueado por IP sospechosa (VPN/Proxy): {}", ip);
            throw new FraudDetectionException("FRAUD_VPN_DETECTED", "No se permite el acceso desde VPNs o proxies anónimos por seguridad.");
        }
    }

    /**
     * Guarda la latitud, longitud y timestamp del login exitoso.
     */
    public void recordLoginLocation(UUID userId, String ip) {
        GeoLocationService.GeoLocationResponse geo = geoLocationService.getLocationInfo(ip);
        if (geo != null && "success".equals(geo.getStatus()) && geo.getLat() != null && geo.getLon() != null) {
            String val = geo.getLat() + "," + geo.getLon() + "," + System.currentTimeMillis();
            redisTemplate.opsForValue().set(LOGIN_LOC_KEY_PREFIX + userId, val, Duration.ofDays(7));
        }
    }

    /**
     * Regla 2: Viaje Imposible (Impossible Travel)
     */
    public void checkImpossibleTravel(UUID userId, String currentIp) {
        String lastLocData = (String) redisTemplate.opsForValue().get(LOGIN_LOC_KEY_PREFIX + userId);
        if (lastLocData == null) return;

        GeoLocationService.GeoLocationResponse currentGeo = geoLocationService.getLocationInfo(currentIp);
        if (currentGeo == null || !"success".equals(currentGeo.getStatus()) || currentGeo.getLat() == null) return;

        String[] parts = lastLocData.split(",");
        double lastLat = Double.parseDouble(parts[0]);
        double lastLon = Double.parseDouble(parts[1]);
        long lastTime = Long.parseLong(parts[2]);

        long currentTime = System.currentTimeMillis();
        double hoursElapsed = (currentTime - lastTime) / (1000.0 * 60 * 60);

        // Si pasó menos de un minuto, asumimos que es el mismo intento (evitar dividir por ~0)
        if (hoursElapsed < 0.016) return;

        double distanceKm = calculateHaversineDistance(lastLat, lastLon, currentGeo.getLat(), currentGeo.getLon());
        double speedKmh = distanceKm / hoursElapsed;

        if (speedKmh > MAX_TRAVEL_SPEED_KMH && distanceKm > 100) {
            log.error("Viaje Imposible Detectado para userId={}. Distancia: {} km, Tiempo: {} h, Vel: {} km/h", 
                userId, distanceKm, hoursElapsed, speedKmh);
            
            // Acción definida por defecto: Rechazar la transacción y obligar re-autenticación
            throw new FraudDetectionException("FRAUD_IMPOSSIBLE_TRAVEL", 
                "Se ha detectado un acceso desde una ubicación físicamente imposible. Por seguridad, la operación fue rechazada.");
        }
    }

    /**
     * Regla 3: Controles de Velocidad (Transfer Velocity)
     */
    public void checkTransferVelocity(UUID userId, UUID destinationAccountId) {
        String key = TRANSFER_VELOCITY_KEY_PREFIX + userId;
        
        // Añadir la cuenta destino al Set
        redisTemplate.opsForSet().add(key, destinationAccountId.toString());
        
        // Si el Set acaba de crearse, configurarle el TTL
        Long ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl == -1) {
            redisTemplate.expire(key, VELOCITY_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        Long uniqueAccounts = redisTemplate.opsForSet().size(key);
        if (uniqueAccounts != null && uniqueAccounts > MAX_VELOCITY_ACCOUNTS) {
            log.warn("Velocity Check fallido para userId={}. Intentó transferir a {} cuentas en 1 minuto.", userId, uniqueAccounts);
            throw new FraudDetectionException("FRAUD_VELOCITY_EXCEEDED", 
                "Has excedido el límite de transferencias a múltiples cuentas en corto tiempo. Por favor intenta más tarde.");
        }
    }

    /**
     * Fórmula de Haversine para calcular distancia en km entre dos puntos de la tierra.
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la tierra en KM
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
