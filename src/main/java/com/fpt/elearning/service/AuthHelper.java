package com.fpt.elearning.service;

import com.fpt.elearning.entity.User;
import com.fpt.elearning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Tien ich lay User dang dang nhmap tu SecurityContext.
 */
@Service
@RequiredArgsConstructor
public class AuthHelper {

    private final UserRepository userRepository;

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    public User requireCurrentUser() {
        User user = currentUser();
        if (user == null) {
            throw new IllegalStateException("Chua dang nhap");
        }
        return user;
    }
}
