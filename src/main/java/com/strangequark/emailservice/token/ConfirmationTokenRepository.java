package com.strangequark.emailservice.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {

    @Query("SELECT t FROM ConfirmationToken t WHERE t.token = ?1")
    Optional<ConfirmationToken> findByToken(String token);

    @Transactional
    @Modifying
    @Query("UPDATE ConfirmationToken t SET t.confirmedAt = ?2 WHERE t.token = ?1")
    int updateConfirmedAt(String token, LocalDateTime confirmedAt);

    @Transactional
    @Modifying
    @Query(value = "DELETE ConfirmationToken t WHERE t.token = ?1")
    int deleteToken(String token);

//    @Transactional
//    @Modifying
//    @Query(value = "DELETE t FROM ConfirmationToken t WHERE t.email = ?1")
//    int deleteTokenByEmail(String email);
}
