package com.cooperativa.met.domain.identity.port;

/**
 * Puerto de dominio para notificaciones relacionadas con seguridad de identidad.
 * La implementación concreta vive en la capa de infraestructura (adaptador de email).
 */
public interface NotificationPort {

    /**
     * Notifica al usuario que su cuenta ha sido bloqueada por intentos fallidos.
     *
     * @param email dirección de correo del usuario
     */
    void sendAccountLockedEmail(String email);

    /**
     * Notifica al usuario que se detectó un inicio de sesión desde una IP no conocida.
     *
     * @param email dirección de correo del usuario
     * @param ip    dirección IP desde la que se inició sesión
     */
    void sendNewLoginFromNewIpEmail(String email, String ip);
}
