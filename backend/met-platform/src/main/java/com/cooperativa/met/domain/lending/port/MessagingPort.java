package com.cooperativa.met.domain.lending.port;

public interface MessagingPort {
    void sendSms(String phoneNumber, String message);
    void sendWhatsApp(String phoneNumber, String message);
}
