package com.sysm.devsync;

import com.sysm.devsync.application.*;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.infrastructure.controllers.dto.request.*;
import com.sysm.devsync.infrastructure.repositories.persistence.TagPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.AbstractEnvironment;

import java.util.Map;

@SpringBootApplication
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.setProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, "development");
        SpringApplication.run(Main.class, args);
    }

    @Bean
    @Profile("development")
    public ApplicationRunner initData(TagService tagService,
                                      UserService userService,
                                      WorkspaceService workspaceService,
                                      ProjectService projectService,
                                      QuestionService questionService,
                                      AnswerService answerService,
                                      NoteService noteService,
                                      CommentService commentService) {
        return args -> {
            // Check if data already exists to avoid re-populating on every restart
            if (userService.searchUsers(SearchQuery.of(Page.of(0, 1), Map.of())).total() > 0) {
                log.info("--- Database already contains data. Skipping sample data initialization. ---");
                return;
            }

            log.info("--- Initializing sample data ---");

            // 1. Create Users
            log.info("Creating users...");
            var user1Response = userService.createUser(new UserCreateUpdate("Wenceslau Neto", "wbaneto@gmail.com", null, UserRole.ADMIN));
            var user2Response = userService.createUser(new UserCreateUpdate("Jane Smith", "jane.smith@example.com", null, UserRole.MEMBER));
            String user1Id = user1Response.id();
            String user2Id = user2Response.id();
            log.info("Users created: {}, {}", user1Id, user2Id);

            // 2. Create Tags
            log.info("Creating tags...");
            var tagJavaResponse = tagService.createTag(new TagCreateUpdate("Java", "#000000", "Java programming language", "Backend"));
            var tagSpringResponse = tagService.createTag(new TagCreateUpdate("Spring Boot", "#0000ff", "Spring Boot framework", "Backend"));
            var tagReactResponse = tagService.createTag(new TagCreateUpdate("React", "#ff0000", "React JavaScript library", "Frontend"));
            var tagDockerResponse = tagService.createTag(new TagCreateUpdate("Docker", "#ffffff", "Containerization platform", "DevOps"));
            String tagJavaId = tagJavaResponse.id();
            String tagSpringId = tagSpringResponse.id();
            String tagReactId = tagReactResponse.id();
            log.info("Tags created.");

            // 3. Create a Workspace
            log.info("Creating workspace...");
            var wsResponse = workspaceService.createWorkspace(new WorkspaceCreateUpdate("DevSync Main Workspace", "The primary workspace for all DevSync projects.", false), user1Id);
            String workspaceId = wsResponse.id();
            log.info("Workspace created: {}", workspaceId);

            // 4. Add members to the workspace
            log.info("Adding members to workspace...");
            workspaceService.addMemberToWorkspace(workspaceId, user2Id);
            log.info("User {} added to workspace {}", user2Id, workspaceId);

            // 5. Create Projects
            log.info("Creating projects...");
            var project1Response = projectService.createProject(new ProjectCreateUpdate("DevSync API", "The core backend API for the DevSync application.", workspaceId));
            var project2Response = projectService.createProject(new ProjectCreateUpdate("DevSync Frontend", "The React-based user interface for DevSync.", workspaceId));
            String project1Id = project1Response.id();
            String project2Id = project2Response.id();
            log.info("Projects created: {}, {}", project1Id, project2Id);

            // 6. Create a Question
            log.info("Creating a question...");
            var questionResponse = questionService.createQuestion(new QuestionCreateUpdate("How to handle authentication in Spring Boot 3?", "We need to implement JWT-based authentication. What are the best practices and required dependencies?", project1Id), user2Id);
            String questionId = questionResponse.id();
            questionService.addTagToQuestion(questionId, tagJavaId);
            questionService.addTagToQuestion(questionId, tagSpringId);
            log.info("Question created: {}", questionId);

            // 7. Create an Answer
            log.info("Creating an answer...");
            var answerResponse = answerService.createAnswer(new AnswerCreateUpdate("You should use the Spring Security starter and a library like `jjwt` for handling JWTs. Start by creating a `SecurityFilterChain` bean and a filter to process the token on each request."), questionId, user1Id);
            String answerId = answerResponse.id();
            answerService.acceptAnswer(answerId); // Mark it as the accepted answer
            log.info("Answer created and accepted: {}", answerId);

            // 8. Create a Note
            log.info("Creating a note...");
            var noteResponse = noteService.createNote(new NoteCreateUpdate("Frontend State Management Strategy", "We should evaluate using Redux Toolkit or Zustand for managing the global state in our React application. Let's list the pros and cons.", project2Id), user1Id);
            String noteId = noteResponse.id();
            noteService.addTagToNote(noteId, tagReactId);
            log.info("Note created: {}", noteId);

            // 9. Create Comments
            log.info("Creating comments...");
            // Comment on the question
            commentService.createComment(new CommentCreateUpdate(TargetType.QUESTION, questionId, "Good question! I was about to ask the same thing."), user1Id);
            // Comment on the note
            commentService.createComment(new CommentCreateUpdate(TargetType.NOTE, noteId, "I've heard good things about Zustand's simplicity. Let's start there."), user2Id);
            log.info("Comments created.");

            log.info("--- Sample data initialization complete ---");
        };
    }

}
