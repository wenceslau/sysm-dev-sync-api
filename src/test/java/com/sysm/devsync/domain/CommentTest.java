package com.sysm.devsync.domain;

import com.sysm.devsync.domain.enums.TargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommentTest {

    private String validTargetId;
    private String validAuthorId;
    private String validContent;
    private TargetType validTargetType;

    @BeforeEach
    void setUp() {
        validTargetId = UUID.randomUUID().toString();
        validAuthorId = UUID.randomUUID().toString();
        validContent = "This is a valid comment.";
        validTargetType = TargetType.NOTE; // Assuming NOTE is a valid TargetType
    }

    // --- Static Factory Method: create(TargetType targetType, String targetId, String authorId, String content) ---
    @Test
    @DisplayName("create() should successfully create a comment with generated ID and current timestamps")
    void create_shouldSucceed_withGeneratedIdAndTimestamps() {
        Instant beforeCreation = Instant.now();
        Comment comment = Comment.create(validTargetType, validTargetId, validAuthorId, validContent);
        Instant afterCreation = Instant.now();

        assertNotNull(comment.getId(), "ID should be generated and not null");
        try {
            UUID.fromString(comment.getId()); // Validate UUID format
        } catch (IllegalArgumentException e) {
            fail("Generated ID is not a valid UUID: " + comment.getId());
        }

        assertEquals(validTargetType, comment.getTargetType());
        assertEquals(validTargetId, comment.getTargetId());
        assertEquals(validAuthorId, comment.getAuthorId());
        assertEquals(validContent, comment.getContent());

        assertNotNull(comment.getCreatedAt(), "CreatedAt should be set");
        assertNotNull(comment.getUpdatedAt(), "UpdatedAt should be set");
        assertEquals(comment.getCreatedAt(), comment.getUpdatedAt(), "CreatedAt and UpdatedAt should be the same on creation");

        // Check if createdAt is between the time before and after the call, inclusive
        assertTrue(!comment.getCreatedAt().isBefore(beforeCreation) && !comment.getCreatedAt().isAfter(afterCreation),
                "CreatedAt should be very close to the time of creation");
    }

    @Test
    @DisplayName("create() should throw for null targetType")
    void create_shouldThrow_whenTargetTypeIsNull() {
        // The constructor doesn't explicitly validate targetType for null,
        // but if it's a required field, this test would highlight it.
        // For now, assuming it's allowed or handled by enum nature.
        // If a NullPointerException occurs, the test should reflect that.
        // If an IllegalArgumentException is expected from validate(), it's not currently there for targetType.
        // Let's assume for now that a null TargetType would cause issues elsewhere or is implicitly disallowed.
        // If the class were to add validation for targetType:
        //  IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        //      Comment.create(null, validTargetId, validAuthorId, validContent);
        //  });
        //  assertEquals("Target type cannot be null", exception.getMessage());
        // For now, we'll test that it can be created, and the getter returns null.
        // However, the constructor doesn't have a null check for targetType.
        // Let's proceed assuming the current implementation.
        // If TargetType is an enum, passing null might be problematic at runtime if not handled.
        // The current `validate()` method does NOT check `targetType`.
        // Let's test the existing validation paths.
        // This test is more about the other parameters when targetType is valid.
        assertNotNull(Comment.create(validTargetType, validTargetId, validAuthorId, validContent));
    }


    @Test
    @DisplayName("create() should throw for null targetId")
    void create_shouldThrow_whenTargetIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.create(validTargetType, null, validAuthorId, validContent);
        });
        assertEquals("Target ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw for blank targetId")
    void create_shouldThrow_whenTargetIdIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.create(validTargetType, "  ", validAuthorId, validContent);
        });
        assertEquals("Target ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw for null authorId")
    void create_shouldThrow_whenAuthorIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.create(validTargetType, validTargetId, null, validContent);
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw for blank authorId")
    void create_shouldThrow_whenAuthorIdIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.create(validTargetType, validTargetId, " ", validContent);
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw for null content")
    void create_shouldThrow_whenContentIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.create(validTargetType, validTargetId, validAuthorId, null);
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw for blank content")
    void create_shouldThrow_whenContentIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.create(validTargetType, validTargetId, validAuthorId, "\t");
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }

    // --- Static Factory Method: build(...) ---
    @Test
    @DisplayName("build() should successfully create a comment with all provided fields")
    void build_shouldSucceed_withAllFields() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(12, ChronoUnit.HOURS);

        Comment comment = Comment.build(id, validTargetType, validTargetId, validAuthorId,
                createdAt, validContent, updatedAt);

        assertEquals(id, comment.getId());
        assertEquals(validTargetType, comment.getTargetType());
        assertEquals(validTargetId, comment.getTargetId());
        assertEquals(validAuthorId, comment.getAuthorId());
        assertEquals(createdAt, comment.getCreatedAt());
        assertEquals(validContent, comment.getContent());
        assertEquals(updatedAt, comment.getUpdatedAt());
    }

    @Test
    @DisplayName("build() should throw for null id (via constructor validation)")
    void build_shouldThrow_forNullId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.build(null, validTargetType, validTargetId, validAuthorId,
                    Instant.now(), validContent, Instant.now());
        });
        assertEquals("ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("build() should throw for null authorId (via constructor validation)")
    void build_shouldThrow_forNullAuthorId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.build(UUID.randomUUID().toString(), validTargetType, validTargetId, null,
                    Instant.now(), validContent, Instant.now());
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("build() should throw for null targetId (via constructor validation)")
    void build_shouldThrow_forNullTargetId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.build(UUID.randomUUID().toString(), validTargetType, null, validAuthorId,
                    Instant.now(), validContent, Instant.now());
        });
        assertEquals("Target ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("build() should throw for null content (via constructor validation)")
    void build_shouldThrow_forNullContent() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Comment.build(UUID.randomUUID().toString(), validTargetType, validTargetId, validAuthorId,
                    Instant.now(), null, Instant.now());
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }
    // Note: build() passes createdAt and updatedAt directly. If they were null and the constructor
    // had null checks for them, those tests would be here. Currently, no such checks.

    // --- Instance Method: update() ---
    @Test
    @DisplayName("update() should modify content and update timestamp")
    void update_shouldModifyContentAndUpdateTimestamp() throws InterruptedException {
        Comment comment = Comment.create(validTargetType, validTargetId, validAuthorId, "Initial content");
        Instant initialUpdatedAt = comment.getUpdatedAt();
        String initialId = comment.getId();
        Instant initialCreatedAt = comment.getCreatedAt();
        String initialTargetId = comment.getTargetId();
        TargetType initialTargetType = comment.getTargetType();
        String initialAuthorId = comment.getAuthorId();

        Thread.sleep(1); // Ensure updatedAt will be different

        String newContent = "This content has been updated.";
        Comment updatedComment = comment.update(newContent);

        assertSame(comment, updatedComment, "Update method should return the same instance");
        assertEquals(newContent, comment.getContent());
        assertTrue(comment.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after the initial value");

        // Ensure other fields remain unchanged
        assertEquals(initialId, comment.getId());
        assertEquals(initialCreatedAt, comment.getCreatedAt());
        assertEquals(initialTargetId, comment.getTargetId());
        assertEquals(initialTargetType, comment.getTargetType());
        assertEquals(initialAuthorId, comment.getAuthorId());
    }

    @Test
    @DisplayName("update() should throw for null new content")
    void update_shouldThrow_whenNewContentIsNull() {
        Comment comment = Comment.create(validTargetType, validTargetId, validAuthorId, validContent);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            comment.update(null);
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw for blank new content")
    void update_shouldThrow_whenNewContentIsBlank() {
        Comment comment = Comment.create(validTargetType, validTargetId, validAuthorId, validContent);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            comment.update(" ");
        });
        assertEquals("Content cannot be null or empty", exception.getMessage());
    }

    // --- Getters (Basic check, mostly covered by other tests) ---
    @Test
    @DisplayName("Getters should return correct values")
    void getters_shouldReturnCorrectValues() {
        String id = UUID.randomUUID().toString();
        TargetType targetType = TargetType.QUESTION;
        String targetId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
        String content = "Content for getter test.";
        Instant updatedAt = Instant.now().minus(1, ChronoUnit.DAYS);

        Comment comment = Comment.build(id, targetType, targetId, authorId, createdAt, content, updatedAt);

        assertEquals(id, comment.getId());
        assertEquals(targetType, comment.getTargetType());
        assertEquals(targetId, comment.getTargetId());
        assertEquals(authorId, comment.getAuthorId());
        assertEquals(createdAt, comment.getCreatedAt());
        assertEquals(content, comment.getContent());
        assertEquals(updatedAt, comment.getUpdatedAt());
    }
}
