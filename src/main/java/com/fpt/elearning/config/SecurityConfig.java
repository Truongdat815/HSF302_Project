package com.fpt.elearning.config;

import com.fpt.elearning.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Tài nguyên tĩnh + trang công khai
                .requestMatchers("/", "/courses/**", "/css/**", "/js/**", "/images/**",
                        "/register", "/login", "/api/chat/**",
                        "/api/payments/sepay/webhook", "/error").permitAll()
                // Khu vực admin
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                // Giỏ hàng, thanh toán, học bài -> cần đăng nhập
                .requestMatchers("/cart/**", "/checkout/**", "/payment/**",
                        "/learn/**", "/my-courses/**", "/reviews/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                    response.sendRedirect(request.getContextPath() + (isAdmin ? "/admin" : "/"));
                })
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .permitAll()
            )
            // Bỏ qua CSRF cho API chat và webhook SePay (server-to-server)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/chat/**", "/api/payments/sepay/webhook"));

        return http.build();
    }
}
