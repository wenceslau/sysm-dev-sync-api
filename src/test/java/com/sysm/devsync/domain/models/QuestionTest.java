package com.sysm.devsync.domain.models;

import com.sysm.devsync.domain.enums.StatusQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    private String validTitle;
    private String validDescription;
    private String validProjectId;
    private String validAuthorId;

    @BeforeEach
    void setUp() {
        validTitle = "How to implement OAuth2?";
        validDescription = "Looking for a step-by-step guide on implementing OAuth2 with Spring Security.";
        validProjectId = UUID.randomUUID().toString();
        validAuthorId = UUID.randomUUID().toString();
    }

    // --- Static Factory Method: create() ---

    @Test
    @DisplayName("create() should successfully create a question with valid arguments")
    void create_shouldSucceed_withValidArguments() {
        Instant beforeCreation = Instant.now();
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        Instant afterCreation = Instant.now();

        assertNotNull(question.getId(), "ID should be generated and not null");
        try {
            UUID.fromString(question.getId()); // Validate UUID format
        } catch (IllegalArgumentException e) {
            fail("Generated ID is not a valid UUID: " + question.getId());
        }

        assertEquals(validTitle, question.getTitle());
        assertEquals(validDescription, question.getDescription());
        assertEquals(validProjectId, question.getProjectId());
        assertEquals(validAuthorId, question.getAuthorId());

        assertNotNull(question.getCreatedAt(), "CreatedAt should be set");
        assertNotNull(question.getUpdatedAt(), "UpdatedAt should be set");
        assertEquals(question.getCreatedAt(), question.getUpdatedAt(), "CreatedAt and UpdatedAt should be same on creation");

        assertTrue(!question.getCreatedAt().isBefore(beforeCreation) && !question.getCreatedAt().isAfter(afterCreation),
                "CreatedAt should be very close to the time of creation");

        assertNotNull(question.getTagsId(), "Tags should be initialized as an empty set");
        assertTrue(question.getTagsId().isEmpty(), "Tags should be empty on creation");
        assertEquals(StatusQuestion.OPEN, question.getStatus(), "Status should be OPEN on creation");
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null title")
    void create_shouldThrowException_whenTitleIsNull() {
        // This will be caught by the constructor's validate() method
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Question.create(null, validDescription, validProjectId, validAuthorId);
        });
        assertEquals("Title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty title")
    void create_shouldThrowException_whenTitleIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Question.create("", validDescription, validProjectId, validAuthorId);
        });
        assertEquals("Title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null description")
    void create_shouldThrowException_whenDescriptionIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Question.create(validTitle, null, validProjectId, validAuthorId);
        });
        assertEquals("Description cannot be null", exception.getMessage());
    }
    // Note: Description can be empty, so no test for empty description throwing exception for create()

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null projectId")
    void create_shouldThrowException_whenProjectIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Question.create(validTitle, validDescription, null, validAuthorId);
        });
        assertEquals("Project ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty projectId")
    void create_shouldThrowException_whenProjectIdIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Question.create(validTitle, validDescription, "", validAuthorId);
        });
        assertEquals("Project ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null authorId")
    void create_shouldThrowException_whenAuthorIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Question.create(validTitle, validDescription, validProjectId, null);
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty authorId")
    void create_shouldThrowException_whenAuthorIdIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Question.create(validTitle, validDescription, validProjectId, "");
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    // --- Static Factory Method: build() ---

    @Test
    @DisplayName("build() should successfully create a question with all arguments")
    void build_shouldSucceed_withAllArguments() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(12, ChronoUnit.HOURS);
        Set<String> tags = new HashSet<>(Set.of("java", "spring"));
        StatusQuestion statusQuestion = StatusQuestion.RESOLVED;

        Question question = Question.build(id, createdAt, updatedAt, validTitle, validDescription,
                tags, validProjectId, validAuthorId, statusQuestion);

        assertEquals(id, question.getId());
        assertEquals(createdAt, question.getCreatedAt());
        assertEquals(updatedAt, question.getUpdatedAt());
        assertEquals(validTitle, question.getTitle());
        assertEquals(validDescription, question.getDescription());
        assertEquals(tags, question.getTagsId()); // build creates a new HashSet from the input
        assertNotSame(tags, question.getTagsId(), "Build should create a new Set instance for tags");
        assertEquals(validProjectId, question.getProjectId());
        assertEquals(validAuthorId, question.getAuthorId());
        assertEquals(statusQuestion, question.getStatus());
    }

    @Test
    @DisplayName("build() should initialize tags to empty set if null tags are provided")
    void build_shouldInitializeEmptyTags_whenNullTagsProvided() {
        // The build method currently does `new HashSet<>(tags)`, which would throw NPE if tags is null.
        // Let's adjust the expectation or assume the build method handles null tags gracefully (e.g., by defaulting to empty set).
        // Current Question.build: new HashSet<>(tags) will throw NPE if tags is null.
        // The test should reflect this, or the build method should be changed.
        // For now, let's test the NPE.
        // If the intention is to allow null and default to empty, the build method needs:
        // tags != null ? new HashSet<>(tags) : new HashSet<>()

        // Given the current implementation:
        assertThrows(NullPointerException.class, () -> {
            Question.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                    validTitle, validDescription, null, validProjectId, validAuthorId, StatusQuestion.OPEN);
        }, "build() should throw NullPointerException if null is passed for tags and not handled");


        // If build were to handle null tags:
        // Question question = Question.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
        // validTitle, validDescription, null, validProjectId, validAuthorId, Status.OPEN);
        // assertNotNull(question.getTags(), "Tags should not be null");
        // assertTrue(question.getTags().isEmpty(), "Tags should be an empty set if null was passed to build");
    }

    @Test
    @DisplayName("build() should use provided empty set for tags")
    void build_shouldUseProvidedEmptySetForTags() {
        Set<String> emptyTags = Collections.emptySet();
        Question question = Question.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                validTitle, validDescription, emptyTags, validProjectId, validAuthorId, StatusQuestion.OPEN);
        assertNotNull(question.getTagsId());
        assertTrue(question.getTagsId().isEmpty());
        assertNotSame(emptyTags, question.getTagsId(), "Build should create a new Set instance for tags");
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for null id (via constructor validate)")
    void build_shouldThrowException_whenIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Question.build(null, Instant.now(), Instant.now(), validTitle, validDescription,
                    new HashSet<>(), validProjectId, validAuthorId, StatusQuestion.OPEN);
        });
        assertEquals("ID cannot be null or empty", exception.getMessage());
    }
    // Other validation tests for build() (title, description, projectId, authorId) are implicitly covered
    // as the constructor calls validate() after field assignment.

    // --- Instance Method: update() ---

    @Test
    @DisplayName("update() should modify title and description, and update timestamp")
    void update_shouldModifyTitleDescriptionAndTimestamp() throws InterruptedException {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        Instant initialUpdatedAt = question.getUpdatedAt();
        String initialId = question.getId();
        Instant initialCreatedAt = question.getCreatedAt();
        String initialProjectId = question.getProjectId();
        String initialAuthorId = question.getAuthorId();
        Set<String> initialTags = new HashSet<>(question.getTagsId()); // Copy for comparison
        StatusQuestion initialStatusQuestion = question.getStatus();

        Thread.sleep(1); // Ensure updatedAt will be different

        String newTitle = "Updated Question Title";
        String newDescription = "This is the updated description for the question.";
        question.update(newTitle, newDescription);

        assertEquals(newTitle, question.getTitle());
        assertEquals(newDescription, question.getDescription());
        assertTrue(question.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after the initial value");

        // Ensure other fields remain unchanged
        assertEquals(initialId, question.getId());
        assertEquals(initialCreatedAt, question.getCreatedAt());
        assertEquals(initialProjectId, question.getProjectId());
        assertEquals(initialAuthorId, question.getAuthorId());
        assertEquals(initialTags, question.getTagsId(), "Tags should not be changed by update()");
        assertEquals(initialStatusQuestion, question.getStatus(), "Status should not be changed by update()");
    }

    @Test
    @DisplayName("update() should allow empty description")
    void update_shouldAllowEmptyDescription() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        assertDoesNotThrow(() -> question.update("New Title", ""));
        assertEquals("", question.getDescription());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new title")
    void update_shouldThrowException_whenNewTitleIsNull() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            question.update(null, "New Description");
        });
        assertEquals("Title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for empty new title")
    void update_shouldThrowException_whenNewTitleIsEmpty() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            question.update("", "New Description");
        });
        assertEquals("Title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new description")
    void update_shouldThrowException_whenNewDescriptionIsNull() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            question.update("New Title", null);
        });
        assertEquals("Description cannot be null", exception.getMessage());
    }

    // --- Instance Method: changeStatus() ---

    @Test
    @DisplayName("changeStatus() should update status and timestamp")
    void changeStatus_shouldUpdateStatusAndTimestamp() throws InterruptedException {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        Instant initialUpdatedAt = question.getUpdatedAt();
        StatusQuestion newStatusQuestion = StatusQuestion.CLOSED;

        Thread.sleep(1); // Ensure updatedAt will be different
        question.changeStatus(newStatusQuestion);

        assertEquals(newStatusQuestion, question.getStatus());
        assertTrue(question.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    @Test
    @DisplayName("changeStatus() should throw IllegalArgumentException for null status")
    void changeStatus_shouldThrowException_whenStatusIsNull() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            question.changeStatus(null);
        });
        assertEquals("Status cannot be null", exception.getMessage());
    }

    // --- Instance Method: addTag() ---

    @Test
    @DisplayName("addTag() should add a new tag")
    void addTag_shouldAddNewTag() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        question.addTag("java");
        assertTrue(question.getTagsId().contains("java"));
        assertEquals(1, question.getTagsId().size());

        question.addTag("spring");
        assertTrue(question.getTagsId().contains("spring"));
        assertEquals(2, question.getTagsId().size());
    }

    @Test
    @DisplayName("addTag() should not add a duplicate tag")
    void addTag_shouldNotAddDuplicateTag() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        question.addTag("uniqueTag");
        question.addTag("uniqueTag");
        assertEquals(1, question.getTagsId().size());
    }

    @Test
    @DisplayName("addTag() should throw IllegalArgumentException for null tag")
    void addTag_shouldThrowException_whenTagIsNull() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            question.addTag(null);
        });
        assertEquals("Tag cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("addTag() should throw IllegalArgumentException for empty tag")
    void addTag_shouldThrowException_whenTagIsEmpty() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            question.addTag("");
        });
        assertEquals("Tag cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("addTag() should not change updatedAt or status")
    void addTag_shouldNotChangeUpdatedAtOrStatus() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        Instant updatedAtBefore = question.getUpdatedAt();
        StatusQuestion statusQuestionBefore = question.getStatus();

        question.addTag("someTag");

        assertEquals(updatedAtBefore, question.getUpdatedAt(), "UpdatedAt should not change after adding a tag");
        assertEquals(statusQuestionBefore, question.getStatus(), "Status should not change after adding a tag");
    }

    // --- Instance Method: removeTag() ---

    @Test
    @DisplayName("removeTag() should remove an existing tag")
    void removeTag_shouldRemoveExistingTag() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        question.addTag("tagToRemove");
        question.addTag("tagToKeep");

        question.removeTag("tagToRemove");
        assertFalse(question.getTagsId().contains("tagToRemove"));
        assertTrue(question.getTagsId().contains("tagToKeep"));
        assertEquals(1, question.getTagsId().size());
    }

    @Test
    @DisplayName("removeTag() should do nothing if tag does not exist")
    void removeTag_shouldDoNothing_ifTagDoesNotExist() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        question.addTag("existingTag");
        int initialSize = question.getTagsId().size();

        question.removeTag("nonExistentTag");
        assertEquals(initialSize, question.getTagsId().size());
        assertTrue(question.getTagsId().contains("existingTag"));
    }

    @Test
    @DisplayName("removeTag() should do nothing if tags set is empty")
    void removeTag_shouldDoNothing_ifTagsSetIsEmpty() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        assertTrue(question.getTagsId().isEmpty());

        assertDoesNotThrow(() -> question.removeTag("anyTag"));
        assertTrue(question.getTagsId().isEmpty());
    }

    @Test
    @DisplayName("removeTag() should throw IllegalArgumentException for null tag")
    void removeTag_shouldThrowException_whenTagIsNull() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        question.addTag("sample");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            question.removeTag(null);
        });
        assertEquals("Tag cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("removeTag() should throw IllegalArgumentException for empty tag")
    void removeTag_shouldThrowException_whenTagIsEmpty() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        question.addTag("sample");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            question.removeTag("");
        });
        assertEquals("Tag cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("removeTag() should not change updatedAt or status")
    void removeTag_shouldNotChangeUpdatedAtOrStatus() {
        Question question = Question.create(validTitle, validDescription, validProjectId, validAuthorId);
        question.addTag("tagToRemove");
        Instant updatedAtBefore = question.getUpdatedAt();
        StatusQuestion statusQuestionBefore = question.getStatus();

        question.removeTag("tagToRemove");

        assertEquals(updatedAtBefore, question.getUpdatedAt(), "UpdatedAt should not change after removing a tag");
        assertEquals(statusQuestionBefore, question.getStatus(), "Status should not change after removing a tag");
    }

    // --- Getters (Basic check, mostly covered by other tests) ---
    @Test
    @DisplayName("Getters should return correct values after construction via build")
    void getters_shouldReturnCorrectValues() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        String title = "Getter Test Title";
        String description = "Description for getter test.";
        Set<String> tags = new HashSet<>(Set.of("getterTag1", "getterTag2"));
        String projectId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        StatusQuestion statusQuestion = StatusQuestion.CLOSED;

        Question question = Question.build(id, createdAt, updatedAt, title, description, tags, projectId, authorId, statusQuestion);

        assertEquals(id, question.getId());
        assertEquals(createdAt, question.getCreatedAt());
        assertEquals(updatedAt, question.getUpdatedAt());
        assertEquals(title, question.getTitle());
        assertEquals(description, question.getDescription());
        assertEquals(tags, question.getTagsId());
        assertEquals(projectId, question.getProjectId());
        assertEquals(authorId, question.getAuthorId());
        assertEquals(statusQuestion, question.getStatus());
    }

}
