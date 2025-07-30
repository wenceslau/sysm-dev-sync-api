package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(QuestionPersistence.class)
public class QuestionPersistenceTest extends AbstractRepositoryTest {

    @Autowired
    private QuestionPersistence questionPersistence;

    // Prerequisite JPA entities
    private UserJpaEntity authorUserJpa;
    private ProjectJpaEntity project1Jpa;
    private ProjectJpaEntity project2Jpa;
    private TagJpaEntity tagJava;
    private TagJpaEntity tagSpring;
    private TagJpaEntity tagJpa;

    // Domain models for testing
    private Question question1Domain;
    private Question question2Domain;
    private Question question3Domain;

    @BeforeEach
    void setUp() {
        clearRepositories();

        // 1. Create Users
        User workspaceOwnerDomain = User.create("Workspace Owner", "ws.owner@example.com", UserRole.ADMIN);
        UserJpaEntity workspaceOwnerJpa = UserJpaEntity.fromModel(workspaceOwnerDomain);
        entityPersist(workspaceOwnerJpa);

        User authorDomain = User.create("Author User", "author@example.com", UserRole.MEMBER);
        authorUserJpa = UserJpaEntity.fromModel(authorDomain);
        entityPersist(authorUserJpa);

        // 2. Create Workspace
        Workspace wsDomain = Workspace.create("Test Workspace", "WS Desc", false, workspaceOwnerJpa.getId());
        WorkspaceJpaEntity workspaceJpa = WorkspaceJpaEntity.fromModel(wsDomain);
        entityPersist(workspaceJpa);

        // 3. Create Projects
        Project p1 = Project.create("Project Alpha", "Desc", workspaceJpa.getId());
        project1Jpa = ProjectJpaEntity.fromModel(p1);
        entityPersist(project1Jpa);

        Project p2 = Project.create("Project Beta", "Desc", workspaceJpa.getId());
        project2Jpa = ProjectJpaEntity.fromModel(p2);
        entityPersist(project2Jpa);

        // 4. Create Tags
        tagJava = TagJpaEntity.fromModel(Tag.create("java", "#FF0000", "Programming"));
        entityPersist(tagJava);
        tagSpring = TagJpaEntity.fromModel(Tag.create("spring", "#00FF00", "Frameworks"));
        entityPersist(tagSpring);
        tagJpa = TagJpaEntity.fromModel(Tag.create("jpa", "#0000FF", "Databases"));
        entityPersist(tagJpa);

        // 5. Create Question Domain Models
        // Question 1: OPEN, Project 1, Tags: java, spring
        question1Domain = Question.create("How to test JPA ManyToMany?", "Desc 1", project1Jpa.getId(), authorUserJpa.getId());
        question1Domain.addTag(tagJava.getId());
        question1Domain.addTag(tagSpring.getId());

        // Question 2: OPEN, Project 1, Tags: spring, jpa
        question2Domain = Question.create("Best practices for Spring Boot?", "Desc 2", project1Jpa.getId(), authorUserJpa.getId());
        question2Domain.addTag(tagSpring.getId());
        question2Domain.addTag(tagJpa.getId());

        // Question 3: RESOLVED, Project 2, Tags: java, jpa
        question3Domain = Question.create("Understanding JPA Fetch Types", "Desc 3", project2Jpa.getId(), authorUserJpa.getId());
        question3Domain.addTag(tagJava.getId());
        question3Domain.addTag(tagJpa.getId());
        question3Domain.changeStatus(QuestionStatus.RESOLVED);
    }

    // --- Basic CRUD, findById, existsById tests are correct and remain unchanged ---
    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a question")
        void create_shouldSaveQuestion() {
            assertDoesNotThrow(() -> create(question1Domain));

            QuestionJpaEntity foundInDb = entityManager.find(QuestionJpaEntity.class, question1Domain.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getTitle()).isEqualTo(question1Domain.getTitle());
            assertThat(foundInDb.getAuthor().getId()).isEqualTo(question1Domain.getAuthorId());
            assertThat(foundInDb.getTags().stream().map(TagJpaEntity::getId).collect(Collectors.toSet()))
                    .hasSameElementsAs(question1Domain.getTagsId());
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing question")
        void update_shouldModifyExistingQuestion() {
            create(question1Domain);

            Question updatedDomainQuestion = Question.build(
                    question1Domain.getId(),
                    question1Domain.getCreatedAt(),
                    Instant.now(),
                    "Updated Title",
                    "Updated description",
                    Set.of(tagJpa.getId()), // Change tags
                    project2Jpa.getId(), // Change project
                    authorUserJpa.getId(),
                    QuestionStatus.CLOSED // Change status
            );

            assertDoesNotThrow(() -> update(updatedDomainQuestion));

            Question foundQuestion = questionPersistence.findById(question1Domain.getId()).orElseThrow();
            assertThat(foundQuestion.getTitle()).isEqualTo("Updated Title");
            assertThat(foundQuestion.getTagsId()).containsExactly(tagJpa.getId());
            assertThat(foundQuestion.getProjectId()).isEqualTo(project2Jpa.getId());
            assertThat(foundQuestion.getStatus()).isEqualTo(QuestionStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("findAllByProjectId Method Tests")
    class FindAllByProjectIdTests {
        @BeforeEach
        void setUpFindAllByProjectId() {
            create(question1Domain); // project1
            create(question2Domain); // project1
            create(question3Domain); // project2
        }

        @Test
        @DisplayName("should return questions for a specific project ID")
        void findAllByProjectId_shouldReturnMatchingQuestions() {
            Page domainPage = Page.of(0, 10);
            Pagination<Question> result = questionPersistence.findAllByProjectId(domainPage, project1Jpa.getId());

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.items()).extracting(Question::getTitle)
                    .containsExactlyInAnyOrder("How to test JPA ManyToMany?", "Best practices for Spring Boot?");
        }
    }

    @Nested
    @DisplayName("findAll Method Tests (Generic Search)")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            create(question1Domain); // OPEN, Project 1, Tags: java, spring
            create(question2Domain); // OPEN, Project 1, Tags: spring, jpa
            create(question3Domain); // RESOLVED, Project 2, Tags: java, jpa
        }

        @Test
        @DisplayName("should return all questions when no search terms provided")
        void findAll_noTerms_shouldReturnAllQuestions() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of());
            Pagination<Question> result = questionPersistence.findAll(query);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by a single term (e.g., status)")
        void findAll_singleTerm_shouldReturnMatching() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("status", "RESOLVED"));
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.items().get(0).getId()).isEqualTo(question3Domain.getId());
        }

        @Test
        @DisplayName("should filter by a joined term (e.g., tagsName)")
        void findAll_byJoinedTerm_shouldReturnMatching() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("tagsName", "spring"));
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.items()).extracting(Question::getId)
                    .containsExactlyInAnyOrder(question1Domain.getId(), question2Domain.getId());
        }

        @Test
        @DisplayName("should filter by multiple terms using AND logic")
        void findAll_withMultipleTerms_shouldReturnAndedResults() {
            // Arrange: Search for questions that are OPEN and have the 'java' tag
            SearchQuery queryWithMatch = SearchQuery.of(Page.of(0, 10), Map.of(
                    "status", "OPEN",
                    "tagsName", "java"
            ));

            // Act
            Pagination<Question> resultWithMatch = questionPersistence.findAll(queryWithMatch);

            // Assert: Should find exactly one question: question1
            assertThat(resultWithMatch.total()).isEqualTo(1);
            assertThat(resultWithMatch.items().get(0).getId()).isEqualTo(question1Domain.getId());

            // Arrange: Search for questions that are RESOLVED and have the 'spring' tag (should be none)
            SearchQuery queryWithoutMatch = SearchQuery.of(Page.of(0, 10), Map.of(
                    "status", "RESOLVED",
                    "tagsName", "spring"
            ));

            // Act
            Pagination<Question> resultWithoutMatch = questionPersistence.findAll(queryWithoutMatch);

            // Assert: Should find no questions
            assertThat(resultWithoutMatch.total()).isZero();
            assertThat(resultWithoutMatch.items()).isEmpty();
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("invalidField", "value"));

            assertThatThrownBy(() -> questionPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should respect pagination and sorting parameters")
        void findAll_withPaginationAndSorting_shouldReturnCorrectPage() {
            SearchQuery queryPage1 = SearchQuery.of(Page.of(0, 2, "title", "asc"), Map.of());
            Pagination<Question> result1 = questionPersistence.findAll(queryPage1);

            assertThat(result1.total()).isEqualTo(3);
            assertThat(result1.items()).hasSize(2);
            assertThat(result1.items().get(0).getTitle()).isEqualTo("Best practices for Spring Boot?");
            assertThat(result1.items().get(1).getTitle()).isEqualTo("How to test JPA ManyToMany?");

            SearchQuery queryPage2 = SearchQuery.of(Page.of(1, 2, "title", "asc"), Map.of());
            Pagination<Question> result2 = questionPersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
            assertThat(result2.items().get(0).getTitle()).isEqualTo("Understanding JPA Fetch Types");
        }
    }

    // Helper methods
    private void create(Question entity) {
        questionPersistence.create(entity);
        flushAndClear();
    }

    private void update(Question entity) {
        questionPersistence.update(entity);
        flushAndClear();
    }

    private void deleteById(String id) {
        questionPersistence.deleteById(id);
        flushAndClear();
    }
}
