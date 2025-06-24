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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class WorkspaceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    // This ID is hardcoded in WorkspaceController, so we need a user with this ID for creation tests
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    @BeforeEach
    void setUp() {
        // Ensure the fake authenticated user exists for tests that need it
        if (!userJpaRepository.existsById(FAKE_AUTHENTICATED_USER_ID)) {
            UserJpaEntity owner = new UserJpaEntity();
            owner.setId(FAKE_AUTHENTICATED_USER_ID);
            owner.setName("Fake Owner");
            owner.setEmail("owner@fake.com");
            owner.setRole(UserRole.ADMIN);
            owner.setCreatedAt(Instant.now());
            owner.setUpdatedAt(Instant.now());
            userJpaRepository.saveAndFlush(owner);
        }
    }

    @Test
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
        var createdWorkspace = workspaceJpaRepository.findAll().get(0);
        assertThat(createdWorkspace.getName()).isEqualTo("DevSync Project");
        assertThat(createdWorkspace.getOwner().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
    }

    @Test
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

    @Test
    @DisplayName("GET /workspaces - should return paginated results")
    void searchWorkspaces_shouldReturnPaginatedResults() throws Exception {
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
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Workspace A"))
                .andExpect(jsonPath("$.items[1].name").value("Workspace B"));
    }
}
