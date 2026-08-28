package com.strangequark.emailservice.servicetests;

import com.strangequark.emailservice.email.EmailRequest;
import com.strangequark.emailservice.email.EmailService;
import com.strangequark.emailservice.response.Response; // Integration line: Auth
import com.strangequark.emailservice.template.EmailTemplate;
import com.strangequark.emailservice.token.ConfirmationToken;
import com.strangequark.emailservice.token.TokenPurpose;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.UUID; // Integration line: Auth
import java.time.LocalDateTime;

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

        Assertions.assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void emailApiAccessRequiredTest() {
        Mockito.when(jwtUtility.validateEmailApiAccess()).thenReturn(false);
        EmailRequest emailRequest = new EmailRequest("recipient@test.com", "sender@test.com",
                "Email body", "Email subject", false);

        ResponseEntity<?> response = emailService.sendEmail(emailRequest, true);

        Assertions.assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void getTemplateEmailTest() {
        ResponseEntity<?> response =  emailService.getTemplateEmail(testTemplateName, false);

        Assertions.assertEquals(200, response.getStatusCode().value());

        EmailTemplate template = (EmailTemplate) response.getBody();
        Assertions.assertEquals(testTemplateSubject, template.getSubject());
        Assertions.assertEquals(testTemplateBody, template.getBody());
    }

    @Test
    void sendTemplateEmailTest() {
        EmailRequest emailRequest = new EmailRequest("recipient@test.com", "sender@test.com",
                true, testTemplateName, null);

        ResponseEntity<?> response =  emailService.sendTemplateEmail(emailRequest, false);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNull(((Response) response.getBody()).getToken());
        Assertions.assertEquals(confirmationTokenRepository.findAll().get(1).getEmail(), "recipient@test.com");
    }

    @Test
    void createTemplateEmailTest() {
        String createTestTemplateName = "T_NAME";

        EmailRequest emailRequest = new EmailRequest("TEST TEMPLATE BODY", "THIS IS A TEST SUBJECT", createTestTemplateName);
        emailRequest.setTokenPurpose("INVITATION");

        ResponseEntity<?> response =  emailService.createTemplateEmail(emailRequest, false);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(emailTemplateRepository.findByName(createTestTemplateName).isPresent());
        Assertions.assertEquals("INVITATION", emailTemplateRepository.findByName(createTestTemplateName).get().getTokenPurpose());
    }

    @Test
    void updateTemplateEmailTest() {
        EmailRequest emailRequest = new EmailRequest("UPDATED TEMPLATE BODY", "UPDATED TEMPLATE SUBJECT", testTemplateName);

        ResponseEntity<?> response = emailService.updateTemplateEmail(emailRequest, false);

        Assertions.assertEquals(200, response.getStatusCode().value());
        EmailTemplate template = emailTemplateRepository.findByName(testTemplateName).get();
        Assertions.assertEquals("UPDATED TEMPLATE BODY", template.getBody());
        Assertions.assertEquals("UPDATED TEMPLATE SUBJECT", template.getSubject());
        Assertions.assertEquals(TokenPurpose.REGISTRATION.name(), template.getTokenPurpose());
    }

    @Test
    void deleteTemplateEmailTest() {
        String createTestTemplateName = "T_NAME";
        emailTemplateRepository.save(new EmailTemplate(createTestTemplateName, "TEST TEMPLATE SUBJECT", "TEST TEMPLATE BODY", "INVITATION"));

        ResponseEntity<?> response = emailService.deleteTemplateEmail(createTestTemplateName, false);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(emailTemplateRepository.findByName(createTestTemplateName).isEmpty());
    }

    @Test
    void deleteSystemTemplateEmailTest() {
        ResponseEntity<?> response = emailService.deleteTemplateEmail(testTemplateName, false);

        Assertions.assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void customTokenCannotResetPasswordTest() {
        UUID customToken = UUID.randomUUID();
        confirmationTokenRepository.save(new ConfirmationToken(customToken, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), "test@test.com", "INVITATION"));

        ResponseEntity<?> response = emailService.resetUserPassword(customToken, "newPassword");

        Assertions.assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void registrationTokenCannotResetPasswordTest() {
        ResponseEntity<?> response = emailService.resetUserPassword(token, "newPassword");

        Assertions.assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void passwordResetTokenCannotConfirmEmailTest() {
        UUID passwordResetToken = UUID.randomUUID();
        confirmationTokenRepository.save(new ConfirmationToken(passwordResetToken, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), "test@test.com", TokenPurpose.PASSWORD_RESET.name()));

        ResponseEntity<?> response = emailService.confirmToken(passwordResetToken);

        Assertions.assertEquals(404, response.getStatusCode().value());
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
    }

    @Test
    void passwordResetTokenIsNotConfirmedWhenAuthResetFailsTest() {
        UUID passwordResetToken = UUID.randomUUID();
        confirmationTokenRepository.save(new ConfirmationToken(passwordResetToken, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), "test@test.com", TokenPurpose.PASSWORD_RESET.name()));

        Mockito.doThrow(new RuntimeException("Auth service unavailable"))
                .when(authUtility).resetPassword("test@test.com", "newPassword");

        emailService.resetUserPassword(passwordResetToken, "newPassword");

        Assertions.assertNull(confirmationTokenRepository.findByToken(passwordResetToken).get().getConfirmedAt());
    }
    // Integration function end: Auth
}
