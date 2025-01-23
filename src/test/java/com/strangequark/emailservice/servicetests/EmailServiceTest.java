package com.strangequark.emailservice.servicetests;

import com.strangequark.emailservice.email.EmailRequest;
import com.strangequark.emailservice.email.EmailService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class EmailServiceTest extends BaseServiceTest {
    @Autowired
    private EmailService emailService;

    @BeforeEach
    void init() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendTest() {
        ResponseEntity<?> response =  emailService.send("recipient@test.com", "sender@test.com",
                "Email body", "Email subject");

        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void sendEmailWithTokenTest() {
        EmailRequest emailRequest = new EmailRequest("recipient@test.com", "sender@test.com",
                "Email body", "Email subject");

        ResponseEntity<?> response =  emailService.sendEmailWithToken(emailRequest, false, false);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(confirmationTokenRepository.findAll().get(1).getEmail(), "recipient@test.com");
    }

    @Test
    void confirmTokenTest() {
        ResponseEntity<?> response =  emailService.confirmToken(token);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(confirmationTokenRepository.findByToken(token).get().getConfirmedAt());
    }
}
