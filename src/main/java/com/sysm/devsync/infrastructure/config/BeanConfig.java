package com.sysm.devsync.infrastructure.config;

import com.sysm.devsync.application.*;
import com.sysm.devsync.application.security.SecurityService;
import com.sysm.devsync.domain.persistence.*;
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
    public WorkspaceService workspaceService(WorkspacePersistencePort workspacePersistencePort, UserPersistencePort userPersistencePort) {
        return new WorkspaceService(workspacePersistencePort, userPersistencePort);
    }

    @Bean
    public ProjectService projectService(WorkspacePersistencePort workspacePersistencePort, ProjectPersistencePort projectPersistencePort) {
        return new ProjectService(projectPersistencePort, workspacePersistencePort);
    }

    @Bean
    public QuestionService questionService(QuestionPersistencePort questionPersistencePort,
                                           ProjectPersistencePort projectPersistencePort,
                                           TagPersistencePort tagPersistencePort,
                                           UserPersistencePort userPersistencePort) {
        return new QuestionService(questionPersistencePort,
                projectPersistencePort,
                tagPersistencePort,
                userPersistencePort);

    }

    @Bean
    public AnswerService answerService(AnswerPersistencePort answerPersistencePort,
                                       QuestionPersistencePort questionPersistencePort,
                                       UserPersistencePort userPersistencePort){
        return new AnswerService(answerPersistencePort, questionPersistencePort, userPersistencePort);
    }

    @Bean
    public NoteService noteService(NotePersistencePort notePersistencePort,
                                   ProjectPersistencePort projectPersistence,
                                   UserPersistencePort userPersistence,
                                   TagPersistencePort tagPersistence) {
        return new NoteService(notePersistencePort, projectPersistence, userPersistence, tagPersistence);
    }

    @Bean
    public CommentService commentService(CommentPersistencePort commentPersistencePort,
                                         NotePersistencePort notePersistencePort,
                                         QuestionPersistencePort questionPersistencePort,
                                         AnswerPersistencePort answerPersistencePort,
                                         UserPersistencePort userPersistencePort){
        return new CommentService(commentPersistencePort,
                notePersistencePort,
                questionPersistencePort,
                answerPersistencePort,
                userPersistencePort);
    }

    @Bean("securityService")
    public SecurityService securityService(NotePersistencePort notePersistencePort,
                                           AnswerPersistencePort answerPersistencePort,
                                           QuestionPersistencePort questionPersistencePort,
                                           WorkspacePersistencePort workspacePersistencePort){
        return new SecurityService(notePersistencePort,
                answerPersistencePort,
                questionPersistencePort,
                workspacePersistencePort);
    }

}
