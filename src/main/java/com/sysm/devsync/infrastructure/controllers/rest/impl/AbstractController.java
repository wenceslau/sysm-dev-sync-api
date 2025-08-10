package com.sysm.devsync.infrastructure.controllers.rest.impl;

import com.sysm.devsync.Main;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class AbstractController {

    @Autowired
    private Environment environment;

    protected String authenticatedUserId() {

        if ("development".equals(this.environment.getDefaultProfiles()[0])){
            return Main.userDevId;
        }
        // SecurityContextHolder.getContext() will not be null.
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // This check correctly handles cases where no user is authenticated.
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // This is an excellent defensive check, though it's unlikely to be hit
            // if your SecurityFilterChain is configured with .anyRequest().authenticated()
            throw new RuntimeException("User not authenticated or no valid token found.");
        }
        return authentication.getName();
    }
}
