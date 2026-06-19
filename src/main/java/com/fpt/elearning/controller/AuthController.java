package com.fpt.elearning.controller;

import com.fpt.elearning.entity.Role;
import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.RoleRepository;
import com.fpt.elearning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String password,
                           RedirectAttributes ra) {
        if (userRepository.existsByEmail(email)) {
            ra.addFlashAttribute("error", "Email đã được sử dụng.");
            return "redirect:/register";
        }
        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STUDENT").build()));

        userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(studentRole))
                .enabled(true)
                .build());

        ra.addFlashAttribute("success", "Đăng ký thành công! Mời bạn đăng nhập.");
        return "redirect:/login";
    }
}
