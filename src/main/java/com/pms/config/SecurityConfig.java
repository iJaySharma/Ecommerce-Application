package com.pms.config;

import com.pms.security.CustomUserDetailsService;
import com.pms.security.JwtAuthEntryPoint;
import com.pms.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            JwtAuthEntryPoint jwtAuthEntryPoint,
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.userDetailsService = userDetailsService;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // public
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()

                    // category & product management -> ADMIN, SUPER_ADMIN
                    .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                    // categories -> SUPER_ADMIN only
                    .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("SUPER_ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("SUPER_ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("SUPER_ADMIN")

                    // user/role management -> SUPER_ADMIN only
                    .requestMatchers("/api/admin/users/**").hasRole("SUPER_ADMIN")
                    .requestMatchers("/api/admin/orders/**").hasRole("SUPER_ADMIN")

                    // cart, address, checkout, own orders -> any authenticated USER (and above)
                    .requestMatchers("/api/cart/**", "/api/addresses/**", "/api/orders/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")

                    .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())); // for h2-console

        return http.build();
    }
}
