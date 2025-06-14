package com.sysm.devsync.domain.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectTest {

    private String validName;
    private String validDescription;
    private String validWorkspaceId;

    // --- Setup ---
    @BeforeEach
    void setUp() {
        validName = "Test Project Alpha";
        validDescription = "This is a detailed description for Test Project Alpha.";
        validWorkspaceId = UUID.randomUUID().toString();
    }

    // --- Private Constructor (tested via static factory methods) ---
    // No direct tests for private constructor, its logic is covered by create() and build()

    // --- Static Factory Method: create() ---

    @Test
    @DisplayName("create() should successfully create a project with valid arguments")
    void create_shouldSucceed_withValidArguments() {
        Instant beforeCreation = Instant.now();
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        Instant afterCreation = Instant.now();

        assertNotNull(project.getId(), "ID should be generated and not null");
        try {
            UUID.fromString(project.getId()); // Validate UUID format
        } catch (IllegalArgumentException e) {
            fail("Generated ID is not a valid UUID: " + project.getId());
        }

        assertEquals(validName, project.getName());
        assertEquals(validDescription, project.getDescription());
        assertEquals(validWorkspaceId, project.getWorkspaceId());

        assertNotNull(project.getCreatedAt(), "CreatedAt should be set");
        assertNotNull(project.getUpdatedAt(), "UpdatedAt should be set");
        assertEquals(project.getCreatedAt(), project.getUpdatedAt(), "CreatedAt and UpdatedAt should be same on creation");

        assertTrue(!project.getCreatedAt().isBefore(beforeCreation) && !project.getCreatedAt().isAfter(afterCreation),
                "CreatedAt should be very close to the time of creation");
    }

    @Test
    @DisplayName("create() should allow null workspaceId")
    void create_shouldAllowNullWorkspaceId() {
        Project project = Project.create(validName, validDescription, null);
        assertNull(project.getWorkspaceId(), "WorkspaceId should be null if passed as null");
    }

    @Test
    @DisplayName("create() should allow blank workspaceId")
    void create_shouldAllowBlankWorkspaceId() {
        Project project = Project.create(validName, validDescription, "   ");
        assertEquals("   ", project.getWorkspaceId(), "WorkspaceId should be blank if passed as blank");
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null name")
    void create_shouldThrowException_whenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Project.create(null, validDescription, validWorkspaceId);
        });
        assertEquals("Project name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for blank name")
    void create_shouldThrowException_whenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Project.create("  ", validDescription, validWorkspaceId);
        });
        assertEquals("Project name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null description")
    void create_shouldThrowException_whenDescriptionIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Project.create(validName, null, validWorkspaceId);
        });
        assertEquals("Project description cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for blank description")
    void create_shouldThrowException_whenDescriptionIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Project.create(validName, "   ", validWorkspaceId);
        });
        assertEquals("Project description cannot be null or blank", exception.getMessage());
    }

    // --- Static Factory Method: build() ---

    @Test
    @DisplayName("build() should successfully create a project with all arguments")
    void build_shouldSucceed_withAllArguments() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(12, ChronoUnit.HOURS);

        Project project = Project.build(id, validName, validDescription, validWorkspaceId, createdAt, updatedAt);

        assertEquals(id, project.getId());
        assertEquals(validName, project.getName());
        assertEquals(validDescription, project.getDescription());
        assertEquals(validWorkspaceId, project.getWorkspaceId());
        assertEquals(createdAt, project.getCreatedAt());
        assertEquals(updatedAt, project.getUpdatedAt());
    }

    @Test
    @DisplayName("build() should allow null workspaceId")
    void build_shouldAllowNullWorkspaceId() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        Project project = Project.build(id, validName, validDescription, null, createdAt, createdAt);
        assertNull(project.getWorkspaceId());
    }

    @Test
    @DisplayName("build() should allow null updatedAt")
    void build_shouldAllowNullUpdatedAt() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        Project project = Project.build(id, validName, validDescription, validWorkspaceId, createdAt, null);
        assertNull(project.getUpdatedAt());
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for null name (via constructor)")
    void build_shouldThrowException_whenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Project.build(UUID.randomUUID().toString(), null, validDescription, validWorkspaceId, Instant.now(), Instant.now());
        });
        assertEquals("Project name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for blank name (via constructor)")
    void build_shouldThrowException_whenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Project.build(UUID.randomUUID().toString(), "  ", validDescription, validWorkspaceId, Instant.now(), Instant.now());
        });
        assertEquals("Project name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for null description (via constructor)")
    void build_shouldThrowException_whenDescriptionIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Project.build(UUID.randomUUID().toString(), validName, null, validWorkspaceId, Instant.now(), Instant.now());
        });
        assertEquals("Project description cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for blank description (via constructor)")
    void build_shouldThrowException_whenDescriptionIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Project.build(UUID.randomUUID().toString(), validName, "  ", validWorkspaceId, Instant.now(), Instant.now());
        });
        assertEquals("Project description cannot be null or blank", exception.getMessage());
    }


    // --- Instance Method: update() ---

    @Test
    @DisplayName("update() should modify name and description, and update timestamp")
    void update_shouldModifyNameAndDescription_andSetTimestamp() throws InterruptedException {
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        Instant initialUpdatedAt = project.getUpdatedAt();
        String initialId = project.getId();
        Instant initialCreatedAt = project.getCreatedAt();
        String initialWorkspaceId = project.getWorkspaceId();

        Thread.sleep(1); // Ensure updatedAt will be different

        String newName = "Updated Project Name";
        String newDescription = "This is the updated description.";
        Project updatedProject = project.update(newName, newDescription);

        assertSame(project, updatedProject, "Update method should return the same instance");
        assertEquals(newName, project.getName());
        assertEquals(newDescription, project.getDescription());
        assertTrue(project.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after the initial value");

        // Ensure other fields remain unchanged
        assertEquals(initialId, project.getId());
        assertEquals(initialCreatedAt, project.getCreatedAt());
        assertEquals(initialWorkspaceId, project.getWorkspaceId());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new name")
    void update_shouldThrowException_whenNewNameIsNull() {
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            project.update(null, "New Description");
        });
        assertEquals("Project name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for blank new name")
    void update_shouldThrowException_whenNewNameIsBlank() {
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            project.update("   ", "New Description");
        });
        assertEquals("Project name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new description")
    void update_shouldThrowException_whenNewDescriptionIsNull() {
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            project.update("New Name", null);
        });
        assertEquals("Project description cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for blank new description")
    void update_shouldThrowException_whenNewDescriptionIsBlank() {
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            project.update("New Name", "   ");
        });
        assertEquals("Project description cannot be null or blank", exception.getMessage());
    }

    // --- Instance Method: changeWorkspace() ---

    @Test
    @DisplayName("changeWorkspace() should update workspaceId and timestamp")
    void changeWorkspace_shouldUpdateWorkspaceId_andSetTimestamp() throws InterruptedException {
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        Instant initialUpdatedAt = project.getUpdatedAt();
        String initialId = project.getId();
        Instant initialCreatedAt = project.getCreatedAt();
        String initialName = project.getName();
        String initialDescription = project.getDescription();

        Thread.sleep(1); // Ensure updatedAt will be different

        String newWorkspaceId = UUID.randomUUID().toString();
        Project updatedProject = project.changeWorkspace(newWorkspaceId);

        assertSame(project, updatedProject, "changeWorkspace method should return the same instance");
        assertEquals(newWorkspaceId, project.getWorkspaceId());
        assertTrue(project.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after the initial value");

        // Ensure other fields remain unchanged
        assertEquals(initialId, project.getId());
        assertEquals(initialCreatedAt, project.getCreatedAt());
        assertEquals(initialName, project.getName());
        assertEquals(initialDescription, project.getDescription());
    }

    @Test
    @DisplayName("changeWorkspace() should throw IllegalArgumentException for null newWorkspaceId")
    void changeWorkspace_shouldThrowException_whenNewWorkspaceIdIsNull() {
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            project.changeWorkspace(null);
        });
        assertEquals("New workspace ID cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("changeWorkspace() should throw IllegalArgumentException for blank newWorkspaceId")
    void changeWorkspace_shouldThrowException_whenNewWorkspaceIdIsBlank() {
        Project project = Project.create(validName, validDescription, validWorkspaceId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            project.changeWorkspace("   ");
        });
        assertEquals("New workspace ID cannot be null or blank", exception.getMessage());
    }

    // --- Getters (Basic check, mostly covered by other tests) ---
    @Test
    @DisplayName("Getters should return correct values after construction via build")
    void getters_shouldReturnCorrectValues() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        String name = "Getter Test Project";
        String description = "Description for getter test.";
        String workspaceId = UUID.randomUUID().toString();

        Project project = Project.build(id, name, description, workspaceId, createdAt, updatedAt);

        assertEquals(id, project.getId());
        assertEquals(name, project.getName());
        assertEquals(description, project.getDescription());
        assertEquals(workspaceId, project.getWorkspaceId());
        assertEquals(createdAt, project.getCreatedAt());
        assertEquals(updatedAt, project.getUpdatedAt());
    }
}
