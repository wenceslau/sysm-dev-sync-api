package com.sysm.devsync.application;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.infrastructure.controller.dto.CreateResponse;
import com.sysm.devsync.infrastructure.controller.dto.request.UserCreateUpdate;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// It's good practice to import StringUtils if your service uses it,
// though for these tests, we are primarily testing the service's logic
// and how it calls the User domain object's methods.
// import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserPersistencePort userPersistence;

    @InjectMocks
    private UserService userService;

    private UserCreateUpdate validUserCreateUpdateDto;
    private String userId;
    private User mockUser; // Added for patch tests

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        validUserCreateUpdateDto = new UserCreateUpdate(
                "Test User",
                "test@example.com",
                "http://example.com/profile.jpg", // Assuming profilePictureUrl is part of UserCreateUpdate
                UserRole.MEMBER
        );
        mockUser = mock(User.class); // Initialize mockUser here
    }

    // --- createUser Tests ---

    @Test
    @DisplayName("createUser should create and save user successfully")
    void createUser_shouldCreateAndSaveUserSuccessfully() {
        // Arrange
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        doNothing().when(userPersistence).create(userCaptor.capture());

        // Act
        CreateResponse response = userService.createUser(validUserCreateUpdateDto);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());

        verify(userPersistence, times(1)).create(any(User.class));
        User capturedUser = userCaptor.getValue();

        assertEquals(validUserCreateUpdateDto.name(), capturedUser.getName());
        assertEquals(validUserCreateUpdateDto.email(), capturedUser.getEmail());
        assertEquals(validUserCreateUpdateDto.userRole(), capturedUser.getRole());
        // Assuming User.create also initializes profilePictureUrl if provided in DTO,
        // or that it's handled by a subsequent call if User.create doesn't take it.
        // Based on User.create signature, profilePictureUrl is not set there.
        // It's set via user.updateProfilePicture in the updateUser method.
        // For createUser, profilePictureUrl from DTO is not used by User.create.
        assertEquals(response.id(), capturedUser.getId());
    }

    @Test
    @DisplayName("createUser should propagate IllegalArgumentException from User.create for invalid email")
    void createUser_shouldPropagateException_forInvalidEmail() {
        // Arrange
        UserCreateUpdate invalidDto = new UserCreateUpdate("Test User", "invalid-email", "http://example.com/pic.jpg", UserRole.MEMBER);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(invalidDto);
        });
        assertTrue(exception.getMessage().contains("Email"));
        verify(userPersistence, never()).create(any());
    }

    // --- updateUser Tests ---

    @Test
    @DisplayName("updateUser should update existing user successfully")
    void updateUser_shouldUpdateExistingUserSuccessfully() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser)); // Use the class-level mockUser
        doNothing().when(userPersistence).update(any(User.class));

        UserCreateUpdate userUpdateDto = new UserCreateUpdate("New Name", "new@example.com", "http://new.pic/url.jpg", UserRole.ADMIN);

        // Act
        userService.updateUser(userId, userUpdateDto);

        // Assert
        verify(userPersistence, times(1)).findById(userId);
        verify(mockUser, times(1)).update(
                userUpdateDto.name(),
                userUpdateDto.email(),
                userUpdateDto.userRole()
        );
        verify(mockUser, times(1)).updateProfilePicture(userUpdateDto.profilePictureUrl()); // Verify profile picture update
        verify(userPersistence, times(1)).update(mockUser);
    }

    @Test
    @DisplayName("updateUser should not update profile picture if URL is null or empty")
    void updateUser_shouldNotUpdateProfilePicture_ifUrlIsNullOrEmpty() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        doNothing().when(userPersistence).update(any(User.class));

        UserCreateUpdate userUpdateDtoNullUrl = new UserCreateUpdate("New Name", "new@example.com", null, UserRole.ADMIN);
        UserCreateUpdate userUpdateDtoEmptyUrl = new UserCreateUpdate("New Name", "new@example.com", "", UserRole.ADMIN);
        UserCreateUpdate userUpdateDtoBlankUrl = new UserCreateUpdate("New Name", "new@example.com", "   ", UserRole.ADMIN);


        // Act & Assert for null URL
        userService.updateUser(userId, userUpdateDtoNullUrl);
        verify(mockUser, never()).updateProfilePicture(null); // StringUtils.hasText(null) is false

        // Act & Assert for empty URL
        userService.updateUser(userId, userUpdateDtoEmptyUrl);
        verify(mockUser, never()).updateProfilePicture(""); // StringUtils.hasText("") is false

        // Act & Assert for blank URL
        userService.updateUser(userId, userUpdateDtoBlankUrl);
        verify(mockUser, never()).updateProfilePicture("   "); // StringUtils.hasText("   ") is false

        // Verify other updates still happen (example for one case)
        verify(mockUser, times(3)).update( // Called for each of the 3 scenarios above
                "New Name",
                "new@example.com",
                UserRole.ADMIN
        );
        verify(userPersistence, times(3)).update(mockUser);
    }


    @Test
    @DisplayName("updateUser should throw IllegalArgumentException if user not found")
    void updateUser_shouldThrowException_ifUserNotFound() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(userId, validUserCreateUpdateDto);
        });
        assertEquals("User not found", exception.getMessage());
        verify(userPersistence, never()).update(any());
    }

    @Test
    @DisplayName("updateUser should propagate IllegalArgumentException from user.update for invalid email")
    void updateUser_shouldPropagateException_forInvalidEmailInUpdate() {
        // Arrange
        // We need a real User instance here to trigger its internal validation,
        // or mock the behavior of user.update() to throw the exception.
        // Let's use a real instance for this specific propagation test.
        User realExistingUser = User.create("Old Name", "old@example.com", UserRole.MEMBER);
        when(userPersistence.findById(userId)).thenReturn(Optional.of(realExistingUser));
        UserCreateUpdate invalidUpdateDto = new UserCreateUpdate("New Name", "invalid-email", null, UserRole.ADMIN);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(userId, invalidUpdateDto);
        });
        assertTrue(exception.getMessage().contains("Email"));
        verify(userPersistence, never()).update(any()); // update on persistence port should not be called
    }

    // --- updateUserPatch Tests ---

    @Test
    @DisplayName("updateUserPatch should throw IllegalArgumentException if user not found")
    void updateUserPatch_shouldThrowException_ifUserNotFound() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.empty());
        UserCreateUpdate patchDto = new UserCreateUpdate("Patch Name", null, null, null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUserPatch(userId, patchDto);
        });
        assertEquals("User not found", exception.getMessage());
        verify(userPersistence, never()).update(any());
    }

    @Test
    @DisplayName("updateUserPatch should update all provided fields")
    void updateUserPatch_shouldUpdateAllProvidedFields() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        UserCreateUpdate patchDto = new UserCreateUpdate(
                "Patched Name",
                "patched.email@example.com",
                "http://patched.url/pic.jpg",
                UserRole.ADMIN
        );

        // Act
        userService.updateUserPatch(userId, patchDto);

        // Assert
        verify(mockUser).updateName("Patched Name");
        verify(mockUser).updateEmail("patched.email@example.com");
        verify(mockUser).updateUserRole(UserRole.ADMIN);
        verify(mockUser).updateProfilePicture("http://patched.url/pic.jpg");
        verify(userPersistence).update(mockUser);
    }

    @Test
    @DisplayName("updateUserPatch should only update name when only name is provided")
    void updateUserPatch_shouldOnlyUpdateName_whenOnlyNameIsProvided() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        UserCreateUpdate patchDto = new UserCreateUpdate("Only Name Patch", null, null, null);

        // Act
        userService.updateUserPatch(userId, patchDto);

        // Assert
        verify(mockUser).updateName("Only Name Patch");
        verify(mockUser, never()).updateEmail(anyString());
        verify(mockUser, never()).updateUserRole(any(UserRole.class));
        verify(mockUser, never()).updateProfilePicture(anyString());
        verify(userPersistence).update(mockUser);
    }

    @Test
    @DisplayName("updateUserPatch should only update email when only email is provided")
    void updateUserPatch_shouldOnlyUpdateEmail_whenOnlyEmailIsProvided() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        UserCreateUpdate patchDto = new UserCreateUpdate(null, "only.email@patch.com", null, null);

        // Act
        userService.updateUserPatch(userId, patchDto);

        // Assert
        verify(mockUser, never()).updateName(anyString());
        verify(mockUser).updateEmail("only.email@patch.com");
        verify(mockUser, never()).updateUserRole(any(UserRole.class));
        verify(mockUser, never()).updateProfilePicture(anyString());
        verify(userPersistence).update(mockUser);
    }

    @Test
    @DisplayName("updateUserPatch should only update role when only role is provided")
    void updateUserPatch_shouldOnlyUpdateRole_whenOnlyRoleIsProvided() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        UserCreateUpdate patchDto = new UserCreateUpdate(null, null, null, UserRole.ADMIN);

        // Act
        userService.updateUserPatch(userId, patchDto);

        // Assert
        verify(mockUser, never()).updateName(anyString());
        verify(mockUser, never()).updateEmail(anyString());
        verify(mockUser).updateUserRole(UserRole.ADMIN);
        verify(mockUser, never()).updateProfilePicture(anyString());
        verify(userPersistence).update(mockUser);
    }

    @Test
    @DisplayName("updateUserPatch should only update profile picture when only URL is provided")
    void updateUserPatch_shouldOnlyUpdateProfilePicture_whenOnlyUrlIsProvided() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        UserCreateUpdate patchDto = new UserCreateUpdate(null, null, "http://only.pic/url.jpg", null);

        // Act
        userService.updateUserPatch(userId, patchDto);

        // Assert
        verify(mockUser, never()).updateName(anyString());
        verify(mockUser, never()).updateEmail(anyString());
        verify(mockUser, never()).updateUserRole(any(UserRole.class));
        verify(mockUser).updateProfilePicture("http://only.pic/url.jpg");
        verify(userPersistence).update(mockUser);
    }

    @Test
    @DisplayName("updateUserPatch should not update fields if DTO values are null or empty/blank for strings")
    void updateUserPatch_shouldNotUpdateFields_forNullOrEmptyDtoValues() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        UserCreateUpdate patchDto = new UserCreateUpdate(null, "", "   ", null); // name=null, email="", profilePic="   ", role=null

        // Act
        userService.updateUserPatch(userId, patchDto);

        // Assert
        verify(mockUser, never()).updateName(anyString());
        verify(mockUser, never()).updateEmail(anyString());
        verify(mockUser, never()).updateUserRole(any(UserRole.class));
        verify(mockUser, never()).updateProfilePicture(anyString());
        verify(userPersistence).update(mockUser); // Persistence update is still called
    }

    @Test
    @DisplayName("updateUserPatch should correctly handle mixed provided and not-provided fields")
    void updateUserPatch_shouldHandleMixedFields() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        UserCreateUpdate patchDto = new UserCreateUpdate("Mixed Name", null, "http://mixed.pic/url.jpg", null);

        // Act
        userService.updateUserPatch(userId, patchDto);

        // Assert
        verify(mockUser).updateName("Mixed Name");
        verify(mockUser, never()).updateEmail(anyString());
        verify(mockUser, never()).updateUserRole(any(UserRole.class));
        verify(mockUser).updateProfilePicture("http://mixed.pic/url.jpg");
        verify(userPersistence).update(mockUser);
    }

    @Test
    @DisplayName("updateUserPatch should propagate exceptions from User domain methods")
    void updateUserPatch_shouldPropagateDomainExceptions() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockUser));
        UserCreateUpdate patchDtoWithInvalidEmail = new UserCreateUpdate(null, "invalid-email", null, null);

        // Configure mockUser.updateEmail to throw an exception when called with "invalid-email"
        doThrow(new IllegalArgumentException("Invalid email format from domain"))
                .when(mockUser).updateEmail("invalid-email");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUserPatch(userId, patchDtoWithInvalidEmail);
        });
        assertEquals("Invalid email format from domain", exception.getMessage());
        verify(mockUser).updateEmail("invalid-email"); // Ensure the method was called
        verify(userPersistence, never()).update(mockUser); // Persistence update should not happen if domain validation fails
    }


    // --- deleteUser Tests ---

    @Test
    @DisplayName("deleteUser should call repository deleteById")
    void deleteUser_shouldCallRepositoryDeleteById() {
        // Arrange
        doNothing().when(userPersistence).deleteById(userId);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userPersistence, times(1)).deleteById(userId);
    }

    // --- getUserById Tests ---

    @Test
    @DisplayName("getUserById should return user if found")
    void getUserById_shouldReturnUser_ifFound() {
        // Arrange
        // For this test, returning the mockUser is fine if we don't need to assert its state,
        // or we can use a real User instance.
        User expectedUser = User.create("Test User", "test@example.com", UserRole.MEMBER); // Or use mockUser
        when(userPersistence.findById(userId)).thenReturn(Optional.of(expectedUser));

        // Act
        User actualUser = userService.getUserById(userId);

        // Assert
        assertNotNull(actualUser);
        assertSame(expectedUser, actualUser);
        verify(userPersistence, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserById should throw IllegalArgumentException if user not found")
    void getUserById_shouldThrowException_ifUserNotFound() {
        // Arrange
        when(userPersistence.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(userId);
        });
        assertEquals("User not found", exception.getMessage());
        verify(userPersistence, times(1)).findById(userId);
    }

    // --- getAllUsers Tests ---

    @Test
    @DisplayName("getAllUsers should return pagination result from repository")
    void getAllUsers_shouldReturnPaginationResult_fromRepository() {
        // Arrange
        SearchQuery query = new SearchQuery(new Pageable(1, 10,  "asc", "search"), "name");
        Pagination<User> expectedPagination = new Pagination<>(1, 10, 0L, Collections.emptyList()); // Ensure totalElements is Long
        when(userPersistence.findAll(query)).thenReturn(expectedPagination);

        // Act
        Pagination<User> actualPagination = userService.getAllUsers(query);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(userPersistence, times(1)).findAll(query);
    }
}

//package com.sysm.devsync.application;
//
//import com.sysm.devsync.domain.Pagination;
//import com.sysm.devsync.domain.SearchQuery;
//import com.sysm.devsync.domain.enums.UserRole;
//import com.sysm.devsync.domain.models.User;
//import com.sysm.devsync.controller.dto.CreateResponse;
//import com.sysm.devsync.controller.dto.request.UserCreateUpdate;
//import com.sysm.devsync.domain.repositories.UserPersistencePort;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Collections;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceTest {
//
//    @Mock
//    private UserPersistencePort userPersistence;
//
//    @InjectMocks
//    private UserService userService;
//
//    private UserCreateUpdate validUserCreateUpdateDto;
//    private String userId;
//
//    @BeforeEach
//    void setUp() {
//        userId = UUID.randomUUID().toString();
//        validUserCreateUpdateDto = new UserCreateUpdate(
//                "Test User",
//                "test@example.com",
//                "hashedPassword123",
//                UserRole.MEMBER
//        );
//    }
//
//    // --- createUser Tests ---
//
//    @Test
//    @DisplayName("createUser should create and save user successfully")
//    void createUser_shouldCreateAndSaveUserSuccessfully() {
//        // Arrange
//        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
//        // Mocking repository.create to simulate saving and allow capturing
//        // The User.create method will generate an ID, so we don't need to mock that part for the ID itself.
//        doNothing().when(userPersistence).create(userCaptor.capture());
//
//        // Act
//        CreateResponse response = userService.createUser(validUserCreateUpdateDto);
//
//        // Assert
//        assertNotNull(response);
//        assertNotNull(response.id());
//
//        verify(userPersistence, times(1)).create(any(User.class));
//        User capturedUser = userCaptor.getValue();
//
//        assertEquals(validUserCreateUpdateDto.name(), capturedUser.getName());
//        assertEquals(validUserCreateUpdateDto.email(), capturedUser.getEmail());
//        assertEquals(validUserCreateUpdateDto.userRole(), capturedUser.getRole());
//        assertEquals(response.id(), capturedUser.getId()); // Ensure the ID in response matches the captured user's ID
//    }
//
//    @Test
//    @DisplayName("createUser should propagate IllegalArgumentException from User.create for invalid email")
//    void createUser_shouldPropagateException_forInvalidEmail() {
//        // Arrange
//        UserCreateUpdate invalidDto = new UserCreateUpdate("Test User", "invalid-email", "pass", UserRole.MEMBER);
//
//        // Act & Assert
//        // Assuming User.create() throws IllegalArgumentException for invalid email format
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//            userService.createUser(invalidDto);
//        });
//        // The exact message depends on User.create() validation
//        // For example: assertEquals("Invalid email format", exception.getMessage());
//        // For now, let's assume a generic message or that the specific validation is tested in UserTest
//        assertTrue(exception.getMessage().contains("Email")); // A more general check
//        verify(userPersistence, never()).create(any());
//    }
//
//    // --- updateUser Tests ---
//
//    @Test
//    @DisplayName("updateUser should update existing user successfully")
//    void updateUser_shouldUpdateExistingUserSuccessfully() {
//        // Arrange
//        User existingUser = User.create("Old Name", "old@example.com", UserRole.MEMBER);
//        // Manually set the ID for the existingUser to match `userId` for the test
//        // This is a bit of a workaround because User.create() generates a new ID.
//        // A better way might be to use User.build() if available and suitable for creating test data.
//        // Or, ensure the `existingUser` instance used in `findById` has the `userId`.
//        // For simplicity here, let's assume `findById` returns a user whose ID is `userId`.
//        // We can mock the User object itself if we need fine-grained control over its ID.
//        // Let's refine this:
//        User mockExistingUser = mock(User.class);
//
//        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockExistingUser));
//        doNothing().when(userPersistence).update(any(User.class));
//
//        UserCreateUpdate userUpdateDto = new UserCreateUpdate("New Name", "new@example.com", null, UserRole.ADMIN); // Password hash is not updated here
//
//        // Act
//        userService.updateUser(userId, userUpdateDto);
//
//        verify(userPersistence, times(1)).findById(userId);
//        verify(mockExistingUser, times(1)).update(
//                userUpdateDto.name(),
//                userUpdateDto.email(),
//                userUpdateDto.userRole()
//        );
//        verify(userPersistence, times(1)).update(mockExistingUser);
//    }
//
//    @Test
//    @DisplayName("updateUser should throw IllegalArgumentException if user not found")
//    void updateUser_shouldThrowException_ifUserNotFound() {
//        // Arrange
//        when(userPersistence.findById(userId)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//            userService.updateUser(userId, validUserCreateUpdateDto);
//        });
//        assertEquals("User not found", exception.getMessage());
//        verify(userPersistence, never()).update(any());
//    }
//
//    @Test
//    @DisplayName("updateUser should propagate IllegalArgumentException from user.update for invalid email")
//    void updateUser_shouldPropagateException_forInvalidEmailInUpdate() {
//        // Arrange
//        User existingUser = User.create("Old Name", "old@example.com", UserRole.MEMBER);
//        when(userPersistence.findById(userId)).thenReturn(Optional.of(existingUser));
//        UserCreateUpdate invalidUpdateDto = new UserCreateUpdate("New Name", "invalid-email", null, UserRole.ADMIN);
//
//        // Act & Assert
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//            userService.updateUser(userId, invalidUpdateDto);
//        });
//        // This assertion depends on the validation message from User.update()
//        assertTrue(exception.getMessage().contains("Email"));
//        verify(userPersistence, never()).update(any());
//    }
//
//    // --- deleteUser Tests ---
//
//    @Test
//    @DisplayName("deleteUser should call repository deleteById")
//    void deleteUser_shouldCallRepositoryDeleteById() {
//        // Arrange
//        doNothing().when(userPersistence).deleteById(userId);
//
//        // Act
//        userService.deleteUser(userId);
//
//        // Assert
//        verify(userPersistence, times(1)).deleteById(userId);
//    }
//
//    // --- getUserById Tests ---
//
//    @Test
//    @DisplayName("getUserById should return user if found")
//    void getUserById_shouldReturnUser_ifFound() {
//        // Arrange
//        User expectedUser = User.create("Test User", "test@example.com", UserRole.MEMBER);
//        when(userPersistence.findById(userId)).thenReturn(Optional.of(expectedUser));
//
//        // Act
//        User actualUser = userService.getUserById(userId);
//
//        // Assert
//        assertNotNull(actualUser);
//        assertSame(expectedUser, actualUser);
//        verify(userPersistence, times(1)).findById(userId);
//    }
//
//    @Test
//    @DisplayName("getUserById should throw IllegalArgumentException if user not found")
//    void getUserById_shouldThrowException_ifUserNotFound() {
//        // Arrange
//        when(userPersistence.findById(userId)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//            userService.getUserById(userId);
//        });
//        assertEquals("User not found", exception.getMessage());
//        verify(userPersistence, times(1)).findById(userId);
//    }
//
//    // --- getAllUsers Tests ---
//
//    @Test
//    @DisplayName("getAllUsers should return pagination result from repository")
//    void getAllUsers_shouldReturnPaginationResult_fromRepository() {
//        // Arrange
//        SearchQuery query = new SearchQuery(new Pageable(1, 10) "name", "asc", "search");
//        Pagination<User> expectedPagination = new Pagination<>(1, 10, 0, Collections.emptyList());
//        when(userPersistence.findAll(query)).thenReturn(expectedPagination);
//
//        // Act
//        Pagination<User> actualPagination = userService.getAllUsers(query);
//
//        // Assert
//        assertNotNull(actualPagination);
//        assertSame(expectedPagination, actualPagination);
//        verify(userPersistence, times(1)).findAll(query);
//    }
//}
