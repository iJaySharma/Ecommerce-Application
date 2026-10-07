package com.pms.dto.request;

import com.pms.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {

    @NotNull(message = "Role is required")
    private RoleName role;
}
