package com.pms.service;

import com.pms.dto.response.UserResponse;
import com.pms.enums.RoleName;

import java.util.List;

public interface UserManagementService {
    List<UserResponse> getAllUsers();
    UserResponse updateUserRole(Long userId, RoleName role);
    UserResponse setUserEnabled(Long userId, boolean enabled);
}
