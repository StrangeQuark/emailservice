package com.strangequark.emailservice.email;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping(path = "sendEmail")
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequest request) {
        return emailService.send(request.getRecipient(), request.getSender(), request.getEmail(), request.getSubject());
    }

    @PostMapping(path = "sendEmailWithToken")
    public ResponseEntity<?> sendEmailWithToken(@RequestBody EmailRequest request) {
        return emailService.sendEmailWithToken(request, false, false);
    }

    @PostMapping(path = "sendRegisterEmail")
    public ResponseEntity<?> sendRegisterEmail(@RequestBody EmailRequest request) {
        return emailService.sendEmailWithToken(request, true, false);
    }

    @PostMapping(path = "sendPasswordResetEmail")
    public ResponseEntity<?> sendPasswordResetEmail(@RequestBody EmailRequest request) {
        return emailService.sendEmailWithToken(request, false, true);
    }

    @GetMapping(path = "confirmToken")
    public ResponseEntity<?> confirmToken(@RequestParam("token") String token) {
        return emailService.confirmToken(token);
    }
}
