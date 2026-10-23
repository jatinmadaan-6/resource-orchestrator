package com.enterprise.orchestrator.security;

import com.enterprise.orchestrator.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Bridges Spring Security to our own users table. Role EMPLOYEE becomes authority ROLE_EMPLOYEE. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername()).password(u.getPassword()).roles(u.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
