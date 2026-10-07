package com.pms.config;

import com.pms.entity.Role;
import com.pms.entity.User;
import com.pms.entity.Cart;
import com.pms.enums.RoleName;
import com.pms.repository.CartRepository;
import com.pms.repository.RoleRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Seeds the three roles (USER, ADMIN, SUPER_ADMIN) on startup, and creates
 * a default SUPER_ADMIN account (username: superadmin / password: Admin@123)
 * so the system is usable immediately without manual DB inserts.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    
    public DataSeeder(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CartRepository cartRepository,
            PasswordEncoder passwordEncoder) {

        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName).orElseGet(() ->
                    roleRepository.save(Role.builder().name(roleName).build()));
        }

        if (!userRepository.existsByUsername("superadmin")) {
            Role superAdminRole = roleRepository.findByName(RoleName.ROLE_SUPER_ADMIN).orElseThrow();
            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);

            User superAdmin = User.builder()
                    .username("superadmin")
                    .email("superadmin@pms.local")
                    .password(passwordEncoder.encode("Admin@123"))
                    .enabled(true)
                    .roles(roles)
                    .build();

            User saved = userRepository.save(superAdmin);
            cartRepository.save(Cart.builder().user(saved).build());
        }
    }
}
