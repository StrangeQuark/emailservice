package com.strangequark.emailservice.servicetests;

import com.strangequark.emailservice.email.EmailRequest;
import com.strangequark.emailservice.email.EmailService;
import com.strangequark.emailservice.email.EmailValidator;
import com.strangequark.emailservice.response.Response;
import com.strangequark.emailservice.template.EmailTemplate;
import com.strangequark.emailservice.template.EmailTemplateRepository;
import com.strangequark.emailservice.token.ConfirmationTokenRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

public class EmailServiceConflictTest {

    @Test
    void databaseDuplicateTemplateReturnsConflictTest() {
        EmailTemplateRepository emailTemplateRepository = Mockito.mock(EmailTemplateRepository.class);
        EmailService emailService = new EmailService(
                Mockito.mock(JavaMailSender.class),
                Mockito.mock(ConfirmationTokenRepository.class),
                Mockito.mock(EmailValidator.class),
                emailTemplateRepository
        );
        EmailRequest request = new EmailRequest("Template body", "Template subject", "DUPLICATE_TEMPLATE");

        Mockito.when(emailTemplateRepository.findByName(request.getTemplateName())).thenReturn(Optional.empty());
        Mockito.when(emailTemplateRepository.save(Mockito.any(EmailTemplate.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate template"));

        ResponseEntity<?> response = emailService.createTemplateEmail(request, false);

        Assertions.assertEquals(409, response.getStatusCode().value());
        Assertions.assertEquals("Template with name DUPLICATE_TEMPLATE already exists",
                ((Response) response.getBody()).getMessage());
    }
}
