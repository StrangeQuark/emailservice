package com.strangequark.emailservice.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequest {
    private String recipient;
    private String sender;
    private String email;
    private String subject;
}
