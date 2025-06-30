package com.sysm.devsync.infrastructure.config.security;


import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation for checking if the current user is an ADMIN
 * or the owner of the Answer being accessed.
 * <p>
 * The target method must have a parameter named 'answerId'.
 * Example: public void updateAnswer(@PathVariable("answerId") String answerId, ...)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or @securityService.isAnswerOwner(authentication.name, #answerId)")
public @interface IsAnswerOwnerOrAdmin {
}
