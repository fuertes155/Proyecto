package com.cooperativa.met.infrastructure.adapter;

import com.cooperativa.met.domain.notification.port.PushNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GoPushNotificationAdapter implements PushNotificationPort {

    private final RestTemplate restTemplate;

    @Value("${met.notification-service.url}")
    private String notificationServiceUrl;

    @Value("${met.notification-service.api-key}")
    private String apiKey;

    public GoPushNotificationAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendPushNotification(String userId, String title, String body) {
        String endpoint = notificationServiceUrl + "/api/v1/notifications/push";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", userId);
        requestBody.put("title", title);
        requestBody.put("body", body);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            restTemplate.postForEntity(endpoint, requestEntity, String.class);
            log.info("Push notification request sent to Go service for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send push notification via Go service to userId: {}", userId, e);
        }
    }
}
