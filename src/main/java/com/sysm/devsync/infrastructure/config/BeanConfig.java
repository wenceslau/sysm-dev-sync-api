package com.sysm.devsync.infrastructure.config;

import com.sysm.devsync.application.TagService;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
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

}
