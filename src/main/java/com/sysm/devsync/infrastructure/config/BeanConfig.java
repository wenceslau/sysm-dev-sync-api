package com.sysm.devsync.infrastructure.config;

import com.sysm.devsync.application.TagService;
import com.sysm.devsync.application.UserService;
import com.sysm.devsync.application.WorkspaceService;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import com.sysm.devsync.domain.persistence.WorkspacePersistencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public TagService tagServiceBean(TagPersistencePort tagPersistencePort) {
        return new TagService(tagPersistencePort);
    }

    @Bean
    public UserService userServiceBean(UserPersistencePort userPersistencePort) {
        return new UserService(userPersistencePort);
    }

    @Bean
    public WorkspaceService workspaceService(WorkspacePersistencePort workspacePersistencePort, UserPersistencePort userPersistencePort){
        return new WorkspaceService(workspacePersistencePort, userPersistencePort);
    }

}
