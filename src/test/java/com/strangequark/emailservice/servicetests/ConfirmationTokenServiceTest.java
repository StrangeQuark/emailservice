package com.strangequark.emailservice.servicetests;

import com.strangequark.emailservice.token.ConfirmationToken;
import com.strangequark.emailservice.token.ConfirmationTokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

public class ConfirmationTokenServiceTest extends BaseServiceTest {
    @Autowired
    ConfirmationTokenService confirmationTokenService;

    @Test
    void saveConfirmationTokenTest() {
        UUID token = UUID.randomUUID();

        ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), "test@test.com");

        confirmationTokenService.saveConfirmationToken(confirmationToken);

        Assertions.assertTrue(confirmationTokenRepository.findByToken(token).isPresent());
    }

    @Test
    void getTokenTest() {
        Assertions.assertTrue(confirmationTokenService.getToken(token).isPresent());
    }

    @Test
    void setConfirmedAtTest() {
        confirmationTokenService.setConfirmedAt(token);

        Assertions.assertNotNull(confirmationTokenRepository.findByToken(token).get().getConfirmedAt());
    }

    @Test
    void deleteTokenTest() {
        confirmationTokenService.deleteToken(token);

        Assertions.assertTrue(confirmationTokenRepository.findByToken(token).isEmpty());
    }
}
