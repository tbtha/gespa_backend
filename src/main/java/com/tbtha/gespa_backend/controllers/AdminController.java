package com.tbtha.gespa_backend.controllers;

import com.tbtha.gespa_backend.dtos.AdminCreateProfessionalInvitationRequest;
import com.tbtha.gespa_backend.dtos.AdminResetPasswordResponse;
import com.tbtha.gespa_backend.dtos.AdminUpdateUserStatusRequest;
import com.tbtha.gespa_backend.dtos.AdminUserResponse;
import com.tbtha.gespa_backend.dtos.ProfessionalInvitationResponse;
import com.tbtha.gespa_backend.services.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/professionals/invitations")
    public ResponseEntity<ProfessionalInvitationResponse> createProfessionalInvitation(
            @Valid @RequestBody AdminCreateProfessionalInvitationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createProfessionalInvitation(request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<AdminResetPasswordResponse> resetUserPassword(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.resetUserPassword(userId));
    }
}
