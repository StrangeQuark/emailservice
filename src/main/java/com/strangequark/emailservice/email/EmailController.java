package com.strangequark.emailservice.email;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/email")
@CrossOrigin
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping(path = "/send-email")
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequest request) {
        return emailService.sendEmail(request, true);
    }

    @PostMapping(path = "/send-template-email")
    public ResponseEntity<?> sendTemplateEmail(@RequestBody EmailRequest request) {
        return emailService.sendTemplateEmail(request, true);
    }

    @PostMapping(path = "/send-email-with-token")
    public ResponseEntity<?> sendEmailWithToken(@RequestBody EmailRequest request) {
        return emailService.sendEmailWithToken(request, false, false);
    }

    @PostMapping(path = "/send-register-email")
    public ResponseEntity<?> sendRegisterEmail(@RequestBody EmailRequest request) {
        return emailService.sendEmailWithToken(request, true, false);
    }

    @PostMapping(path = "/send-password-reset-email")
    public ResponseEntity<?> sendPasswordResetEmail(@RequestBody EmailRequest request) {
        return emailService.sendEmailWithToken(request, false, true);
    }

    @GetMapping(path = "/confirm-token")
    public ResponseEntity<?> confirmToken(@RequestParam("token") UUID token) {
        return emailService.confirmToken(token);
    }
    // Integration function start: Auth
    @GetMapping(path = "/enable-user")
    public ResponseEntity<?> enableUser(@RequestParam("token") UUID token) {
        return emailService.enableUser(token);
    }

    @PostMapping(path = "/reset-user-password")
    public ResponseEntity<?> resetUserPassword(@RequestParam("token") UUID token, @RequestParam("newPassword") String newPassword) {
        return emailService.resetUserPassword(token, newPassword);
    } // Integration function end: Auth
}
