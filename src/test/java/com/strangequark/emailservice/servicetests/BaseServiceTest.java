package com.strangequark.emailservice.servicetests;

import com.strangequark.emailservice.template.EmailTemplate;
import com.strangequark.emailservice.template.EmailTemplateRepository;
import com.strangequark.emailservice.token.ConfirmationToken;
import com.strangequark.emailservice.token.ConfirmationTokenRepository;
import com.strangequark.emailservice.token.TokenPurpose;
import com.strangequark.emailservice.utility.JwtUtility; // Integration line: Auth
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when; // Integration line: Auth

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseServiceTest {

    static {
        System.setProperty("ENCRYPTION_KEY", "8C636049C7763F06A35A17E86A542B15");
        System.setProperty("SERVICE_SECRET_EMAIL", "testClientPassword"); // Integration line: Auth
    }

    @Autowired
    public ConfirmationTokenRepository confirmationTokenRepository;
    @MockitoBean
    public JavaMailSender javaMailSender;
    @Autowired
    public EmailTemplateRepository emailTemplateRepository;
    @MockitoBean // Integration line: Auth
    public JwtUtility jwtUtility; // Integration line: Auth

    public UUID token;
    public String testTemplateName = "USER_REGISTER";
    public String testTemplateSubject = "TEST_TEMPLATE_SUBJECT";
    public String testTemplateBody = "TEST TEMPLATE BODY";

    @BeforeEach
    void setup() {
        when(jwtUtility.validateEmailApiAccess()).thenReturn(true); // Integration line: Auth
        token = UUID.randomUUID();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), "test@test.com", TokenPurpose.REGISTRATION.name());

        confirmationTokenRepository.save(confirmationToken);
        emailTemplateRepository.save(new EmailTemplate(testTemplateName, testTemplateSubject, testTemplateBody, TokenPurpose.REGISTRATION.name()));
    }

    @AfterEach
    void teardown() {
        confirmationTokenRepository.deleteAll();
        confirmationTokenRepository.flush();
        emailTemplateRepository.deleteAll();
        emailTemplateRepository.flush();
    }
}
