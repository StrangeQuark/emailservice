package com.strangequark.emailservice.email;

import com.strangequark.emailservice.response.Response;
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
import java.util.Map; // Integration line: Telemetry
import java.util.UUID;

@Service
@Configuration
@EnableScheduling
public class EmailService implements EmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender javaMailSender;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailValidator emailValidator;
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

    public EmailService(JavaMailSender javaMailSender, ConfirmationTokenRepository confirmationTokenRepository, EmailValidator emailValidator) {
        this.javaMailSender = javaMailSender;
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.emailValidator = emailValidator;
    }

    @Override
    @Async
    public ResponseEntity<?> send(String recipient, String sender, String email, String subject) {
        LOGGER.info("Attempting to send an email");

        // Integration function start: Auth
        if(!jwtUtility.validateToken()) {
            LOGGER.error("Invalid JWT token");
            return ResponseEntity.status(400).body(new Response("Invalid JWT token"));
        }
        // Integration function end: Auth
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

            mimeMessageHelper.setText(email, true);
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

    public ResponseEntity<?> sendEmailWithToken(EmailRequest request, boolean isRegister, boolean isPasswordReset) {
        LOGGER.info("Attempting to send an email with a token");

        // Integration function start: Auth
        if(!jwtUtility.validateToken()) {
            LOGGER.error("Invalid JWT token");
            return ResponseEntity.status(400).body(new Response("Invalid JWT token"));
        }
        // Integration function end: Auth
        if(!emailValidator.test(request.getRecipient())) {
            LOGGER.error("Invalid recipient email address");
            return ResponseEntity.status(400).body(new Response("Invalid recipient email address"));
        }
        if(!emailValidator.test(request.getSender())) {
            LOGGER.error("Invalid sender email address");
            return ResponseEntity.status(400).body(new Response("Invalid sender email address"));
        }

        try {
            //Create an email confirmation token
            UUID token = UUID.randomUUID();
            ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), request.getRecipient());

            //Send the email
            ResponseEntity<?> response = send(request.getRecipient(),
                    request.getSender(),
                    isRegister ? buildUserSignupEmail("http://react-service/confirm-email?token=" + token) :
                            isPasswordReset ? buildPasswordResetEmail("http://react-service/new-password?token=" + token) : request.getEmail(),
                    request.getSubject());
            if(response.getStatusCode().value() != 200) {
                LOGGER.error(response.toString());
                return response;
            }

            //Save the confirmation token to the database
            confirmationTokenRepository.save(confirmationToken);

            LOGGER.info("Token email has been successfully sent");
            //Return the token
            return ResponseEntity.ok(new Response("Email with token successfully sent", token));
        } catch (Exception ex) {
            LOGGER.error("Failed to send email with token: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(
              new Response("There was an error sending token email, please contact the system administrator")
            );
        }
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
                sendEmailWithToken(new EmailRequest(confirmationToken.getEmail(), "donotreply@emailservice.com", null, "Account confirmation"), true, false);
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

    private String buildUserSignupEmail(String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Activate your account</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hello,</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Please click on the confirmation link below to activate your account: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">" + link + "</a> </p></blockquote>\n This link will expire in 15 minutes. <p>Thank you</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }

    private String buildPasswordResetEmail(String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Reset your password</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi,</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Please click on the below link to reset your password: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Reset password</a> </p></blockquote>\n This link will expire in 15 minutes. <p>Thank you</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }
}
