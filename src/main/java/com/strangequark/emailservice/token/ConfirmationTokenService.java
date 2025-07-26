package com.strangequark.emailservice.token;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConfirmationTokenService {
    private final ConfirmationTokenRepository confirmationTokenRepository;

    public ConfirmationTokenService(ConfirmationTokenRepository confirmationTokenRepository) {
        this.confirmationTokenRepository = confirmationTokenRepository;
    }

    public void saveConfirmationToken(ConfirmationToken token) {
        confirmationTokenRepository.save(token);
    }

    public Optional<ConfirmationToken> getToken(UUID token) {
        return confirmationTokenRepository.findByToken(token);
    }

    public int setConfirmedAt(UUID token) {
        return confirmationTokenRepository.updateConfirmedAt(token, LocalDateTime.now());
    }

    public void deleteToken(UUID token) {
        confirmationTokenRepository.deleteToken(token);
    }
}
