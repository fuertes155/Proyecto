package com.cooperativa.met.domain.notification.port;

public interface PushNotificationPort {
    /**
     * Envia una notificacion push a un usuario especifico.
     * 
     * @param userId El identificador del usuario (puede ser usado como topic o mapped a device token)
     * @param title El titulo de la notificacion
     * @param body El cuerpo o mensaje de la notificacion
     */
    void sendPushNotification(String userId, String title, String body);
}
