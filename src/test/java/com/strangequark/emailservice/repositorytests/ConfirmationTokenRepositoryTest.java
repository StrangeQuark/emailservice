package com.strangequark.emailservice.repositorytests;

import com.strangequark.emailservice.token.ConfirmationToken;
import com.strangequark.emailservice.token.ConfirmationTokenRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class ConfirmationTokenRepositoryTest {
    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    UUID token;

    @BeforeEach
    void setup() {
        token = UUID.randomUUID();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), "test@test.com");

        testEntityManager.persistAndFlush(confirmationToken);
    }

    @Test
    void findByTokenTest() {
        Optional<ConfirmationToken> confirmationToken = confirmationTokenRepository.findByToken(token);

        Assertions.assertTrue(confirmationToken.isPresent());
    }

    @Test
    void updateConfirmedAtTest() {
        LocalDateTime localDateTime = LocalDateTime.now().withNano((LocalDateTime.now().getNano() / 1_000_000) * 1_000_000);

        confirmationTokenRepository.updateConfirmedAt(token, localDateTime);

        testEntityManager.clear(); // Clear persistence context to ensure fresh fetch

        Optional<ConfirmationToken> confirmationToken = confirmationTokenRepository.findByToken(token);

        Assertions.assertEquals(confirmationToken.get().getConfirmedAt(), localDateTime);
    }

    @Test
    void deleteTokenTest() {
        confirmationTokenRepository.deleteToken(token);

        Assertions.assertTrue(confirmationTokenRepository.findByToken(token).isEmpty());
    }
}
