package com.example.controller.dtos.request;

import com.example.persistence.Enums.GlobalRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleDTO(
        @NotNull(message = "globalRole is required")
        GlobalRole globalRole
) {
}
