package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.infrastructure.controllers.dto.request.WorkspaceCreateUpdate;
import com.sysm.devsync.infrastructure.repositories.UserJpaRepository;
import com.sysm.devsync.infrastructure.repositories.WorkspaceJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class WorkspaceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    // This ID is hardcoded in WorkspaceController, so we need a user with this ID for creation tests
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";
    private UserJpaEntity ownerUser;

    @BeforeEach
    void setUp() {
        // Clean repositories to ensure test isolation
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // Ensure the fake authenticated user exists for tests that need it
        ownerUser = new UserJpaEntity();
        ownerUser.setId(FAKE_AUTHENTICATED_USER_ID);
        ownerUser.setName("Fake Owner");
        ownerUser.setEmail("owner@fake.com");
        ownerUser.setRole(UserRole.ADMIN);
        ownerUser.setCreatedAt(Instant.now());
        ownerUser.setUpdatedAt(Instant.now());
        userJpaRepository.saveAndFlush(ownerUser);
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("POST /workspaces - should create a new workspace successfully")
    void createWorkspace_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new WorkspaceCreateUpdate("DevSync Project", "Main workspace for DevSync", false);
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(header().exists("Location"));

        // Verify DB state
        var createdWorkspaces = workspaceJpaRepository.findAll();
        assertThat(createdWorkspaces).hasSize(1);
        var createdWorkspace = createdWorkspaces.get(0);
        assertThat(createdWorkspace.getName()).isEqualTo("DevSync Project");
        assertThat(createdWorkspace.getOwner().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("POST /workspaces - should fail with 400 for invalid data")
    void createWorkspace_withInvalidData_shouldFail() throws Exception {
        // Arrange: Name is blank
        var requestDto = new WorkspaceCreateUpdate("", "Description", false);
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name[0]", equalTo("Workspace name must not be blank")));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /workspaces/{id} - should retrieve an existing workspace")
    void getWorkspaceById_shouldSucceed() throws Exception {
        // Arrange
        Workspace ws = Workspace.create("My Workspace", "A test workspace", true, FAKE_AUTHENTICATED_USER_ID);
        WorkspaceJpaEntity savedWs = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(ws));

        // Act & Assert
        mockMvc.perform(get("/workspaces/{id}", savedWs.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedWs.getId())))
                .andExpect(jsonPath("$.name", equalTo("My Workspace")))
                .andExpect(jsonPath("$.ownerId", equalTo(FAKE_AUTHENTICATED_USER_ID)));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /workspaces - should return paginated and sorted results")
    void searchWorkspaces_shouldReturnPaginatedAndSortedResults() throws Exception {
        // Arrange
        workspaceJpaRepository.save(WorkspaceJpaEntity.fromModel(Workspace.create("Workspace C", "...", false, FAKE_AUTHENTICATED_USER_ID)));
        workspaceJpaRepository.save(WorkspaceJpaEntity.fromModel(Workspace.create("Workspace A", "...", false, FAKE_AUTHENTICATED_USER_ID)));
        workspaceJpaRepository.save(WorkspaceJpaEntity.fromModel(Workspace.create("Workspace B", "...", false, FAKE_AUTHENTICATED_USER_ID)));
        workspaceJpaRepository.flush();

        // Act & Assert
        mockMvc.perform(get("/workspaces")
                        .param("pageNumber", "0")
                        .param("pageSize", "2")
                        .param("sort", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("Workspace A"))
                .andExpect(jsonPath("$.items[1].name").value("Workspace B"));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /workspaces - should return paginated and filtered results")
    void searchWorkspaces_withFilters_shouldReturnFilteredResults() throws Exception {
        // Arrange
        UserJpaEntity anotherOwner = userJpaRepository.save(UserJpaEntity.fromModel(User.create("Another Owner", "another@owner.com", UserRole.ADMIN)));
        UserJpaEntity memberUser = userJpaRepository.save(UserJpaEntity.fromModel(User.create("Member One", "member1@test.com", UserRole.MEMBER)));
        userJpaRepository.flush();

        Workspace ws1 = Workspace.create("Public Alpha", "Alpha Desc", false, ownerUser.getId());
        workspaceJpaRepository.save(WorkspaceJpaEntity.fromModel(ws1));

        Workspace ws2 = Workspace.create("Private Beta", "Beta Desc", true, ownerUser.getId());
        workspaceJpaRepository.save(WorkspaceJpaEntity.fromModel(ws2));

        Workspace ws3 = Workspace.create("Public Gamma", "Gamma Desc", false, anotherOwner.getId());
        ws3.addMember(memberUser.getId());
        workspaceJpaRepository.save(WorkspaceJpaEntity.fromModel(ws3));
        workspaceJpaRepository.flush();

        // Act & Assert - Filter by name
        mockMvc.perform(get("/workspaces")
                        .param("name", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Public Alpha"));

        // Act & Assert - Filter by isPrivate
        mockMvc.perform(get("/workspaces")
                        .param("isPrivate", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Private Beta"));

        // Act & Assert - Filter by ownerId
        mockMvc.perform(get("/workspaces")
                        .param("ownerId", ownerUser.getId())
                        .param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("Private Beta"))
                .andExpect(jsonPath("$.items[1].name").value("Public Alpha"));

        // Act & Assert - Filter by memberId
        mockMvc.perform(get("/workspaces")
                        .param("memberId", memberUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Public Gamma"));

        // Act & Assert - Filter by multiple fields (name and isPrivate)
        mockMvc.perform(get("/workspaces")
                        .param("name", "Gamma")
                        .param("isPrivate", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Public Gamma"));

        // Act & Assert - Filter with no results
        mockMvc.perform(get("/workspaces")
                        .param("name", "Alpha")
                        .param("isPrivate", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("PUT /workspaces/{id} - should update an existing workspace")
    void updateWorkspace_shouldSucceed() throws Exception {
        // Arrange
        Workspace ws = Workspace.create("Old Name", "Old Desc", true, FAKE_AUTHENTICATED_USER_ID);
        WorkspaceJpaEntity savedWs = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(ws));
        var requestDto = new WorkspaceCreateUpdate("New Name", "New Desc", false);
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(put("/workspaces/{id}", savedWs.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<WorkspaceJpaEntity> updatedWs = workspaceJpaRepository.findById(savedWs.getId());
        assertThat(updatedWs).isPresent();
        assertThat(updatedWs.get().getName()).isEqualTo("New Name");
        assertThat(updatedWs.get().getDescription()).isEqualTo("New Desc");
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("DELETE /workspaces/{id} - should delete an existing workspace")
    void deleteWorkspace_shouldSucceed() throws Exception {
        // Arrange
        Workspace ws = Workspace.create("To Be Deleted", "...", false, FAKE_AUTHENTICATED_USER_ID);
        WorkspaceJpaEntity savedWs = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(ws));
        assertThat(workspaceJpaRepository.existsById(savedWs.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/workspaces/{id}", savedWs.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        assertThat(workspaceJpaRepository.existsById(savedWs.getId())).isFalse();
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("POST /workspaces/{id}/members/{memberId} - should add a member")
    void addMember_shouldSucceed() throws Exception {
        // Arrange
        UserJpaEntity member = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("New Member", "member@test.com", UserRole.MEMBER)));
        Workspace ws = Workspace.create("Team Workspace", "...", false, FAKE_AUTHENTICATED_USER_ID);
        WorkspaceJpaEntity savedWs = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(ws));

        // Act & Assert
        mockMvc.perform(post("/workspaces/{id}/members/{memberId}", savedWs.getId(), member.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<WorkspaceJpaEntity> updatedWs = workspaceJpaRepository.findById(savedWs.getId());
        assertThat(updatedWs).isPresent();
        assertThat(updatedWs.get().getMembers().stream().map(UserJpaEntity::getId).toList()).contains(member.getId());
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("DELETE /workspaces/{id}/members/{memberId} - should remove a member")
    void removeMember_shouldSucceed() throws Exception {
        // Arrange
        UserJpaEntity member = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Old Member", "old@test.com", UserRole.MEMBER)));
        Workspace ws = Workspace.create("Team Workspace", "...", false, FAKE_AUTHENTICATED_USER_ID);
        ws.addMember(member.getId());
        WorkspaceJpaEntity savedWs = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(ws));
        assertThat(savedWs.getMembers().stream().map(UserJpaEntity::getId).toList()).contains(member.getId());

        // Act & Assert
        mockMvc.perform(delete("/workspaces/{id}/members/{memberId}", savedWs.getId(), member.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<WorkspaceJpaEntity> updatedWs = workspaceJpaRepository.findById(savedWs.getId());
        assertThat(updatedWs).isPresent();
        assertThat(updatedWs.get().getMembers().stream().map(UserJpaEntity::getId).toList()).doesNotContain(member.getId());
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("PATCH /workspaces/{id}/owner/{newOwnerId} - should change the owner")
    void changeOwner_shouldSucceed() throws Exception {
        // Arrange
        UserJpaEntity newOwner = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("New Owner", "new.owner@test.com", UserRole.ADMIN)));
        Workspace ws = Workspace.create("Transferable WS", "...", false, FAKE_AUTHENTICATED_USER_ID);
        WorkspaceJpaEntity savedWs = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(ws));

        // Act & Assert
        mockMvc.perform(patch("/workspaces/{id}/owner/{newOwnerId}", savedWs.getId(), newOwner.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<WorkspaceJpaEntity> updatedWs = workspaceJpaRepository.findById(savedWs.getId());
        assertThat(updatedWs).isPresent();
        assertThat(updatedWs.get().getOwner().getId()).isEqualTo(newOwner.getId());
    }
}
