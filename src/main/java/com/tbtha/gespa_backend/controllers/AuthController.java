package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.auth.AuthService;
import com.tbtha.gespa_backend.dtos.AcceptProfessionalInvitationRequest;
import com.tbtha.gespa_backend.dtos.CheckEmailRequest;
import com.tbtha.gespa_backend.dtos.CheckEmailResponse;
import com.tbtha.gespa_backend.dtos.LoginRequest;
import com.tbtha.gespa_backend.dtos.LoginResponse;
import com.tbtha.gespa_backend.dtos.MeResponse;
import com.tbtha.gespa_backend.dtos.PasswordResetConfirmRequest;
import com.tbtha.gespa_backend.dtos.PasswordResetRequest;
import com.tbtha.gespa_backend.dtos.PasswordResetRequestResponse;
import com.tbtha.gespa_backend.dtos.RefreshTokenRequest;
import com.tbtha.gespa_backend.dtos.RegisterPatientRequest;
import com.tbtha.gespa_backend.dtos.RegisterPatientResponse;
import com.tbtha.gespa_backend.dtos.SwitchRoleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/professional")
    public ResponseEntity<LoginResponse> loginProfessional(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAsProfessional(request));
    }

    @PostMapping("/login/patient")
    public ResponseEntity<LoginResponse> loginPatient(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAsPatient(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/switch-role")
    public ResponseEntity<LoginResponse> switchRole(@Valid @RequestBody SwitchRoleRequest request) {
        return ResponseEntity.ok(authService.switchRole(request.refreshToken(), request.role()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetRequestResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        return ResponseEntity.ok(authService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<Void> acceptProfessionalInvitation(
            @Valid @RequestBody AcceptProfessionalInvitationRequest request
    ) {
        authService.acceptProfessionalInvitation(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register/patient")
    public ResponseEntity<RegisterPatientResponse> registerPatient(
            @Valid @RequestBody RegisterPatientRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerPatient(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PostMapping("/check-email")
    public ResponseEntity<CheckEmailResponse> checkEmail(@RequestBody CheckEmailRequest request) {
        return ResponseEntity.ok(authService.checkEmail(request.email()));
    }
}
