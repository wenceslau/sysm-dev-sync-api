package com.sysm.devsync.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AnswerTest {

    private String validQuestionId;
    private String validAuthorId;
    private String validContent;

    @BeforeEach
    void setUp() {
        validQuestionId = UUID.randomUUID().toString();
        validAuthorId = UUID.randomUUID().toString();
        validContent = "This is a valid answer content.";
    }

    // --- Static Factory Method: create() ---

    @Test
    @DisplayName("create() should successfully create an answer with valid arguments")
    void create_shouldSucceed_withValidArguments() {
        Instant beforeCreation = Instant.now();
        Answer answer = Answer.create(validQuestionId, validAuthorId, validContent);
        Instant afterCreation = Instant.now();

        assertNotNull(answer.getId(), "ID should be generated and not null");
        try {
            UUID.fromString(answer.getId()); // Validate UUID format
        } catch (IllegalArgumentException e) {
            fail("Generated ID is not a valid UUID: " + answer.getId());
        }

        assertEquals(validQuestionId, answer.getQuestionId());
        assertEquals(validAuthorId, answer.getAuthorId());
        assertEquals(validContent, answer.getContent());

        assertNotNull(answer.getCreatedAt(), "CreatedAt should be set");
        assertNotNull(answer.getUpdatedAt(), "UpdatedAt should be set");
        assertEquals(answer.getCreatedAt(), answer.getUpdatedAt(), "CreatedAt and UpdatedAt should be same on creation");

        assertTrue(!answer.getCreatedAt().isBefore(beforeCreation) && !answer.getCreatedAt().isAfter(afterCreation),
                "CreatedAt should be very close to the time of creation");

        assertFalse(answer.isAccepted(), "isAccepted should be false on creation");
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null questionId")
    void create_shouldThrowException_whenQuestionIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Answer.create(null, validAuthorId, validContent);
        });
        assertEquals("Question ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty questionId")
    void create_shouldThrowException_whenQuestionIdIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Answer.create("", validAuthorId, validContent);
        });
        assertEquals("Question ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null authorId")
    void create_shouldThrowException_whenAuthorIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Answer.create(validQuestionId, null, validContent);
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty authorId")
    void create_shouldThrowException_whenAuthorIdIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Answer.create(validQuestionId, "", validContent);
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null content")
    void create_shouldThrowException_whenContentIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Answer.create(validQuestionId, validAuthorId, null);
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty content")
    void create_shouldThrowException_whenContentIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Answer.create(validQuestionId, validAuthorId, "");
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }

    // --- Static Factory Method: build() ---

    @Test
    @DisplayName("build() should successfully create an answer with all arguments")
    void build_shouldSucceed_withAllArguments() {
        // Given valid arguments
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(12, ChronoUnit.HOURS);
        boolean isAccepted = true;

        // When building the answer
        Answer answer = Answer.build(id, validQuestionId, validAuthorId, createdAt, validContent, isAccepted, updatedAt);

        // Then all fields should be set correctly
        assertEquals(id, answer.getId());
        assertEquals(validQuestionId, answer.getQuestionId());
        assertEquals(validAuthorId, answer.getAuthorId());
        assertEquals(createdAt, answer.getCreatedAt());
        assertEquals(validContent, answer.getContent());
        assertEquals(isAccepted, answer.isAccepted());
        assertEquals(updatedAt, answer.getUpdatedAt());
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for null id (via constructor validate)")
    void build_shouldThrowException_whenIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Answer.build(null, validQuestionId, validAuthorId, Instant.now(), validContent, false, Instant.now());
        });
        assertEquals("ID cannot be null or empty", exception.getMessage());
    }
    // Other validation tests for build() (questionId, authorId, content) are implicitly covered
    // as the constructor calls validate() after field assignment.

    // --- Instance Method: update() ---

    @Test
    @DisplayName("update() should modify content and update timestamp")
    void update_shouldModifyContentAndTimestamp() throws InterruptedException {
        Answer answer = Answer.create(validQuestionId, validAuthorId, "Initial content");
        Instant initialUpdatedAt = answer.getUpdatedAt();
        String initialId = answer.getId();
        Instant initialCreatedAt = answer.getCreatedAt();
        String initialQuestionId = answer.getQuestionId();
        String initialAuthorId = answer.getAuthorId();
        boolean initialIsAccepted = answer.isAccepted();

        Thread.sleep(1); // Ensure updatedAt will be different

        String newContent = "Updated answer content.";
        answer.update(newContent);

        assertEquals(newContent, answer.getContent());
        assertTrue(answer.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after the initial value");

        // Ensure other fields remain unchanged
        assertEquals(initialId, answer.getId());
        assertEquals(initialCreatedAt, answer.getCreatedAt());
        assertEquals(initialQuestionId, answer.getQuestionId());
        assertEquals(initialAuthorId, answer.getAuthorId());
        assertEquals(initialIsAccepted, answer.isAccepted(), "isAccepted should not be changed by update()");
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new content")
    void update_shouldThrowException_whenNewContentIsNull() {
        Answer answer = Answer.create(validQuestionId, validAuthorId, validContent);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            answer.update(null);
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for empty new content")
    void update_shouldThrowException_whenNewContentIsEmpty() {
        Answer answer = Answer.create(validQuestionId, validAuthorId, validContent);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            answer.update("");
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }

    // --- Instance Method: accept() ---

    @Test
    @DisplayName("accept() should set isAccepted to true and update timestamp")
    void accept_shouldSetIsAcceptedTrueAndUpdateTimestamp() throws InterruptedException {
        Answer answer = Answer.create(validQuestionId, validAuthorId, validContent); // isAccepted is false initially
        assertFalse(answer.isAccepted());
        Instant initialUpdatedAt = answer.getUpdatedAt();

        Thread.sleep(1);
        answer.accept();

        assertTrue(answer.isAccepted(), "isAccepted should be true after accept()");
        assertTrue(answer.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after accept()");

        // Ensure other fields like content are not changed
        assertEquals(validContent, answer.getContent());
    }

    @Test
    @DisplayName("accept() should keep isAccepted true if already true and update timestamp")
    void accept_shouldKeepIsAcceptedTrueAndUpdateTimestamp() throws InterruptedException {
        Answer answer = Answer.build(UUID.randomUUID().toString(), validQuestionId, validAuthorId,
                Instant.now(), validContent, true, Instant.now()); // isAccepted is true initially
        assertTrue(answer.isAccepted());
        Instant initialUpdatedAt = answer.getUpdatedAt();

        Thread.sleep(1);
        answer.accept();

        assertTrue(answer.isAccepted(), "isAccepted should remain true after accept()");
        assertTrue(answer.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after accept() even if state didn't change");
    }

    // --- Instance Method: reject() ---

    @Test
    @DisplayName("reject() should set isAccepted to false and update timestamp")
    void reject_shouldSetIsAcceptedFalseAndUpdateTimestamp() throws InterruptedException {
        Answer answer = Answer.build(UUID.randomUUID().toString(), validQuestionId, validAuthorId,
                Instant.now(), validContent, true, Instant.now()); // isAccepted is true initially
        assertTrue(answer.isAccepted());
        Instant initialUpdatedAt = answer.getUpdatedAt();

        Thread.sleep(1);
        answer.reject();

        assertFalse(answer.isAccepted(), "isAccepted should be false after reject()");
        assertTrue(answer.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after reject()");

        // Ensure other fields like content are not changed
        assertEquals(validContent, answer.getContent());
    }

    @Test
    @DisplayName("reject() should keep isAccepted false if already false and update timestamp")
    void reject_shouldKeepIsAcceptedFalseAndUpdateTimestamp() throws InterruptedException {
        Answer answer = Answer.create(validQuestionId, validAuthorId, validContent); // isAccepted is false initially
        assertFalse(answer.isAccepted());
        Instant initialUpdatedAt = answer.getUpdatedAt();

        Thread.sleep(1);
        answer.reject();

        assertFalse(answer.isAccepted(), "isAccepted should remain false after reject()");
        assertTrue(answer.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after reject() even if state didn't change");
    }


    // --- Getters (Basic check, mostly covered by other tests) ---
    @Test
    @DisplayName("Getters should return correct values after construction via build")
    void getters_shouldReturnCorrectValues() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        String questionId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        String content = "Content for getter test.";
        boolean isAccepted = true;

        Answer answer = Answer.build(id, questionId, authorId, createdAt, content, isAccepted, updatedAt);

        assertEquals(id, answer.getId());
        assertEquals(questionId, answer.getQuestionId());
        assertEquals(authorId, answer.getAuthorId());
        assertEquals(createdAt, answer.getCreatedAt());
        assertEquals(content, answer.getContent());
        assertEquals(isAccepted, answer.isAccepted());
        assertEquals(updatedAt, answer.getUpdatedAt());
    }

}
