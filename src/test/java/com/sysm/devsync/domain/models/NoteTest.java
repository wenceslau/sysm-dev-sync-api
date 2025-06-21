package com.sysm.devsync.domain.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.sysm.devsync.infrastructure.Utils.iTruncatedNow;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;

public class NoteTest {

    private String validTitle;
    private String validContent;
    private String validProjectId;
    private String validAuthorId;

    @BeforeEach
    void setUp() {
        validTitle = "Test Note Title";
        validContent = "This is the content of the test note.";
        validProjectId = UUID.randomUUID().toString();
        validAuthorId = UUID.randomUUID().toString();
    }

    // --- Static Factory Method: create() ---

    @Test
    @DisplayName("create() should successfully create a note with valid arguments")
    void create_shouldSucceed_withValidArguments() {
        Instant beforeCreation = iTruncatedNow();
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        Instant afterCreation = iTruncatedNow();
        assertNotNull(note.getId(), "ID should be generated and not null");
        try {
            UUID.fromString(note.getId()); // Validate UUID format
        } catch (IllegalArgumentException e) {
            fail("Generated ID is not a valid UUID: " + note.getId());
        }

        assertEquals(validTitle, note.getTitle());
        assertEquals(validContent, note.getContent());
        assertEquals(validProjectId, note.getProjectId());
        assertEquals(validAuthorId, note.getAuthorId());

        assertNotNull(note.getCreatedAt(), "CreatedAt should be set");
        assertNotNull(note.getUpdatedAt(), "UpdatedAt should be set");
        assertEquals(note.getCreatedAt(), note.getUpdatedAt(), "CreatedAt and UpdatedAt should be same on creation");

        assertTrue(!note.getCreatedAt().isBefore(beforeCreation) && !note.getCreatedAt().isAfter(afterCreation),
                "CreatedAt should be very close to the time of creation");

        assertNotNull(note.getTagsId(), "Tags should be initialized as an empty set");
        assertTrue(note.getTagsId().isEmpty(), "Tags should be empty on creation");
        assertEquals(1, note.getVersion(), "Version should be 1 on creation");
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null title")
    void create_shouldThrowException_whenTitleIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Note.create(null, validContent, validProjectId, validAuthorId);
        });
        assertEquals("Title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty title")
    void create_shouldThrowException_whenTitleIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Note.create("", validContent, validProjectId, validAuthorId);
        });
        assertEquals("Title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null content")
    void create_shouldThrowException_whenContentIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Note.create(validTitle, null, validProjectId, validAuthorId);
        });
        assertEquals("Content cannot be null", exception.getMessage());
    }

    // Note: Content can be empty, so no test for empty content throwing exception for create()

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null projectId")
    void create_shouldThrowException_whenProjectIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Note.create(validTitle, validContent, null, validAuthorId);
        });
        assertEquals("Project ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty projectId")
    void create_shouldThrowException_whenProjectIdIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Note.create(validTitle, validContent, "", validAuthorId);
        });
        assertEquals("Project ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null authorId")
    void create_shouldThrowException_whenAuthorIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Note.create(validTitle, validContent, validProjectId, null);
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty authorId")
    void create_shouldThrowException_whenAuthorIdIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Note.create(validTitle, validContent, validProjectId, "");
        });
        assertEquals("Author ID cannot be null or empty", exception.getMessage());
    }

    // --- Static Factory Method: build() ---

    @Test
    @DisplayName("build() should successfully create a note with all arguments")
    void build_shouldSucceed_withAllArguments() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(MILLIS);
        Instant updatedAt = Instant.now().minus(12, ChronoUnit.HOURS).truncatedTo(MILLIS);
        Set<String> tags = new HashSet<>(Set.of("tag1", "tag2"));
        int version = 5;

        Note note = Note.build(id, createdAt, updatedAt, validTitle, validContent, tags, validProjectId, validAuthorId, version);

        assertEquals(id, note.getId());
        assertEquals(createdAt, note.getCreatedAt());
        assertEquals(updatedAt, note.getUpdatedAt());
        assertEquals(validTitle, note.getTitle());
        assertEquals(validContent, note.getContent());
        assertEquals(tags, note.getTagsId());
        assertEquals(validProjectId, note.getProjectId());
        assertEquals(validAuthorId, note.getAuthorId());
        assertEquals(version, note.getVersion());
    }

    @Test
    @DisplayName("build() should use provided empty set for tags")
    void build_shouldUseProvidedEmptySetForTags() {
        Set<String> emptyTags = Collections.emptySet(); // or new HashSet<>()
        Note note = Note.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                validTitle, validContent, emptyTags, validProjectId, validAuthorId, 1);
        assertNotNull(note.getTagsId());
        assertTrue(note.getTagsId().isEmpty());
        // Depending on whether the constructor copies the set or uses the reference:
        // If it copies: assertNotSame(emptyTags, note.getTags());
        // If it uses reference (as it seems to): assertSame(emptyTags, note.getTags());
        // Current implementation of build() passes the reference if not null.
        // Let's assume it's okay to use the reference, or the test can be adjusted if a defensive copy is intended.
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for null id (via constructor)")
    void build_shouldThrowException_whenIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Note.build(null, Instant.now(), Instant.now(), validTitle, validContent, new HashSet<>(), validProjectId, validAuthorId, 1);
        });
        assertEquals("ID cannot be null or empty", exception.getMessage());
    }
    // Other validation tests for build() (title, content, projectId, authorId) would mirror create() tests as they go through the same constructor validation.

    // --- Instance Method: update() ---

    @Test
    @DisplayName("update() should modify title and content, update timestamp, and increment version")
    void update_shouldModifyTitleContentTimestampAndVersion() throws InterruptedException {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        Instant initialUpdatedAt = note.getUpdatedAt();
        int initialVersion = note.getVersion();
        String initialId = note.getId();
        Instant initialCreatedAt = note.getCreatedAt();
        String initialProjectId = note.getProjectId();
        String initialAuthorId = note.getAuthorId();
        Set<String> initialTags = new HashSet<>(note.getTagsId()); // Copy for comparison

        Thread.sleep(1); // Ensure updatedAt will be different

        String newTitle = "Updated Note Title";
        String newContent = "This is the updated content.";
        note.update(newTitle, newContent);

        assertEquals(newTitle, note.getTitle());
        assertEquals(newContent, note.getContent());
        assertTrue(note.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after the initial value");
        assertEquals(initialVersion + 1, note.getVersion(), "Version should be incremented");

        // Ensure other fields remain unchanged
        assertEquals(initialId, note.getId());
        assertEquals(initialCreatedAt, note.getCreatedAt());
        assertEquals(initialProjectId, note.getProjectId());
        assertEquals(initialAuthorId, note.getAuthorId());
        assertEquals(initialTags, note.getTagsId(), "Tags should not be changed by update()");
    }

    @Test
    @DisplayName("update() should allow empty content")
    void update_shouldAllowEmptyContent() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        assertDoesNotThrow(() -> note.update("New Title", ""));
        assertEquals("", note.getContent());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new title")
    void update_shouldThrowException_whenNewTitleIsNull() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            note.update(null, "New Content");
        });
        assertEquals("Title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for empty new title")
    void update_shouldThrowException_whenNewTitleIsEmpty() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            note.update("", "New Content");
        });
        assertEquals("Title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new content")
    void update_shouldThrowException_whenNewContentIsNull() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            note.update("New Title", null);
        });
        assertEquals("Content cannot be null", exception.getMessage());
    }


    @Test
    @DisplayName("UpdateUserRole should update role and timestamp")
    void updateNoteContent_shouldUpdateContentAndTimestamp() throws InterruptedException {
        String newContent = "Updated content for the note.";
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        Instant initialUpdatedAt = note.getUpdatedAt();

        Thread.sleep(1); // Ensure time moves forward
        note.updateContent(newContent);

        assertEquals(newContent, note.getContent());
        assertTrue(note.getUpdatedAt().isAfter(initialUpdatedAt));
        // Verify other fields remain unchanged
        assertEquals(validTitle, note.getTitle());
        assertEquals(validProjectId, note.getProjectId());
        assertEquals(validAuthorId, note.getAuthorId());
        assertEquals(2, note.getVersion(), "Version should be w as updateContent increment it");
    }

    @Test
    @DisplayName("UpdateUserRole should throw IllegalArgumentException for null role")
    void updateNoteContent_shouldThrowException_whenContentIsNull() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            note.updateContent(null);
        });
        assertEquals("Content cannot be null", exception.getMessage());
    }

    // --- Instance Method: addTag() ---

    @Test
    @DisplayName("addTag() should add a new tag to an existing set")
    void addTag_shouldAddNewTag() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        note.addTag("initialTag"); // Ensure tags set is initialized and has one item

        note.addTag("newTag");
        assertTrue(note.getTagsId().contains("newTag"));
        assertEquals(2, note.getTagsId().size());
    }

    @Test
    @DisplayName("addTag() should check if tag is empty on initialization")
    void addTag_shouldInitializeTagsAndAddTag_ifTagsIsNull() {
        // Create a note where 'tags' might be null (e.g., if build allowed it, or direct constructor use without init)
        // For this test, let's use build and pass null, relying on addTag's internal null check
        Note note = Note.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                validTitle, validContent, null, validProjectId, validAuthorId, 1);
        assertTrue(note.getTagsId().isEmpty(), "Tags should be empty initially");

        note.addTag("firstTag");
        assertNotNull(note.getTagsId(), "Tags should be initialized by addTag");
        assertTrue(note.getTagsId().contains("firstTag"));
        assertEquals(1, note.getTagsId().size());
    }

    @Test
    @DisplayName("addTag() should not add a duplicate tag")
    void addTag_shouldNotAddDuplicateTag() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        note.addTag("uniqueTag");
        note.addTag("uniqueTag");
        assertEquals(1, note.getTagsId().size());
    }

    @Test
    @DisplayName("addTag() should throw IllegalArgumentException for null tag")
    void addTag_shouldThrowException_whenTagIsNull() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            note.addTag(null);
        });
        assertEquals("Tag cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("addTag() should throw IllegalArgumentException for empty tag")
    void addTag_shouldThrowException_whenTagIsEmpty() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            note.addTag("");
        });
        assertEquals("Tag cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("addTag() should not change updatedAt or version")
    void addTag_shouldNotChangeUpdatedAtOrVersion() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        Instant updatedAtBefore = note.getUpdatedAt();
        int versionBefore = note.getVersion();

        note.addTag("someTag");

        assertEquals(updatedAtBefore, note.getUpdatedAt(), "UpdatedAt should not change after adding a tag");
        assertEquals(versionBefore, note.getVersion(), "Version should not change after adding a tag");
    }


    // --- Instance Method: removeTag() ---

    @Test
    @DisplayName("removeTag() should remove an existing tag")
    void removeTag_shouldRemoveExistingTag() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        note.addTag("tagToRemove");
        note.addTag("tagToKeep");

        note.removeTag("tagToRemove");
        assertFalse(note.getTagsId().contains("tagToRemove"));
        assertTrue(note.getTagsId().contains("tagToKeep"));
        assertEquals(1, note.getTagsId().size());
    }

    @Test
    @DisplayName("removeTag() should do nothing if tag does not exist")
    void removeTag_shouldDoNothing_ifTagDoesNotExist() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        note.addTag("existingTag");
        int initialSize = note.getTagsId().size();

        note.removeTag("nonExistentTag");
        assertEquals(initialSize, note.getTagsId().size());
        assertTrue(note.getTagsId().contains("existingTag"));
    }

    @Test
    @DisplayName("removeTag() should do nothing if tags set is null")
    void removeTag_shouldDoNothing_ifTagsSetIsNull() {
        Note note = Note.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                validTitle, validContent, null, validProjectId, validAuthorId, 1);
        assertTrue(note.getTagsId().isEmpty());

        assertDoesNotThrow(() -> note.removeTag("anyTag"));
        assertTrue(note.getTagsId().isEmpty(), "Tags should remain empty if they were null");
    }

    @Test
    @DisplayName("removeTag() should do nothing if tags set is empty")
    void removeTag_shouldDoNothing_ifTagsSetIsEmpty() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId); // Tags is empty HashSet
        assertTrue(note.getTagsId().isEmpty());

        assertDoesNotThrow(() -> note.removeTag("anyTag"));
        assertTrue(note.getTagsId().isEmpty());
    }


    @Test
    @DisplayName("removeTag() should throw IllegalArgumentException for null tag")
    void removeTag_shouldThrowException_whenTagIsNull() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        note.addTag("sample");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            note.removeTag(null);
        });
        assertEquals("Tag cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("removeTag() should throw IllegalArgumentException for empty tag")
    void removeTag_shouldThrowException_whenTagIsEmpty() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        note.addTag("sample");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            note.removeTag("");
        });
        assertEquals("Tag cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("removeTag() should not change updatedAt or version")
    void removeTag_shouldNotChangeUpdatedAtOrVersion() {
        Note note = Note.create(validTitle, validContent, validProjectId, validAuthorId);
        note.addTag("tagToRemove");
        Instant updatedAtBefore = note.getUpdatedAt();
        int versionBefore = note.getVersion();

        note.removeTag("tagToRemove");

        assertEquals(updatedAtBefore, note.getUpdatedAt(), "UpdatedAt should not change after removing a tag");
        assertEquals(versionBefore, note.getVersion(), "Version should not change after removing a tag");
    }

    // --- Getters (Basic check, mostly covered by other tests) ---
    @Test
    @DisplayName("Getters should return correct values after construction via build")
    void getters_shouldReturnCorrectValues() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(MILLIS);
        Instant updatedAt = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(MILLIS);
        String title = "Getter Test Title";
        String content = "Content for getter test.";
        Set<String> tags = new HashSet<>(Set.of("getterTag1", "getterTag2"));
        String projectId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        int version = 10;

        Note note = Note.build(id, createdAt, updatedAt, title, content, tags, projectId, authorId, version);

        assertEquals(id, note.getId());
        assertEquals(createdAt, note.getCreatedAt());
        assertEquals(updatedAt, note.getUpdatedAt());
        assertEquals(title, note.getTitle());
        assertEquals(content, note.getContent());
        assertEquals(tags, note.getTagsId());
        assertEquals(projectId, note.getProjectId());
        assertEquals(authorId, note.getAuthorId());
        assertEquals(version, note.getVersion());
    }
}
