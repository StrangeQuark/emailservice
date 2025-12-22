package com.strangequark.emailservice.email;

import com.strangequark.emailservice.response.Response;
import com.strangequark.emailservice.template.EmailTemplate;
import com.strangequark.emailservice.template.EmailTemplateRepository;
import com.strangequark.emailservice.token.ConfirmationToken;
import com.strangequark.emailservice.token.ConfirmationTokenRepository;
import com.strangequark.emailservice.utility.AuthUtility; // Integration line: Auth
import com.strangequark.emailservice.utility.JwtUtility; // Integration line: Auth
import com.strangequark.emailservice.utility.TelemetryUtility; // Integration line: Telemetry
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException; // Integration line: Auth

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map; // Integration line: Telemetry
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Configuration
@EnableScheduling
public class EmailService implements EmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender javaMailSender;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailValidator emailValidator;
    private final EmailTemplateRepository emailTemplateRepository;
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");
    private static final Pattern SYSTEM_VAR_PATTERN = Pattern.compile("\\[\\[([a-zA-Z0-9_]+)]]");
    // Integration function start: Auth
    @Autowired
    AuthUtility authUtility;
    @Autowired
    JwtUtility jwtUtility;
    // Integration function end: Auth
    // Integration function start: Telemetry
    @Autowired
    TelemetryUtility telemetryUtility;
    // Integration function end: Telemetry

    public EmailService(JavaMailSender javaMailSender, ConfirmationTokenRepository confirmationTokenRepository,
                        EmailValidator emailValidator, EmailTemplateRepository emailTemplateRepository) {
        this.javaMailSender = javaMailSender;
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.emailValidator = emailValidator;
        this.emailTemplateRepository = emailTemplateRepository;
    }

    @Override
    @Async
    public ResponseEntity<?> send(String recipient, String sender, String body, String subject) {
        LOGGER.info("Attempting to send an email");

        if(!emailValidator.test(recipient)) {
            LOGGER.error("Invalid recipient email address");
            return ResponseEntity.status(400).body(new Response("Invalid recipient email address"));
        }
        if(!emailValidator.test(sender)) {
            LOGGER.error("Invalid sender email address");
            return ResponseEntity.status(400).body(new Response("Invalid sender email address"));
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");

            mimeMessageHelper.setText(body, true);
            mimeMessageHelper.setTo(recipient);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setFrom(sender);
            javaMailSender.send(mimeMessage);
            // Integration function start: Telemetry
            try {
                telemetryUtility.sendTelemetryEvent("email-send",
                        Map.of(
                                "userId", jwtUtility.extractId(), // Integration line: Auth
                                "sender-domain", sender.substring(sender.indexOf("@") + 1),
                                "recipient-domain", recipient.substring(recipient.indexOf("@") + 1)
                        )
                );
            } catch (Exception ex) {
                LOGGER.error("Failed to send telemetry event during email send: " + ex.getMessage());
                LOGGER.debug("Stack trace: ", ex);
            }// Integration function end: Telemetry

            LOGGER.info("Email successfully sent");
            return ResponseEntity.ok(new Response("Your email has been sent"));
        } catch(MessagingException ex) {
            LOGGER.error("Failed to send email: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(
                    new Response("There was an error when sending email, please contact the system administrator")
            );
        } catch(IllegalArgumentException ex) {
            LOGGER.error("Illegal argument when sending email: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(
                    new Response(ex.getMessage())
            );
        }
    }

    public ResponseEntity<?> sendEmail(EmailRequest request, boolean requireJwt) {
        // Integration function start: Auth
        if(requireJwt && !jwtUtility.validateToken()) {
            LOGGER.error("Invalid JWT token");
            return ResponseEntity.status(400).body(new Response("Invalid JWT token"));
        }
        // Integration function end: Auth

        try {
            //Create an email confirmation token
            UUID token = UUID.randomUUID();
            ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), request.getRecipient());

            //Send the email
            ResponseEntity<?> response = send(request.getRecipient(),
                    request.getSender(),
                    request.getBody(),
                    request.getSubject());
            if(response.getStatusCode().value() != 200) {
                LOGGER.error(response.toString());
                return response;
            }

            LOGGER.info("Email has been successfully sent");

            if(request.getIncludeToken()) {
                confirmationTokenRepository.save(confirmationToken);
                return ResponseEntity.ok(new Response("Email successfully sent", token));
            }
            return ResponseEntity.ok(new Response("Email successfully sent"));
        } catch (Exception ex) {
            LOGGER.error("Failed to send email: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(
                    new Response("Failed to send email: " + ex.getMessage())
            );
        }
    }

    public ResponseEntity<?> sendTemplateEmail(EmailRequest request, boolean requireJwt) {
        // Integration function start: Auth
        if(requireJwt && !jwtUtility.validateToken()) {
            LOGGER.error("Invalid JWT token");
            return ResponseEntity.status(400).body(new Response("Invalid JWT token"));
        }
        // Integration function end: Auth

        try {
            LOGGER.info("Setting email values from template");
            EmailTemplate template = emailTemplateRepository.findByName(request.getTemplateName())
                    .orElseThrow(() -> new RuntimeException("Template was not found"));

            //Create an email confirmation token
            UUID token = UUID.randomUUID();
            ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), request.getRecipient());

            if(request.getIncludeToken())
                template.setBody(template.getBody().replace("[[confirmationToken]]", token.toString()));

            //Send the email
            ResponseEntity<?> response = send(request.getRecipient(),
                    request.getSender(),
                    renderTemplateVars(template.getBody(), request.getTemplateVariables()),
                    template.getSubject());
            if(response.getStatusCode().value() != 200) {
                LOGGER.error(response.toString());
                return response;
            }

            LOGGER.info("Template email has been successfully sent");
            if(request.getIncludeToken()) {
                confirmationTokenRepository.save(confirmationToken);
                return ResponseEntity.ok(new Response("Template email successfully sent", token));
            }
            return ResponseEntity.ok(new Response("Template email successfully sent"));
        } catch (Exception ex) {
            LOGGER.error("Failed to send template email: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(
                    new Response("Failed to send template email: " + ex.getMessage())
            );
        }
    }

    private Set<String> extractVars(String template) {
        Matcher matcher = VAR_PATTERN.matcher(template);
        Set<String> vars = new HashSet<>();
        while (matcher.find()) {
            vars.add(matcher.group(1));
        }
        return vars;
    }

    private String renderTemplateVars(String template, Map<String, String> vars) {
        Set<String> required = extractVars(template);

        if (required.isEmpty())
            return template;

        if (vars == null || vars.isEmpty())
            throw new IllegalArgumentException("Template variables are required: " + required);

        Set<String> missing = required.stream()
                .filter(v -> !vars.containsKey(v))
                .collect(Collectors.toSet());

        if (!missing.isEmpty())
            throw new IllegalArgumentException("Missing template variables: " + missing);

        String result = template;
        for (var entry : vars.entrySet()) {
            result = result.replace(
                    "{{" + entry.getKey() + "}}",
                    entry.getValue()
            );
        }
        return result;
    }

    private String insertConfirmationToken(String template, String token) {
        return template.replace("[[confirmationToken]]", token);
    }

    @Transactional
    public ResponseEntity<?> confirmToken(UUID token) {
        LOGGER.info("Attempting to confirm token");

        ConfirmationToken confirmationToken;

        try {
            confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new IllegalStateException("Token not found"));

            //Check if the email has already been confirmed
            if (confirmationToken.getConfirmedAt() != null) {
                LOGGER.error("Token has already been confirmed");
                return ResponseEntity.status(409).body(new Response("The token is already confirmed"));
            }

            //Check if the token has expired
            if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                LOGGER.error("Token has expired");
                return ResponseEntity.status(409).body(new Response("The token has expired"));
            }

            confirmationTokenRepository.updateConfirmedAt(token, LocalDateTime.now());
        } catch (Exception ex) {
            LOGGER.error("Failed to confirm token: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(404).body(
                    new Response("Token not found")
            );
        }

        telemetryUtility.sendTelemetryEvent("email-confirm-token", Map.of()); // Integration line: Telemetry
        LOGGER.info("Token successfully confirmed");
        return ResponseEntity.ok(new Response("Token successfully confirmed", confirmationToken.getEmail()));
    }
    // Integration function start: Auth
    @Transactional
    public ResponseEntity<?> enableUser(UUID token) {
        LOGGER.info("Attempting to enable user");

        try {
            ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new IllegalStateException("Token not found"));

            //Check if the email has already been confirmed
            if (confirmationToken.getConfirmedAt() != null) {
                LOGGER.error("Token already confirmed when attempting to enable user");
                return ResponseEntity.status(409).body(new Response("The token has already been confirmed"));
            }

            //Check if the token has expired
            if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                LOGGER.error("Token has expired when attempting to enable user - resending email");

                EmailRequest emailRequest = new EmailRequest(confirmationToken.getEmail(),
                        "donotreply@emailservice.com", true, "USER_REGISTER",
                        Map.of("link", "http://localhost:6080/confirm-email?token=" + token));

                sendTemplateEmail(emailRequest, false);
                return ResponseEntity.status(409).body(new Response("The token has expired - A new confirmation email has been sent"));
            }

            //Call the AuthService to enable the User
            LOGGER.debug("Attempting to send enableUser call to Auth service");
            try {
                authUtility.enableUser(confirmationToken.getEmail());
            } catch (ResourceAccessException resourceAccessException) {
                LOGGER.error("Unable to reach the Auth service");
                LOGGER.error(resourceAccessException.getMessage());
                return ResponseEntity.status(500).body(new Response("Error when enabling user - unable to reach auth service"));
            }

            confirmationTokenRepository.updateConfirmedAt(token, LocalDateTime.now());
        } catch (Exception ex) {
            LOGGER.error("Failed to enable user: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(404).body(
                    new Response("Token not found")
            );
        }

        telemetryUtility.sendTelemetryEvent("email-enable-user", Map.of()); // Integration line: Telemetry
        LOGGER.info("Account verified, user enabled");
        return ResponseEntity.ok(new Response("Account verified, user enabled"));
    }

    @Transactional
    public ResponseEntity<?> resetUserPassword(UUID token, String newPassword) {
        LOGGER.info("Attempting to confirm token and reset user password");

        if(!jwtUtility.validateToken()) {
            LOGGER.error("Invalid JWT token");
            return ResponseEntity.status(400).body(new Response("Invalid JWT token"));
        }

        ConfirmationToken confirmationToken;

        try {
            confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new IllegalStateException("Token not found"));

            //Check if the email has already been confirmed
            if (confirmationToken.getConfirmedAt() != null) {
                LOGGER.error("Token has already been confirmed");
                return ResponseEntity.status(409).body(new Response("The token is already confirmed"));
            }

            //Check if the token has expired
            if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                LOGGER.error("Token has expired");
                return ResponseEntity.status(409).body(new Response("The token has expired"));
            }

            confirmationTokenRepository.updateConfirmedAt(token, LocalDateTime.now());

            authUtility.resetPassword(confirmationToken.getEmail(), newPassword);
            telemetryUtility.sendTelemetryEvent("email-reset-password", Map.of()); // Integration line: Telemetry
        } catch (Exception ex) {
            LOGGER.error("Failed to reset user password: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(404).body(new Response(ex.getMessage()));
        }

        LOGGER.info("User password successfully reset");
        return ResponseEntity.ok(new Response("User password successfully reset", confirmationToken.getEmail()));
    }// Integration function end: Auth
}
