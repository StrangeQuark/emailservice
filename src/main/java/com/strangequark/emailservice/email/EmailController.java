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

    @GetMapping(path = "/get-template-email")
    public ResponseEntity<?> getTemplateEmail(@RequestParam("templateName") String templateName) {
        return emailService.getTemplateEmail(templateName, true);
    }

    @PostMapping(path = "/send-template-email")
    public ResponseEntity<?> sendTemplateEmail(@RequestBody EmailRequest request) {
        return emailService.sendTemplateEmail(request, true);
    }

    @PostMapping(path = "/create-template-email")
    public ResponseEntity<?> createTemplateEmail(@RequestBody EmailRequest request) {
        return emailService.createTemplateEmail(request, true);
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
