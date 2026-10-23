package com.enterprise.orchestrator.service;

import com.enterprise.orchestrator.domain.AppUser;
import com.enterprise.orchestrator.exception.ResourceNotFoundException;
import com.enterprise.orchestrator.repository.AppUserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserLookup {
    private final AppUserRepository users;
    public UserLookup(AppUserRepository users) { this.users = users; }

    public AppUser byUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
