package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.*;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QuestionJpaRepositoryTest extends AbstractRepositoryTest {

    private UserJpaEntity authorUser;
    private ProjectJpaEntity project1;
    private ProjectJpaEntity project2;
    private TagJpaEntity tag1;
    private TagJpaEntity tag3;

    private QuestionJpaEntity question1;
    private QuestionJpaEntity question2;
    private QuestionJpaEntity question3;

    @BeforeEach
    void setUp() {

        clearRepositories(); // Clear previous data to ensure a clean state

        // 1. Create Users
        UserJpaEntity workspaceOwner = new UserJpaEntity(UUID.randomUUID().toString());
        workspaceOwner.setName("Workspace Owner");
        workspaceOwner.setEmail("ws.owner@example.com");
        workspaceOwner.setCreatedAt(Instant.now());
        workspaceOwner.setUpdatedAt(Instant.now());
        workspaceOwner.setRole(UserRole.ADMIN);
        entityManager.persist(workspaceOwner);

        authorUser = new UserJpaEntity(UUID.randomUUID().toString());
        authorUser.setName("Author User");
        authorUser.setEmail("author@example.com");
        authorUser.setCreatedAt(Instant.now());
        authorUser.setUpdatedAt(Instant.now());
        authorUser.setRole(UserRole.MEMBER);
        entityManager.persist(authorUser);

        // 2. Create Workspace
        WorkspaceJpaEntity workspace = new WorkspaceJpaEntity(UUID.randomUUID().toString());
        workspace.setName("Test Workspace for Questions");
        workspace.setOwner(workspaceOwner); // Set managed owner
        workspace.setCreatedAt(Instant.now());
        workspace.setUpdatedAt(Instant.now());
        entityManager.persist(workspace);

        // 3. Create Projects
        project1 = new ProjectJpaEntity(UUID.randomUUID().toString());
        project1.setName("Project Alpha for Questions");
        project1.setWorkspace(workspace); // Set managed workspace
        project1.setCreatedAt(Instant.now());
        project1.setUpdatedAt(Instant.now());
        entityManager.persist(project1);

        project2 = new ProjectJpaEntity(UUID.randomUUID().toString());
        project2.setName("Project Beta for Questions");
        project2.setWorkspace(workspace);
        project2.setCreatedAt(Instant.now());
        project2.setUpdatedAt(Instant.now());
        entityManager.persist(project2);

        // 4. Create Tags
        tag1 = new TagJpaEntity(UUID.randomUUID().toString(), "java");
        tag1.setColor("#FF0000"); // Example color
        entityManager.persist(tag1);

        TagJpaEntity tag2 = new TagJpaEntity(UUID.randomUUID().toString(), "spring");
        tag2.setColor("#00FF00"); // Example color
        entityManager.persist(tag2);

        tag3 = new TagJpaEntity(UUID.randomUUID().toString(), "jpa");
        tag3.setColor("#0000FF"); // Example color
        entityManager.persist(tag3);

        entityManager.flush(); // Ensure all prerequisites are in DB

        // 5. Create Question Entities
        // For repository tests, it's often clearer to construct JPA entities directly
        // and set managed related entities, rather than relying on fromModel
        // if fromModel creates transient related entities.

        question1 = new QuestionJpaEntity(UUID.randomUUID().toString());
        question1.setTitle("How to test JPA ManyToMany?");
        question1.setDescription("A detailed description of the problem for question 1.");
        question1.setStatus(QuestionStatus.OPEN);
        question1.setAuthor(authorUser); // Set managed author
        question1.setProject(project1);  // Set managed project
        question1.setTags(Set.of(tag1, tag2)); // Set managed tags
        question1.setCreatedAt(Instant.now());
        question1.setUpdatedAt(Instant.now()); // Set updatedAt, if needed

        question2 = new QuestionJpaEntity(UUID.randomUUID().toString());
        question2.setTitle("Best practices for Spring Boot?");
        question2.setDescription("Looking for best practices in Spring Boot applications.");
        question2.setStatus(QuestionStatus.OPEN);
        question2.setAuthor(authorUser);
        question2.setProject(project1); // Same project as question1
        question2.setTags(Set.of(tag2, tag3));
        question2.setCreatedAt(Instant.now());
        question2.setUpdatedAt(Instant.now());

        question3 = new QuestionJpaEntity(UUID.randomUUID().toString());
        question3.setTitle("Understanding JPA Fetch Types");
        question3.setDescription("Deep dive into LAZY and EAGER fetching.");
        question3.setStatus(QuestionStatus.RESOLVED);
        question3.setAuthor(authorUser);
        question3.setProject(project2); // Different project
        question3.setTags(Set.of(tag1, tag3));
        question3.setCreatedAt(Instant.now());
        question3.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("should save a question and find it by id")
    void saveAndFindById() {
        QuestionJpaEntity savedQuestion = questionJpaRepository.save(question1);
        entityManager.flush();
        entityManager.clear();

        Optional<QuestionJpaEntity> foundQuestionOpt = questionJpaRepository.findById(savedQuestion.getId());

        assertThat(foundQuestionOpt).isPresent();
        QuestionJpaEntity foundQuestion = foundQuestionOpt.get();
        assertThat(foundQuestion.getTitle()).isEqualTo(question1.getTitle());
        assertThat(foundQuestion.getAuthor().getId()).isEqualTo(authorUser.getId());
        assertThat(foundQuestion.getProject().getId()).isEqualTo(project1.getId());
        assertThat(foundQuestion.getTags()).hasSize(2);
        assertThat(foundQuestion.getTags().stream().map(TagJpaEntity::getName).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("java", "spring");
        assertThat(foundQuestion.getCreatedAt()).isNotNull();
        assertThat(foundQuestion.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should fail to save question with null title")
    void save_withNullTitle_shouldFail() {
        QuestionJpaEntity invalidQuestion = new QuestionJpaEntity(UUID.randomUUID().toString());
        // invalidQuestion.setTitle(null); // Title is nullable=false
        invalidQuestion.setDescription("Test");
        invalidQuestion.setStatus(QuestionStatus.OPEN);
        invalidQuestion.setAuthor(authorUser);
        invalidQuestion.setProject(project1);

        assertThatThrownBy(() -> {
            questionJpaRepository.save(invalidQuestion);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class); // Or ConstraintViolationException
    }

    @Test
    @DisplayName("should fail to save question with null author")
    void save_withNullAuthor_shouldFail() {
        QuestionJpaEntity invalidQuestion = new QuestionJpaEntity(UUID.randomUUID().toString());
        invalidQuestion.setTitle("Test Title");
        invalidQuestion.setDescription("Test");
        invalidQuestion.setStatus(QuestionStatus.OPEN);
        // invalidQuestion.setAuthor(null); // Author is nullable=false
        invalidQuestion.setProject(project1);

        assertThatThrownBy(() -> {
            questionJpaRepository.save(invalidQuestion);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("should update an existing question")
    void updateQuestion() {
        QuestionJpaEntity persistedQuestion = questionJpaRepository.save(question1);
        entityManager.flush();

        persistedQuestion.setTitle("Updated: How to test JPA ManyToMany?");
        persistedQuestion.setStatus(QuestionStatus.CLOSED);
        persistedQuestion.getTags().remove(tag1); // Remove one tag
        persistedQuestion.getTags().add(tag3);   // Add a new tag

        questionJpaRepository.save(persistedQuestion);
        entityManager.flush();
        entityManager.clear();

        Optional<QuestionJpaEntity> updatedQuestionOpt = questionJpaRepository.findById(persistedQuestion.getId());
        assertThat(updatedQuestionOpt).isPresent();
        QuestionJpaEntity updatedQuestion = updatedQuestionOpt.get();

        assertThat(updatedQuestion.getTitle()).isEqualTo("Updated: How to test JPA ManyToMany?");
        assertThat(updatedQuestion.getStatus()).isEqualTo(QuestionStatus.CLOSED);
        assertThat(updatedQuestion.getTags()).hasSize(2);
        assertThat(updatedQuestion.getTags().stream().map(TagJpaEntity::getName).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("spring", "jpa"); // java removed, jpa added
    }

    @Test
    @DisplayName("should delete a question by id")
    void deleteById() {
        QuestionJpaEntity persistedQuestion = questionJpaRepository.save(question1);
        entityManager.flush();
        String idToDelete = persistedQuestion.getId();

        questionJpaRepository.deleteById(idToDelete);
        entityManager.flush();
        entityManager.clear();

        Optional<QuestionJpaEntity> deletedQuestion = questionJpaRepository.findById(idToDelete);
        assertThat(deletedQuestion).isNotPresent();
    }

    @Test
    @DisplayName("findAllByProject_Id should return questions for a specific project")
    void findAllByProjectId_shouldReturnMatchingQuestions() {
        questionJpaRepository.save(question1); // project1
        questionJpaRepository.save(question2); // project1
        questionJpaRepository.save(question3); // project2
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);
        Page<QuestionJpaEntity> project1QuestionsPage = questionJpaRepository.findAllByProject_Id(project1.getId(), pageable);

        assertThat(project1QuestionsPage.getContent()).hasSize(2);
        assertThat(project1QuestionsPage.getContent()).extracting(QuestionJpaEntity::getTitle)
                .containsExactlyInAnyOrder("How to test JPA ManyToMany?", "Best practices for Spring Boot?");
        assertThat(project1QuestionsPage.getTotalElements()).isEqualTo(2);

        Page<QuestionJpaEntity> project2QuestionsPage = questionJpaRepository.findAllByProject_Id(project2.getId(), pageable);
        assertThat(project2QuestionsPage.getContent()).hasSize(1);
        assertThat(project2QuestionsPage.getContent().get(0).getTitle()).isEqualTo("Understanding JPA Fetch Types");
    }

    @Test
    @DisplayName("findAllByProject_Id should return empty page if no questions for project")
    void findAllByProjectId_noMatches_shouldReturnEmptyPage() {
        questionJpaRepository.save(question3); // project2
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);
        Page<QuestionJpaEntity> project1QuestionsPage = questionJpaRepository.findAllByProject_Id(project1.getId(), pageable); // project1 has no questions yet

        assertThat(project1QuestionsPage.getContent()).isEmpty();
        assertThat(project1QuestionsPage.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findAll with Specification should filter by question title")
    void findAll_withSpecification_byTitle() {
        questionJpaRepository.save(question1);
        questionJpaRepository.save(question2);
        entityManager.flush();

        Specification<QuestionJpaEntity> spec = (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%best practices%");
        Pageable pageable = PageRequest.of(0, 10);

        Page<QuestionJpaEntity> result = questionJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Best practices for Spring Boot?");
    }

    @Test
    @DisplayName("findAll with Specification should filter by question status")
    void findAll_withSpecification_byStatus() {
        questionJpaRepository.save(question1); // OPEN
        questionJpaRepository.save(question3); // RESOLVED
        entityManager.flush();

        Specification<QuestionJpaEntity> spec = (root, query, cb) ->
                cb.equal(root.get("status"), QuestionStatus.RESOLVED);
        Pageable pageable = PageRequest.of(0, 10);

        Page<QuestionJpaEntity> result = questionJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(QuestionStatus.RESOLVED);
    }

    @Test
    @DisplayName("findAll with Specification should filter by author id")
    void findAll_withSpecification_byAuthorId() {
        // Create another author and question for differentiation
        UserJpaEntity anotherAuthor = new UserJpaEntity(UUID.randomUUID().toString());
        anotherAuthor.setName("Another Author");
        anotherAuthor.setEmail("another.author@example.com");
        anotherAuthor.setCreatedAt(Instant.now());
        anotherAuthor.setUpdatedAt(Instant.now());
        anotherAuthor.setRole(UserRole.MEMBER);
        entityManager.persist(anotherAuthor);

        QuestionJpaEntity questionByAnotherAuthor = new QuestionJpaEntity(UUID.randomUUID().toString());
        questionByAnotherAuthor.setTitle("Question by Another");
        questionByAnotherAuthor.setDescription("Desc");
        questionByAnotherAuthor.setStatus(QuestionStatus.OPEN);
        questionByAnotherAuthor.setAuthor(anotherAuthor); // Different author
        questionByAnotherAuthor.setProject(project1);
        questionByAnotherAuthor.setCreatedAt(Instant.now());
        questionByAnotherAuthor.setUpdatedAt(Instant.now());
        questionJpaRepository.save(questionByAnotherAuthor);


        questionJpaRepository.save(question1); // Authored by authorUser
        entityManager.flush();

        Specification<QuestionJpaEntity> spec = (root, query, cb) ->
                cb.equal(root.get("author").get("id"), authorUser.getId());
        Pageable pageable = PageRequest.of(0, 10);

        Page<QuestionJpaEntity> result = questionJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthor().getId()).isEqualTo(authorUser.getId());
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(question1.getTitle());
    }

    @Test
    @DisplayName("findAll with Specification should filter by tag name (requires join)")
    void findAll_withSpecification_byTagName() {
        questionJpaRepository.save(question1); // Tags: java, spring
        questionJpaRepository.save(question2); // Tags: spring, jpa
        questionJpaRepository.save(question3); // Tags: java, jpa
        entityManager.flush();

        Specification<QuestionJpaEntity> spec = (root, query, cb) -> {
            // To avoid duplicate questions if a question has multiple matching tags,
            // but for this specific query (filtering by one tag name), it might not be an issue.
            // If combining multiple tag conditions, distinct might be needed.
            // query.distinct(true);
            return cb.equal(root.join("tags").get("name"), "java");
        };
        Pageable pageable = PageRequest.of(0, 10);
        Page<QuestionJpaEntity> result = questionJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().stream().map(QuestionJpaEntity::getTitle).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(question1.getTitle(), question3.getTitle());
    }
}
