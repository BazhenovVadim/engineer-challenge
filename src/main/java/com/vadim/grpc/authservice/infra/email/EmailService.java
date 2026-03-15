package com.vadim.grpc.authservice.infra.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendPasswordReset(String to, String token) {
        log.info("Sending password reset to {}: token={}", to, token);
        //TODO: реальная отправка соо вместо лога
    }
}