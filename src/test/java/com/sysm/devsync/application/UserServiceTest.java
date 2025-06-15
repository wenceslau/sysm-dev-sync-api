package com.sysm.devsync.application;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.controller.dto.CreateResponse;
import com.sysm.devsync.controller.dto.request.UserCreateUpdate;
import com.sysm.devsync.domain.repositories.UserPersistencePort;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        validUserCreateUpdateDto = new UserCreateUpdate(
                "Test User",
                "test@example.com",
                "hashedPassword123",
                UserRole.MEMBER
        );
    }

    // --- createUser Tests ---

    @Test
    @DisplayName("createUser should create and save user successfully")
    void createUser_shouldCreateAndSaveUserSuccessfully() {
        // Arrange
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        // Mocking repository.create to simulate saving and allow capturing
        // The User.create method will generate an ID, so we don't need to mock that part for the ID itself.
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
        assertEquals(response.id(), capturedUser.getId()); // Ensure the ID in response matches the captured user's ID
    }

    @Test
    @DisplayName("createUser should propagate IllegalArgumentException from User.create for invalid email")
    void createUser_shouldPropagateException_forInvalidEmail() {
        // Arrange
        UserCreateUpdate invalidDto = new UserCreateUpdate("Test User", "invalid-email", "pass", UserRole.MEMBER);

        // Act & Assert
        // Assuming User.create() throws IllegalArgumentException for invalid email format
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(invalidDto);
        });
        // The exact message depends on User.create() validation
        // For example: assertEquals("Invalid email format", exception.getMessage());
        // For now, let's assume a generic message or that the specific validation is tested in UserTest
        assertTrue(exception.getMessage().contains("Email")); // A more general check
        verify(userPersistence, never()).create(any());
    }

    // --- updateUser Tests ---

    @Test
    @DisplayName("updateUser should update existing user successfully")
    void updateUser_shouldUpdateExistingUserSuccessfully() {
        // Arrange
        User existingUser = User.create("Old Name", "old@example.com", UserRole.MEMBER);
        // Manually set the ID for the existingUser to match `userId` for the test
        // This is a bit of a workaround because User.create() generates a new ID.
        // A better way might be to use User.build() if available and suitable for creating test data.
        // Or, ensure the `existingUser` instance used in `findById` has the `userId`.
        // For simplicity here, let's assume `findById` returns a user whose ID is `userId`.
        // We can mock the User object itself if we need fine-grained control over its ID.
        // Let's refine this:
        User mockExistingUser = mock(User.class);

        when(userPersistence.findById(userId)).thenReturn(Optional.of(mockExistingUser));
        doNothing().when(userPersistence).update(any(User.class));

        UserCreateUpdate userUpdateDto = new UserCreateUpdate("New Name", "new@example.com", null, UserRole.ADMIN); // Password hash is not updated here

        // Act
        userService.updateUser(userId, userUpdateDto);

        verify(userPersistence, times(1)).findById(userId);
        verify(mockExistingUser, times(1)).update(
                userUpdateDto.name(),
                userUpdateDto.email(),
                userUpdateDto.userRole()
        );
        verify(userPersistence, times(1)).update(mockExistingUser);
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
        User existingUser = User.create("Old Name", "old@example.com", UserRole.MEMBER);
        when(userPersistence.findById(userId)).thenReturn(Optional.of(existingUser));
        UserCreateUpdate invalidUpdateDto = new UserCreateUpdate("New Name", "invalid-email", null, UserRole.ADMIN);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(userId, invalidUpdateDto);
        });
        // This assertion depends on the validation message from User.update()
        assertTrue(exception.getMessage().contains("Email"));
        verify(userPersistence, never()).update(any());
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
        User expectedUser = User.create("Test User", "test@example.com", UserRole.MEMBER);
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
        SearchQuery query = new SearchQuery(1, 10, "name", "asc", "search");
        Pagination<User> expectedPagination = new Pagination<>(1, 10, 0, Collections.emptyList());
        when(userPersistence.findAll(query)).thenReturn(expectedPagination);

        // Act
        Pagination<User> actualPagination = userService.getAllUsers(query);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(userPersistence, times(1)).findAll(query);
    }
}
