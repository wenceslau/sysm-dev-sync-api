package com.sysm.devsync.domain.models;

import com.sysm.devsync.domain.enums.UserRole;
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

// Note: The class under test has a typo "Workspacce" instead of "Workspace".
// Tests reflect this current naming.
class WorkspaceTest {

    private String validName;
    private String validDescription;
    private String validOwner;
    private String member1;
    private String member2;

    @BeforeEach
    void setUp() {
        validName = "Test Workspace";
        validDescription = "A description for the test workspace.";
        // Assuming User.create is available and works as defined in UserTest.
        // ROLE enum is also assumed to be available.
        validOwner = UUID.randomUUID().toString();
        member1 =  UUID.randomUUID().toString();
        member2 =  UUID.randomUUID().toString();
    }

    // --- Create Tests ---

    @Test
    @DisplayName("create should create workspace successfully with valid arguments")
    void create_shouldCreateWorkspace_whenArgumentsAreValid() {
        Instant beforeCreation = Instant.now();

        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        workspace.addMember(member1);

        Instant afterCreation = Instant.now();

        assertNotNull(workspace.getId(), "ID should not be null");
        try {
            UUID.fromString(workspace.getId()); // Check if ID is a valid UUID
        } catch (IllegalArgumentException e) {
            fail("ID is not a valid UUID: " + workspace.getId());
        }

        assertNotNull(workspace.getCreatedAt(), "CreatedAt should not be null");
        assertTrue(!workspace.getCreatedAt().isBefore(beforeCreation) && !workspace.getCreatedAt().isAfter(afterCreation),
                "CreatedAt should be very close to Instant.now()");

        assertEquals(validName, workspace.getName());
        assertEquals(validDescription, workspace.getDescription());
        assertFalse(workspace.isPrivate(), "isPrivate should be false as provided");
        assertEquals(validOwner, workspace.getOwnerId());

        assertNotNull(workspace.getMembersId(), "Members set should not be null");
        assertEquals(1, workspace.getMembersId().size(), "Members set should contain one member");
        assertTrue(workspace.getMembersId().contains(member1), "Members set should contain the added member");

        // Test immutability of the returned members set
        assertThrows(UnsupportedOperationException.class, () -> workspace.getMembersId().add(member2),
                "Should not be able to modify the members set returned by getMembers()");
    }

    @Test
    @DisplayName("create should initialize with empty members set if provided members set is empty")
    void create_shouldInitializeWithEmptyMembers_whenProvidedMembersIsEmpty() {
        Workspace workspace = Workspace.create(validName, validDescription, true, validOwner);
        assertNotNull(workspace.getMembersId());
        assertTrue(workspace.getMembersId().isEmpty(), "Members should be an empty set");
    }

    @Test
    @DisplayName("create should result in NPE for getMembers if members argument is null and no members added")
    void create_shouldResultInNPEForGetMembers_ifMembersArgumentIsNull() {
        // Current behavior: if 'members' is null, 'this.members' in Workspacce becomes null.
        // getMembers() calls Collections.unmodifiableSet(null), throwing NPE.
        Workspace workspaceWithNullMembers = Workspace.create(validName, validDescription, true, validOwner);

        assertNotNull(workspaceWithNullMembers.getMembersId(), "Members should be an empty set");

        // However, addMember initializes the set if it's null
        workspaceWithNullMembers.addMember(member1);
        assertNotNull(workspaceWithNullMembers.getMembersId());
        assertEquals(1, workspaceWithNullMembers.getMembersId().size());
        assertTrue(workspaceWithNullMembers.getMembersId().contains(member1));
    }


    @Test
    @DisplayName("create should throw IllegalArgumentException for null name")
    void create_shouldThrowException_whenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Workspace.create(null, validDescription, false, validOwner);
        });
        assertEquals("Workspace name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException for blank name")
    void create_shouldThrowException_whenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Workspace.create("  ", validDescription, false, validOwner);
        });
        assertEquals("Workspace name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException for null description")
    void create_shouldThrowException_whenDescriptionIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Workspace.create(validName, null, false, validOwner);
        });
        assertEquals("Workspace description cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException for blank description")
    void create_shouldThrowException_whenDescriptionIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Workspace.create(validName, "  ", false, validOwner);
        });
        assertEquals("Workspace description cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException for null ownerId")
    void create_shouldThrowException_whenOwnerIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Workspace.create(validName, validDescription, false, null);
        });
        assertEquals("Owner cannot be null", exception.getMessage());
    }

    // --- Build Tests ---

    @Test
    @DisplayName("build should create workspace successfully with all arguments")
    void build_shouldCreateWorkspace_whenArgumentsAreValid() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(12, ChronoUnit.HOURS);
        Set<String> members = new HashSet<>();
        members.add(member1);

        Workspace workspace = Workspace.build(id, createdAt, updatedAt, validName, validDescription, true, validOwner, members);

        assertEquals(id, workspace.getId());
        assertEquals(createdAt, workspace.getCreatedAt());
        assertEquals(updatedAt, workspace.getUpdatedAt());
        assertEquals(validName, workspace.getName());
        assertEquals(validDescription, workspace.getDescription());
        assertTrue(workspace.isPrivate());
        assertEquals(validOwner, workspace.getOwnerId());
        assertNotNull(workspace.getMembersId());
        assertEquals(1, workspace.getMembersId().size());
        assertTrue(workspace.getMembersId().contains(member1));
    }

    @Test
    @DisplayName("build should allow null updatedAt")
    void build_shouldAllowNullUpdatedAt() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Workspace workspace = Workspace.build(id, createdAt, null, validName, validDescription, true, validOwner, Collections.emptySet());
        assertNull(workspace.getUpdatedAt());
    }

    @Test
    @DisplayName("build should result in NPE for getMembers if members argument is null and no members added")
    void build_shouldResultInNPEForGetMembers_ifMembersArgumentIsNull() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();

        Workspace workspaceWithNullMembers = Workspace.build(id, createdAt, updatedAt, validName, validDescription, true, validOwner, null);

        assertNotNull(workspaceWithNullMembers.getMembersId(), "Members should be an empty set");

        workspaceWithNullMembers.addMember(member1); // addMember initializes the set
        assertNotNull(workspaceWithNullMembers.getMembersId());
        assertEquals(1, workspaceWithNullMembers.getMembersId().size());
    }

    @Test
    @DisplayName("build should throw IllegalArgumentException for null name (from constructor validation)")
    void build_shouldThrowException_whenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Workspace.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(), null, validDescription, false, validOwner, Collections.emptySet());
        });
        assertEquals("Workspace name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("build should throw IllegalArgumentException for null description (from constructor validation)")
    void build_shouldThrowException_whenDescriptionIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Workspace.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(), validName, null, false, validOwner, Collections.emptySet());
        });
        assertEquals("Workspace description cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("build should throw IllegalArgumentException for null ownerId (from constructor validation)")
    void build_shouldThrowException_whenOwnerIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Workspace.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(), validName, validDescription, false, null, Collections.emptySet());
        });
        assertEquals("Owner cannot be null", exception.getMessage());
    }

    // --- Update Tests ---
    @Test
    @DisplayName("update should modify name and description, and set/update timestamp")
    void update_shouldModifyNameAndDescription_andSetOrUpdateTimestamp() throws InterruptedException {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        assertNotNull(workspace.getUpdatedAt(), "UpdatedAt should be null after create()");

        Thread.sleep(1); // Ensure time moves forward
        Instant timeBeforeActualUpdateCall = Instant.now();
        Thread.sleep(1); // Optional: another small delay

        String newName = "Updated Workspace Name";
        String newDescription = "Updated description.";
        Workspace updatedWorkspace = workspace.update(newName, newDescription);

        assertSame(workspace, updatedWorkspace, "Update method should return the same instance");
        assertEquals(newName, workspace.getName());
        assertEquals(newDescription, workspace.getDescription());

        Instant finalUpdatedAt = workspace.getUpdatedAt();
        assertNotNull(finalUpdatedAt, "UpdatedAt should now be set");
        assertTrue(finalUpdatedAt.isAfter(timeBeforeActualUpdateCall), "UpdatedAt should be after the call to update");

        // Ensure other fields remain unchanged
        assertNotNull(workspace.getId());
        assertNotNull(workspace.getCreatedAt());
        assertFalse(workspace.isPrivate());
        assertEquals(validOwner, workspace.getOwnerId());
        assertTrue(workspace.getMembersId().isEmpty());
    }

    @Test
    @DisplayName("update should throw IllegalArgumentException for null name")
    void update_shouldThrowException_whenNameIsNull() {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            workspace.update(null, "New Description");
        });
        assertEquals("Workspace name cannot be null or blank", exception.getMessage());
    }

    // Other update validation tests (blank name, null/blank description) follow the same pattern...
    @Test
    @DisplayName("update should throw IllegalArgumentException for blank name")
    void update_shouldThrowException_whenNameIsBlank() {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            workspace.update(" ", "New Description");
        });
        assertEquals("Workspace name cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("update should throw IllegalArgumentException for null description")
    void update_shouldThrowException_whenDescriptionIsNull() {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            workspace.update("New Name", null);
        });
        assertEquals("Workspace description cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("update should throw IllegalArgumentException for blank description")
    void update_shouldThrowException_whenDescriptionIsBlank() {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            workspace.update("New Name", "  ");
        });
        assertEquals("Workspace description cannot be null or blank", exception.getMessage());
    }

    // --- ChangeOwner Tests ---
    @Test
    @DisplayName("changeOwner should update owner and timestamp")
    void changeOwner_shouldUpdateOwner_andUpdateTimestamp() throws InterruptedException {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        User newOwner = User.create("newOwnerUser", "newowner@example.com", UserRole.ADMIN);

        // Ensure updatedAt is set if it was null, or capture current if already set by a previous update
        workspace.update("temp name", "temp desc"); // Sets initial updatedAt
        Instant updatedAtBeforeChange = workspace.getUpdatedAt();
        assertNotNull(updatedAtBeforeChange);
        Thread.sleep(1);

        workspace.changeOwner(newOwner.getId());

        assertEquals(newOwner.getId(), workspace.getOwnerId());
        assertTrue(workspace.getUpdatedAt().isAfter(updatedAtBeforeChange));
    }

    @Test
    @DisplayName("changeOwner should throw IllegalArgumentException for null newOwner")
    void changeOwner_shouldThrowException_whenNewOwnerIsNull() {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            workspace.changeOwner(null);
        });
        assertEquals("New owner cannot be null", exception.getMessage());
    }

    // --- SetPrivate Tests ---
    @Test
    @DisplayName("setPrivate should update isPrivate flag and timestamp")
    void setPrivate_shouldUpdateIsPrivate_andUpdateTimestamp() throws InterruptedException {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner); // Initially public
        assertFalse(workspace.isPrivate());

        workspace.update("temp name", "temp desc"); // Sets initial updatedAt
        Instant updatedAtBeforeSetTrue = workspace.getUpdatedAt();
        assertNotNull(updatedAtBeforeSetTrue);
        Thread.sleep(1);

        workspace.setPrivate(true);
        assertTrue(workspace.isPrivate());
        assertTrue(workspace.getUpdatedAt().isAfter(updatedAtBeforeSetTrue), "UpdatedAt should be after setting private to true");

        Instant updatedAtAfterSetTrue = workspace.getUpdatedAt();
        Thread.sleep(1);

        workspace.setPrivate(false);
        assertFalse(workspace.isPrivate());
        assertTrue(workspace.getUpdatedAt().isAfter(updatedAtAfterSetTrue), "UpdatedAt should be after setting private to false");
    }

    // --- AddMember Tests ---
    @Test
    @DisplayName("addMember should add user to members set and initialize set if null")
    void addMember_shouldAddUserToMembers_andInitializeSet() {
        // Start with null members by using build carefully or knowing create's behavior
        Workspace workspace = Workspace.build(UUID.randomUUID().toString(), Instant.now(), null,
                validName, validDescription, false, validOwner, null); // members is null

        workspace.addMember(member1);
        assertNotNull(workspace.getMembersId(), "Members set should be initialized by addMember");
        assertEquals(1, workspace.getMembersId().size());
        assertTrue(workspace.getMembersId().contains(member1));

        workspace.addMember(member2);
        assertEquals(2, workspace.getMembersId().size());
        assertTrue(workspace.getMembersId().contains(member1));
        assertTrue(workspace.getMembersId().contains(member2));

        // Adding the same member again should not change the set size
        workspace.addMember(member1);
        assertEquals(2, workspace.getMembersId().size());
    }

    @Test
    @DisplayName("addMember should throw IllegalArgumentException if user is null")
    void addMember_shouldThrowException_whenUserIsNull() {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            workspace.addMember(null);
        });
        assertEquals("User cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("addMember should not update 'updatedAt' timestamp")
    void addMember_shouldNotUpdateTimestamp() throws InterruptedException {
        Workspace workspace = Workspace.create(validName, validDescription, false, validOwner);
        workspace.update("temp", "temp"); // Set an initial updatedAt
        Instant updatedAtBeforeAdd = workspace.getUpdatedAt();
        assertNotNull(updatedAtBeforeAdd);

        Thread.sleep(1);
        workspace.addMember(member1);

        assertEquals(updatedAtBeforeAdd, workspace.getUpdatedAt(), "UpdatedAt should not change after adding a member");
    }

    // --- Getters Tests ---
    @Test
    @DisplayName("Getters should return correct values set by build")
    void getters_shouldReturnCorrectValues() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant updatedAt = Instant.now().minusSeconds(60);
        String name = "Getter Test Name";
        String description = "Getter Test Description";
        boolean isPrivate = true;
        User owner = User.create("getterOwner", "getter@owner.com",  UserRole.ADMIN);
        Set<String> members = new HashSet<>();
        User getterMember = User.create("getterMember", "getter@member.com", UserRole.MEMBER);
        members.add(getterMember.getId());

        Workspace workspace = Workspace.build(id, createdAt, updatedAt, name, description, isPrivate, owner.getId(), members);

        assertEquals(id, workspace.getId());
        assertEquals(createdAt, workspace.getCreatedAt());
        assertEquals(updatedAt, workspace.getUpdatedAt());
        assertEquals(name, workspace.getName());
        assertEquals(description, workspace.getDescription());
        assertEquals(isPrivate, workspace.isPrivate());
        assertEquals(owner.getId(), workspace.getOwnerId());

        assertNotNull(workspace.getMembersId());
        assertEquals(1, workspace.getMembersId().size());
        assertTrue(workspace.getMembersId().contains(getterMember.getId()));
        assertThrows(UnsupportedOperationException.class, () -> workspace.getMembersId().clear(),
                "Should not be able to modify members set via getter");
    }

    // --- Equals and HashCode Tests ---
    @Test
    @DisplayName("equals and hashCode should be based on ID")
    void equalsAndHashCode_shouldBeBasedOnId() {
        String id = UUID.randomUUID().toString();
        // Create two workspaces with the same ID but different other properties
        Workspace ws1 = Workspace.build(id, Instant.now(), Instant.now(), "Name1", "Desc1", false, validOwner, null);
        Workspace ws2 = Workspace.build(id, Instant.now().plusSeconds(10), Instant.now().plusSeconds(20), "Name2", "Desc2", true, member1, new HashSet<>());
        // Create a third workspace with a different ID
        Workspace ws3 = Workspace.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(), "Name1", "Desc1", false, validOwner, null);

        assertEquals(ws1, ws2, "Equals should return true for workspaces with the same ID");
        assertEquals(ws1.hashCode(), ws2.hashCode(), "HashCode should be the same for workspaces with the same ID");

        assertNotEquals(ws1, ws3, "Equals should return false for workspaces with different IDs");
        // Note: HashCode *could* be the same for different IDs (collision), but unlikely for UUIDs.
        // The primary contract is: if equals() is true, hashCode() must be same.
        // If equals() is false, hashCode() can be same or different.
        // For this test, we expect them to be different with high probability.
        if (ws1.hashCode() == ws3.hashCode()) {
            System.out.println("Warning: HashCode collision between ws1 and ws3, though IDs are different. This is rare but possible.");
        }


        assertNotEquals(null, ws1, "Equals should return false when comparing with null");
        assertNotEquals(new Object(), ws1, "Equals should return false when comparing with an object of a different type");
    }
}
