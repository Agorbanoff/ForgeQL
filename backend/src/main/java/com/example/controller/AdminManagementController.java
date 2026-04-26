package com.example.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.controller.dtos.request.AssignDataSourceAccessDTO;
import com.example.controller.dtos.request.UpdateDataSourceAccessDTO;
import com.example.controller.dtos.request.UpdateUserRoleDTO;
import com.example.controller.dtos.response.AdminUserDTO;
import com.example.controller.dtos.response.DataSourceAccessDTO;
import com.example.service.AdminManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @PostMapping("/datasources/{id}/access")
    public ResponseEntity<Void> assignAccess(
            @PathVariable Integer id,
            @Valid @RequestBody AssignDataSourceAccessDTO request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        adminManagementService.assignAccess(authenticatedUser.userId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/datasources/{id}/access/{userId}")
    public ResponseEntity<Void> updateAccess(
            @PathVariable Integer id,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateDataSourceAccessDTO request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        adminManagementService.updateAccess(authenticatedUser.userId(), id, userId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/datasources/{id}/access/{userId}")
    public ResponseEntity<Void> removeAccess(
            @PathVariable Integer id,
            @PathVariable Integer userId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        adminManagementService.removeAccess(authenticatedUser.userId(), id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/datasources/{id}/access")
    public ResponseEntity<List<DataSourceAccessDTO>> listAccess(
            @PathVariable Integer id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(adminManagementService.listAccess(authenticatedUser.userId(), id));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> listUsers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(adminManagementService.listUsers(authenticatedUser.userId()));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRoleDTO request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        adminManagementService.updateUserRole(authenticatedUser.userId(), id, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
