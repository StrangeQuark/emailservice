package com.strangequark.emailservice.servicetests;

import com.strangequark.emailservice.email.EmailRequest;
import com.strangequark.emailservice.email.EmailService;
import com.strangequark.emailservice.response.Response; // Integration line: Auth
import com.strangequark.emailservice.template.EmailTemplate;
import com.strangequark.emailservice.template.EmailTemplateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.UUID; // Integration line: Auth

public class EmailServiceTest extends BaseServiceTest {
    @Autowired
    private EmailService emailService;
    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

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
    void sendEmailWithoutTokenTest() {
        EmailRequest emailRequest = new EmailRequest("recipient@test.com", "sender@test.com",
                "Email body", "Email subject", false);

        ResponseEntity<?> response =  emailService.sendEmail(emailRequest, false);

        Assertions.assertEquals(200, response.getStatusCode().value());

        // Expect there to be 1 element in the CTR, since we initialize one in setup function
        Assertions.assertEquals(1, confirmationTokenRepository.findAll().size());
    }

    @Test
    void sendEmailWithTokenTest() {
        EmailRequest emailRequest = new EmailRequest("recipient@test.com", "sender@test.com",
                "Email body", "Email subject", true);

        ResponseEntity<?> response =  emailService.sendEmail(emailRequest, false);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(confirmationTokenRepository.findAll().get(1).getEmail(), "recipient@test.com");
    }

    @Test
    void sendTemplateEmailTest() {
        emailTemplateRepository.save(new EmailTemplate("NAME", "SUBJECT", "BODY"));

        EmailRequest emailRequest = new EmailRequest("recipient@test.com", "sender@test.com",
                true, "NAME", null);

        ResponseEntity<?> response =  emailService.sendTemplateEmail(emailRequest, false);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(confirmationTokenRepository.findAll().get(1).getEmail(), "recipient@test.com");
    }

    @Test
    void createTemplateEmailTest() {
        EmailRequest emailRequest = new EmailRequest("TEST TEMPLATE BODY", "THIS IS A TEST SUBJECT", "T_NAME");

        ResponseEntity<?> response =  emailService.createTemplateEmail(emailRequest, false);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(emailTemplateRepository.findByName("T_NAME").isPresent());
    }

    @Test
    void confirmTokenTest() {
        ResponseEntity<?> response =  emailService.confirmToken(token);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(confirmationTokenRepository.findByToken(token).get().getConfirmedAt());
    }
    // Integration function start: Auth
    @Test
    void enableUserTest() {
        ResponseEntity<?> response = emailService.enableUser(UUID.randomUUID());

        Assertions.assertEquals(404, response.getStatusCode().value());
        Assertions.assertEquals("Token not found", ((Response) response.getBody()).getMessage());
    }

    @Test
    void resetUserPasswordTest() {
        ResponseEntity<?> response = emailService.resetUserPassword(UUID.randomUUID(), "newPassword");

        Assertions.assertEquals(404, response.getStatusCode().value());
        Assertions.assertEquals("Token not found", ((Response) response.getBody()).getMessage());
    }// Integration function end: Auth
}
