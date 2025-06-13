package com.sysm.devsync.domain;

import com.sysm.devsync.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private String validUsername;
    private String validEmail;
    private String validProfilePictureUrl;
    private Role validRole;

    @BeforeEach
    void setUp() {
        validUsername = "testuser";
        validEmail = "test@example.com";
        validProfilePictureUrl = "http://example.com/pic.jpg";
        validRole = Role.MEMBER; // Assuming ROLE is an enum with MEMBER and ADMIN
    }

    // --- Create Tests ---

    @Test
    @DisplayName("Create should create user successfully with valid arguments")
    void create_shouldCreateUser_whenArgumentsAreValid() {
        Instant beforeCreation = Instant.now();
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        Instant afterCreation = Instant.now();

        assertNotNull(user.getId(), "ID should not be null");
        try {
            UUID.fromString(user.getId()); // Check if ID is a valid UUID
        } catch (IllegalArgumentException e) {
            fail("ID is not a valid UUID: " + user.getId());
        }

        assertNotNull(user.getCreatedAt(), "CreatedAt should not be null");
        // Check if createdAt is between the time before and after the call, inclusive
        assertTrue(!user.getCreatedAt().isBefore(beforeCreation) && !user.getCreatedAt().isAfter(afterCreation),
                "CreatedAt should be very close to Instant.now()");


        assertNotNull(user.getUpdatedAt(), "UpdatedAt should not be null");
        assertEquals(user.getCreatedAt(), user.getUpdatedAt(), "Initially, createdAt and updatedAt should be the same");

        assertEquals(validUsername, user.getName());
        assertEquals(validEmail, user.getEmail());
        assertEquals(validProfilePictureUrl, user.getProfilePictureUrl());
        assertEquals(validRole, user.getRole());
        assertNull(user.getPasswordHash(), "Password hash should be null on initial creation");
    }

    @Test
    @DisplayName("Create should allow null profilePictureUrl")
    void create_shouldAllowNullProfilePictureUrl() {
        User user = User.create(validUsername, validEmail, null, validRole);
        assertNull(user.getProfilePictureUrl(), "ProfilePictureUrl should be null if passed as null");
    }

    @Test
    @DisplayName("Create should allow blank profilePictureUrl")
    void create_shouldAllowBlankProfilePictureUrl() {
        User user = User.create(validUsername, validEmail, " ", validRole);
        assertEquals(" ", user.getProfilePictureUrl(), "ProfilePictureUrl should be blank if passed as blank");
    }

    @Test
    @DisplayName("Create should throw IllegalArgumentException for null username")
    void create_shouldThrowException_whenUsernameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.create(null, validEmail, validProfilePictureUrl, validRole);
        });
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Create should throw IllegalArgumentException for blank username")
    void create_shouldThrowException_whenUsernameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.create(" ", validEmail, validProfilePictureUrl, validRole);
        });
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Create should throw IllegalArgumentException for null email")
    void create_shouldThrowException_whenEmailIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.create(validUsername, null, validProfilePictureUrl, validRole);
        });
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Create should throw IllegalArgumentException for blank email")
    void create_shouldThrowException_whenEmailIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.create(validUsername, "  ", validProfilePictureUrl, validRole);
        });
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Create should throw IllegalArgumentException for null role")
    void create_shouldThrowException_whenRoleIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.create(validUsername, validEmail, validProfilePictureUrl, null);
        });
        assertEquals("Role cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Create should throw IllegalArgumentException for blank role")
    void create_shouldThrowException_whenRoleIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.create(validUsername, validEmail, validProfilePictureUrl, null);
        });
        assertEquals("Role cannot be null or empty", exception.getMessage());
    }

    // --- Update Method Tests ---

    @Test
    @DisplayName("Update method should modify user details and update timestamp")
    void update_shouldModifyUserDetails_andUpdateTimestamp() throws InterruptedException {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        Instant initialUpdatedAt = user.getUpdatedAt();
        String initialId = user.getId();
        Instant initialCreatedAt = user.getCreatedAt();
        String initialPasswordHash = user.getPasswordHash(); // Should be null initially
        String initialProfilePicUrl = user.getProfilePictureUrl();


        // Ensure time moves forward for updatedAt comparison
        Thread.sleep(1); // Small delay to ensure updatedAt changes for the test

        String newUsername = "updatedUser";
        String newEmail = "updated@example.com";
        Role newRole = Role.ADMIN;

        User updatedUser = user.update(newUsername, newEmail, newRole);

        assertSame(user, updatedUser, "Update method should return the same instance");
        assertEquals(newUsername, user.getName());
        assertEquals(newEmail, user.getEmail());
        assertEquals(newRole, user.getRole());
        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt), "UpdatedAt should be after initial value");

        // Ensure non-updated fields remain the same
        assertEquals(initialId, user.getId(), "ID should not change on update");
        assertEquals(initialCreatedAt, user.getCreatedAt(), "CreatedAt should not change on update");
        assertEquals(initialProfilePicUrl, user.getProfilePictureUrl(), "Profile picture URL should not change on general update");
        assertEquals(initialPasswordHash, user.getPasswordHash(), "Password hash should not change on general update");
    }

    @Test
    @DisplayName("Update method should throw IllegalArgumentException for null username")
    void update_shouldThrowException_whenUsernameIsNull() {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.update(null, "new@email.com", Role.ADMIN);
        });
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Update method should throw IllegalArgumentException for blank username")
    void update_shouldThrowException_whenUsernameIsBlank() {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.update("  ", "new@email.com", Role.ADMIN);
        });
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Update method should throw IllegalArgumentException for null email")
    void update_shouldThrowException_whenEmailIsNull() {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.update("newUsername", null, Role.ADMIN);
        });
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Update method should throw IllegalArgumentException for blank email")
    void update_shouldThrowException_whenEmailIsBlank() {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.update("newUsername", "  ", Role.ADMIN);
        });
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Update method should throw IllegalArgumentException for null role")
    void update_shouldThrowException_whenRoleIsNull() {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.update("newUsername", "new@email.com", null);
        });
        assertEquals("Role cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Update method should throw IllegalArgumentException for blank role")
    void update_shouldThrowException_whenRoleIsBlank() {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.update("newUsername", "new@email.com", null);
        });
        assertEquals("Role cannot be null or empty", exception.getMessage());
    }

    // --- UpdatePassword Method Tests ---
    @Test
    @DisplayName("UpdatePassword method should update password hash and timestamp")
    void updatePassword_shouldUpdatePasswordHash_andUpdateTimestamp() throws InterruptedException {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        Instant initialUpdatedAt = user.getUpdatedAt();
        String newPasswordHash = "newSecureHash123";

        Thread.sleep(1); // Ensure time moves forward

        User updatedUser = user.updatePassword(newPasswordHash);

        assertSame(user, updatedUser, "UpdatePassword method should return the same instance");
        assertEquals(newPasswordHash, user.getPasswordHash());
        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt));

        // Check other fields remain unchanged
        assertEquals(validUsername, user.getName());
        assertEquals(validEmail, user.getEmail());
        assertEquals(validRole, user.getRole());
        assertEquals(validProfilePictureUrl, user.getProfilePictureUrl());
    }

    @Test
    @DisplayName("UpdatePassword method should allow null password hash")
    void updatePassword_shouldAllowNullPasswordHash() throws InterruptedException {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        user.updatePassword("initialHash"); // Set an initial hash
        assertNotNull(user.getPasswordHash());

        Instant initialUpdatedAt = user.getUpdatedAt();
        Thread.sleep(1); // Ensure time moves forward

        user.updatePassword(null);
        assertNull(user.getPasswordHash(), "PasswordHash should be updatable to null");
        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt));
    }


    // --- UpdateProfilePicture Method Tests ---
    @Test
    @DisplayName("UpdateProfilePicture method should update URL and timestamp")
    void updateProfilePicture_shouldUpdateUrl_andUpdateTimestamp() throws InterruptedException {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        Instant initialUpdatedAt = user.getUpdatedAt();
        String newProfilePictureUrl = "http://example.com/new_pic.jpg";

        Thread.sleep(1); // Ensure time moves forward

        User updatedUser = user.updateProfilePicture(newProfilePictureUrl);

        assertSame(user, updatedUser, "UpdateProfilePicture method should return the same instance");
        assertEquals(newProfilePictureUrl, user.getProfilePictureUrl());
        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt));

        // Check other fields remain unchanged
        assertEquals(validUsername, user.getName());
        assertEquals(validEmail, user.getEmail());
        assertEquals(validRole, user.getRole());
        assertNull(user.getPasswordHash()); // Password hash should be null if not set
    }

    @Test
    @DisplayName("UpdateProfilePicture method should allow null URL")
    void updateProfilePicture_shouldAllowNullUrl() throws InterruptedException {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        Instant initialUpdatedAt = user.getUpdatedAt();
        Thread.sleep(1); // Ensure time moves forward

        user.updateProfilePicture(null);
        assertNull(user.getProfilePictureUrl(), "ProfilePictureUrl should be updatable to null");
        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    @Test
    @DisplayName("UpdateProfilePicture method should allow blank URL")
    void updateProfilePicture_shouldAllowBlankUrl() throws InterruptedException {
        User user = User.create(validUsername, validEmail, validProfilePictureUrl, validRole);
        Instant initialUpdatedAt = user.getUpdatedAt();
        Thread.sleep(1); // Ensure time moves forward

        user.updateProfilePicture("  ");
        assertEquals("  ", user.getProfilePictureUrl(), "ProfilePictureUrl should be updatable to blank");
        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    // --- Build Static Factory Method Tests ---
    @Test
    @DisplayName("Build method should create user with all fields set correctly")
    void build_shouldCreateUserWithAllFields() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(1, ChronoUnit.HOURS);
        String username = "buildUser";
        String email = "build@example.com";
        String passwordHash = "buildPasswordHash";
        String profilePictureUrl = "http://build.com/pic.png";
        Role role = Role.ADMIN;

        User user = User.build(id, createdAt, updatedAt, username, email, passwordHash, profilePictureUrl, role);

        assertEquals(id, user.getId());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
        assertEquals(username, user.getName());
        assertEquals(email, user.getEmail());
        assertEquals(passwordHash, user.getPasswordHash());
        assertEquals(profilePictureUrl, user.getProfilePictureUrl());
        assertEquals(role, user.getRole());
    }

    @Test
    @DisplayName("Build method should allow null passwordHash and profilePictureUrl")
    void build_shouldAllowNullOptionalFields() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updatedAt = Instant.now().minus(1, ChronoUnit.HOURS);
        String username = "buildUserOptional";
        String email = "buildopt@example.com";
        Role role = Role.MEMBER;

        User user = User.build(id, createdAt, updatedAt, username, email, null, null, role);

        assertEquals(id, user.getId());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
        assertEquals(username, user.getName());
        assertEquals(email, user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getProfilePictureUrl());
        assertEquals(role, user.getRole());
    }

    @Test
    @DisplayName("Build method should throw IllegalArgumentException for null username due to create validation")
    void build_shouldThrowException_whenUsernameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                    null, validEmail, "hash", validProfilePictureUrl, validRole);
        });
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Build method should throw IllegalArgumentException for blank username due to create validation")
    void build_shouldThrowException_whenUsernameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                    " ", validEmail, "hash", validProfilePictureUrl, validRole);
        });
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }


    @Test
    @DisplayName("Build method should throw IllegalArgumentException for null email due to create validation")
    void build_shouldThrowException_whenEmailIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                    validUsername, null, "hash", validProfilePictureUrl, validRole);
        });
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Build method should throw IllegalArgumentException for blank email due to create validation")
    void build_shouldThrowException_whenEmailIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                    validUsername, "  ", "hash", validProfilePictureUrl, validRole);
        });
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Build method should throw IllegalArgumentException for null role due to create validation")
    void build_shouldThrowException_whenRoleIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                    validUsername, validEmail, "hash", validProfilePictureUrl, null);
        });
        assertEquals("Role cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Build method should throw IllegalArgumentException for blank role due to create validation")
    void build_shouldThrowException_whenRoleIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            User.build(UUID.randomUUID().toString(), Instant.now(), Instant.now(),
                    validUsername, validEmail, "hash", validProfilePictureUrl, null);
        });
        assertEquals("Role cannot be null or empty", exception.getMessage());
    }

    // --- Getters Tests (Basic check, mostly covered by other tests) ---
    @Test
    @DisplayName("Getters should return correct values after construction via build")
    void getters_shouldReturnCorrectValues() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        Instant updatedAt = createdAt; // Initially the same, but build will set it
        String username = "getterUser";
        String email = "getter@example.com";
        String passwordHash = "getterPasswordHash";
        String profilePictureUrl = "http://getter.com/pic.png";
        Role role = Role.ADMIN;

        // Use build to set all fields for a comprehensive getter test
        User user = User.build(id, createdAt, updatedAt, username, email, passwordHash, profilePictureUrl, role);

        assertEquals(id, user.getId());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
        assertEquals(username, user.getName());
        assertEquals(email, user.getEmail());
        assertEquals(passwordHash, user.getPasswordHash());
        assertEquals(profilePictureUrl, user.getProfilePictureUrl());
        assertEquals(role, user.getRole());
    }
}
