package com.cooperativa.met.infrastructure.web.support;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooperativa.met.application.support.dto.ChatRequest;
import com.cooperativa.met.application.support.dto.ChatResponse;
import com.cooperativa.met.domain.support.service.AiChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.cooperativa.met.application.support.dto.ContactFormRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String reply = aiChatService.generateReply(request.getMessage());
        return ResponseEntity.ok(ChatResponse.builder().reply(reply).build());
    }

    @PostMapping("/public/contact")
    public ResponseEntity<Void> submitContactForm(@Valid @RequestBody ContactFormRequest request) {
        log.info("Nuevo mensaje de contacto recibido de: {} ({})", request.getName(), request.getEmail());
        log.info("Contenido del mensaje: {}", request.getMessage());
        
        // TODO: En el futuro, esto puede persistirse en base de datos o enviar un correo a soporte
        return ResponseEntity.ok().build();
    }
}
