package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.infrastructure.controllers.dto.request.ProjectCreateUpdate;
import com.sysm.devsync.infrastructure.repositories.ProjectJpaRepository;
import com.sysm.devsync.infrastructure.repositories.UserJpaRepository;
import com.sysm.devsync.infrastructure.repositories.WorkspaceJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.ProjectJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProjectIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectJpaRepository projectJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private WorkspaceJpaEntity testWorkspace;
    private UserJpaEntity testUser;

    @BeforeEach
    void setUp() {
        // A project needs a user and a workspace, so we create them first.
        projectJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        testUser = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Test User", "user@test.com", UserRole.ADMIN)));
        Workspace ws = Workspace.create("Test Workspace", "A workspace for tests", false, testUser.getId());
        testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(ws));
    }

    @Test
    @DisplayName("POST /projects - should create a new project successfully")
    void createProject_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new ProjectCreateUpdate("New Awesome Project", "A great project description", testWorkspace.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(header().exists("Location"));

        // Verify DB state
        var createdProject = projectJpaRepository.findAll().get(0);
        assertThat(createdProject.getName()).isEqualTo("New Awesome Project");
        assertThat(createdProject.getWorkspace().getId()).isEqualTo(testWorkspace.getId());
    }

    @Test
    @DisplayName("POST /projects - should fail with 400 for invalid data (blank name)")
    void createProject_withInvalidData_shouldFail() throws Exception {
        // Arrange: Name is blank
        var requestDto = new ProjectCreateUpdate("", "Description", testWorkspace.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name[0]", equalTo("Project name must not be blank")));
    }

    @Test
    @DisplayName("POST /projects - should fail with 404 if workspace does not exist")
    void createProject_withNonExistentWorkspace_shouldFail() throws Exception {
        // Arrange
        var nonExistentWorkspaceId = "00000000-0000-0000-0000-000000000000";
        var requestDto = new ProjectCreateUpdate("Project with Bad WS", "Desc", nonExistentWorkspaceId);
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Workspace not found")));
    }

    @Test
    @DisplayName("GET /projects/{id} - should retrieve an existing project")
    void getProjectById_shouldSucceed() throws Exception {
        // Arrange
        var project = ProjectJpaEntity.fromModel(com.sysm.devsync.domain.models.Project.create("Find Me", "Desc", testWorkspace.getId()));
        project.setWorkspace(testWorkspace);
        var savedProject = projectJpaRepository.saveAndFlush(project);

        // Act & Assert
        mockMvc.perform(get("/projects/{id}", savedProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedProject.getId())))
                .andExpect(jsonPath("$.name", equalTo("Find Me")))
                .andExpect(jsonPath("$.workspaceId", equalTo(testWorkspace.getId())));
    }

    @Test
    @DisplayName("PUT /projects/{id} - should update an existing project")
    void updateProject_shouldSucceed() throws Exception {
        // Arrange
        var project = ProjectJpaEntity.fromModel(com.sysm.devsync.domain.models.Project.create("Old Name", "Old Desc", testWorkspace.getId()));
        project.setWorkspace(testWorkspace);
        var savedProject = projectJpaRepository.saveAndFlush(project);

        var requestDto = new ProjectCreateUpdate("New Name", "New Desc", testWorkspace.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(put("/projects/{id}", savedProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<ProjectJpaEntity> updatedProject = projectJpaRepository.findById(savedProject.getId());
        assertThat(updatedProject).isPresent();
        assertThat(updatedProject.get().getName()).isEqualTo("New Name");
        assertThat(updatedProject.get().getDescription()).isEqualTo("New Desc");
    }

    @Test
    @DisplayName("DELETE /projects/{id} - should delete an existing project")
    void deleteProject_shouldSucceed() throws Exception {
        // Arrange
        var project = ProjectJpaEntity.fromModel(com.sysm.devsync.domain.models.Project.create("To Be Deleted", "Desc", testWorkspace.getId()));
        project.setWorkspace(testWorkspace);
        var savedProject = projectJpaRepository.saveAndFlush(project);
        assertThat(projectJpaRepository.existsById(savedProject.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/projects/{id}", savedProject.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        assertThat(projectJpaRepository.existsById(savedProject.getId())).isFalse();
    }

    @Test
    @DisplayName("PATCH /projects/{id}/workspace/{workspaceId} - should change the project's workspace")
    void changeWorkspace_shouldSucceed() throws Exception {
        // Arrange
        // Create a second workspace to move the project to
        Workspace newWsDomain = Workspace.create("New Target Workspace", "...", false, testUser.getId());
        WorkspaceJpaEntity newWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(newWsDomain));

        var project = ProjectJpaEntity.fromModel(com.sysm.devsync.domain.models.Project.create("Movable Project", "Desc", testWorkspace.getId()));
        project.setWorkspace(testWorkspace);
        var savedProject = projectJpaRepository.saveAndFlush(project);

        // Act & Assert
        mockMvc.perform(patch("/projects/{id}/workspace/{workspaceId}", savedProject.getId(), newWorkspace.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<ProjectJpaEntity> updatedProject = projectJpaRepository.findById(savedProject.getId());
        assertThat(updatedProject).isPresent();
        assertThat(updatedProject.get().getWorkspace().getId()).isEqualTo(newWorkspace.getId());
    }
}
