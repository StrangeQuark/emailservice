package com.strangequark.emailservice.template;

import com.strangequark.emailservice.utility.LocalDateTimeEncryptDecryptConverter;
import com.strangequark.emailservice.utility.StringEncryptDecryptConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class EmailTemplate {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable=false)
    @Convert(converter = StringEncryptDecryptConverter.class)
    private String name;

    @Column(nullable=false)
    @Convert(converter = StringEncryptDecryptConverter.class)
    private String subject;

    @Column(nullable=false)
    @Convert(converter = StringEncryptDecryptConverter.class)
    private String body;

    @Column(nullable=false)
    @Convert(converter = LocalDateTimeEncryptDecryptConverter.class)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    @Convert(converter = LocalDateTimeEncryptDecryptConverter.class)
    private LocalDateTime updatedAt;
}
