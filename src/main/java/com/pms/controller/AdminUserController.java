package com.pms.controller;

import com.pms.dto.request.RoleUpdateRequest;
import com.pms.dto.response.UserResponse;
import com.pms.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// SUPER_ADMIN only (enforced in SecurityConfig)
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(userManagementService.updateUserRole(id, request.getRole()));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enable(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.setUserEnabled(id, true));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disable(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.setUserEnabled(id, false));
    }
}
