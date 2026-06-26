package com.terraformlabs.ums.dto;

import com.terraformlabs.ums.entity.Role;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateUserRolesRequest(@NotEmpty Set<Role> roles) {
}
