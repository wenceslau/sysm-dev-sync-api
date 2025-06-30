package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.infrastructure.controllers.dto.request.UserCreateUpdate;
import com.sysm.devsync.infrastructure.repositories.UserJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserIntegrationTest extends AbstractIntegrationTest {

    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    @Autowired
    private UserJpaRepository userJpaRepository;

    @BeforeEach
    void setUp() {
        // This ensures each test runs with a clean database, preventing side effects.
        userJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("POST /users - should create a new user successfully")
    void createUser_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new UserCreateUpdate("John Doe", "john.doe@example.com", "http://example.com/pic.jpg", UserRole.ADMIN);
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        var responseContent = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();

        // Extract the ID from the response
        var createdUserId = objectMapper.readTree(responseContent).get("id").asText();

        // Verify the state in the database
        Optional<UserJpaEntity> createdUserEntity = userJpaRepository.findById(createdUserId);
        assertThat(createdUserEntity).isPresent();
        assertThat(createdUserEntity.get().getName()).isEqualTo("John Doe");
        assertThat(createdUserEntity.get().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(createdUserEntity.get().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("POST /users - should fail with 400 Bad Request for invalid data (blank email)")
    void createUser_withInvalidData_shouldFail() throws Exception {
        // Arrange: Email is blank, which violates the @NotBlank constraint
        var requestDto = new UserCreateUpdate("Jane Doe", "", null, UserRole.MEMBER);
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Location", nullValue()))
                .andExpect(jsonPath("$.validationErrors.email[0]", equalTo("User email must not be blank")));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /users/{id} - should retrieve an existing user")
    void getUserById_shouldSucceed() throws Exception {
        // Arrange: Create a user directly in the DB
        User userDomain = User.create("Alice", "alice@example.com", UserRole.ADMIN);
        UserJpaEntity userJpa = UserJpaEntity.fromModel(userDomain);
        userJpaRepository.saveAndFlush(userJpa);

        // Act & Assert
        mockMvc.perform(get("/users/{id}", userJpa.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userJpa.getId()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /users/{id} - should return 404 Not Found for non-existent user")
    void getUserById_whenNotFound_shouldFail() throws Exception {
        // Arrange
        var nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(get("/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("User not found")));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("PUT /users/{id} - should update an existing user")
    void updateUser_shouldSucceed() throws Exception {
        // Arrange: Create the initial user
        User userDomain = User.create("Bob", "bob@example.com", UserRole.ADMIN);
        UserJpaEntity userJpa = UserJpaEntity.fromModel(userDomain);
        userJpaRepository.saveAndFlush(userJpa);

        // Prepare the update request
        var updateDto = new UserCreateUpdate("Robert", "robert@example.com", "http://example.com/new-pic.jpg", UserRole.MEMBER);
        var requestJson = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/users/{id}", userJpa.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify the state in the database
        Optional<UserJpaEntity> updatedUserEntity = userJpaRepository.findById(userJpa.getId());
        assertThat(updatedUserEntity).isPresent();
        assertThat(updatedUserEntity.get().getName()).isEqualTo("Robert");
        assertThat(updatedUserEntity.get().getEmail()).isEqualTo("robert@example.com");
        assertThat(updatedUserEntity.get().getProfilePictureUrl()).isEqualTo("http://example.com/new-pic.jpg");
        assertThat(updatedUserEntity.get().getRole()).isEqualTo(UserRole.MEMBER);
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("PATCH /users/{id} - should partially update an existing user's name")
    void updateUserPatch_shouldSucceed() throws Exception {
        // Arrange: Create the initial user
        User userDomain = User.create("Charlie", "charlie@example.com", UserRole.MEMBER);
        UserJpaEntity userJpa = UserJpaEntity.fromModel(userDomain);
        userJpaRepository.saveAndFlush(userJpa);

        // Prepare the PATCH request - only updating the name. Other fields are null.
        // This tests your service logic, as @Valid is correctly omitted on the controller.
        var patchDto = new UserCreateUpdate("Charles", null, null, null);
        var requestJson = objectMapper.writeValueAsString(patchDto);

        // Act & Assert
        mockMvc.perform(patch("/users/{id}", userJpa.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify the state in the database
        Optional<UserJpaEntity> updatedUserEntity = userJpaRepository.findById(userJpa.getId());
        assertThat(updatedUserEntity).isPresent();
        // Name should be updated
        assertThat(updatedUserEntity.get().getName()).isEqualTo("Charles");
        // Email and Role should remain unchanged
        assertThat(updatedUserEntity.get().getEmail()).isEqualTo("charlie@example.com");
        assertThat(updatedUserEntity.get().getRole()).isEqualTo(UserRole.MEMBER);
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("DELETE /users/{id} - should delete an existing user")
    void deleteUser_shouldSucceed() throws Exception {
        // Arrange: Create the user to be deleted
        User userDomain = User.create("David", "david@example.com", UserRole.ADMIN);
        UserJpaEntity userJpa = UserJpaEntity.fromModel(userDomain);
        userJpaRepository.saveAndFlush(userJpa);

        // Verify it exists before deletion
        assertThat(userJpaRepository.existsById(userJpa.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/users/{id}", userJpa.getId()))
                .andExpect(status().isNoContent());

        // Verify it no longer exists in the database
        assertThat(userJpaRepository.existsById(userJpa.getId())).isFalse();
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /users - should return paginated list of users")
    void searchUsers_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        userJpaRepository.save(UserJpaEntity.fromModel(User.create("Zoe", "zoe@example.com", UserRole.ADMIN)));
        userJpaRepository.save(UserJpaEntity.fromModel(User.create("Aaron", "aaron@example.com", UserRole.ADMIN)));
        userJpaRepository.save(UserJpaEntity.fromModel(User.create("Brian", "brian@example.com", UserRole.MEMBER)));
        userJpaRepository.flush();

        // Act & Assert
        mockMvc.perform(get("/users")
                        .param("pageNumber", "0")
                        .param("pageSize", "2")
                        .param("sort", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Aaron"))
                .andExpect(jsonPath("$.items[1].name").value("Brian"));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /users - should return users filtered by query parameters")
    void searchUsers_withFilters_shouldReturnFilteredResults() throws Exception {
        // Arrange
        userJpaRepository.save(UserJpaEntity.fromModel(User.create("Charles Admin", "charles@example.com", UserRole.ADMIN)));
        userJpaRepository.save(UserJpaEntity.fromModel(User.create("Diana Member", "diana@example.com", UserRole.MEMBER)));
        userJpaRepository.save(UserJpaEntity.fromModel(User.create("Charles Member", "charles.m@example.com", UserRole.MEMBER)));
        userJpaRepository.flush();

        // Act & Assert - Filter by name
        mockMvc.perform(get("/users")
                        .param("name", "Diana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Diana Member"));

        // Act & Assert - Filter by role
        mockMvc.perform(get("/users")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Charles Admin"));

        // Act & Assert - Filter by multiple fields (name and role)
        mockMvc.perform(get("/users")
                        .param("name", "Charles")
                        .param("role", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Charles Member"));

        // Act & Assert - Filter with no results
        mockMvc.perform(get("/users")
                        .param("name", "Diana")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
