package com.strangequark.emailservice.servicetests;

import com.strangequark.emailservice.token.ConfirmationToken;
import com.strangequark.emailservice.token.ConfirmationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseServiceTest {
    @Autowired
    public ConfirmationTokenRepository confirmationTokenRepository;
    @MockBean
    JavaMailSender javaMailSender;

    @BeforeEach
    void setup() {
        ConfirmationToken confirmationToken = new ConfirmationToken(UUID.randomUUID().toString(), LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), "test@test.com");

        confirmationTokenRepository.save(confirmationToken);
    }
}
