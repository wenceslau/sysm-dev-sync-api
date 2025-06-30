package com.sysm.devsync.infrastructure.config.security;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or @securityService.canUserAcceptAnswer(authentication.name, #answerId)")
public @interface CanUserAcceptAnswer {
}
