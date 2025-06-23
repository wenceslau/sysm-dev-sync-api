package com.sysm.devsync.infrastructure.config;

import com.sysm.devsync.application.TagService;
import com.sysm.devsync.application.UserService;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    private final TagPersistencePort tagPersistencePort;

    public BeanConfig(TagPersistencePort tagPersistencePort) {
        this.tagPersistencePort = tagPersistencePort;
    }

    @Bean
    public TagService tagServiceBean() {
        return new TagService(tagPersistencePort);
    }

    @Bean
    public UserService userServiceBean(UserPersistencePort userPersistencePort) {
        return new UserService(userPersistencePort);
    }

}
