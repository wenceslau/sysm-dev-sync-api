package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Project;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProjectIntegrationTest extends AbstractIntegrationTest {

    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    @Autowired
    private ProjectJpaRepository projectJpaRepository;
    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;

    private WorkspaceJpaEntity workspace1;
    private WorkspaceJpaEntity workspace2;

    @BeforeEach
    void setUp() {
        // Clean repositories to ensure test isolation
        projectJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // Setup prerequisite data
        User owner = User.create("Owner", "owner@example.com", UserRole.ADMIN);
        UserJpaEntity ownerJpa = UserJpaEntity.fromModel(owner);
        ownerJpa.setId(FAKE_AUTHENTICATED_USER_ID);
        userJpaRepository.save(ownerJpa);

        Workspace ws1 = Workspace.create("Workspace One", "Description one", false, ownerJpa.getId());
        workspace1 = WorkspaceJpaEntity.fromModel(ws1);
        workspaceJpaRepository.save(workspace1);

        Workspace ws2 = Workspace.create("Workspace Two", "Description two", true, ownerJpa.getId());
        workspace2 = WorkspaceJpaEntity.fromModel(ws2);
        workspaceJpaRepository.save(workspace2);

        workspaceJpaRepository.flush();
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("POST /projects - should create a new project successfully")
    void createProject_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new ProjectCreateUpdate("New Project", "A great new project", workspace1.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        var responseContent = mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        var createdProjectId = objectMapper.readTree(responseContent).get("id").asText();

        // Verify in DB
        Optional<ProjectJpaEntity> createdProject = projectJpaRepository.findById(createdProjectId);
        assertThat(createdProject).isPresent();
        assertThat(createdProject.get().getName()).isEqualTo("New Project");
        assertThat(createdProject.get().getWorkspace().getId()).isEqualTo(workspace1.getId());
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /projects/{id} - should retrieve an existing project")
    void getProjectById_shouldSucceed() throws Exception {
        // Arrange
        Project project = Project.create("Existing Project", "Desc", workspace1.getId());
        projectJpaRepository.save(ProjectJpaEntity.fromModel(project));

        // Act & Assert
        mockMvc.perform(get("/projects/{id}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Existing Project"))
                .andExpect(jsonPath("$.workspaceId").value(workspace1.getId()));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /projects - should return paginated and filtered projects")
    void searchProjects_withFilters_shouldReturnFilteredResults() throws Exception {
        // Arrange
        projectJpaRepository.save(ProjectJpaEntity.fromModel(Project.create("Project Alpha", "Alpha Desc", workspace1.getId())));
        projectJpaRepository.save(ProjectJpaEntity.fromModel(Project.create("Project Beta", "Beta Desc", workspace2.getId())));
        projectJpaRepository.save(ProjectJpaEntity.fromModel(Project.create("Project Gamma", "Gamma Desc", workspace1.getId())));
        projectJpaRepository.flush();

        // Act & Assert - Filter by name
        mockMvc.perform(get("/projects")
                        .param("name", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Project Alpha"));

        // Act & Assert - Filter by workspaceId
        mockMvc.perform(get("/projects")
                        .param("workspaceId", workspace1.getId())
                        .param("sort", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("Project Alpha"))
                .andExpect(jsonPath("$.items[1].name").value("Project Gamma"));

        // Act & Assert - Filter by multiple fields (name and workspaceId)
        mockMvc.perform(get("/projects")
                        .param("name", "Gamma")
                        .param("workspaceId", workspace1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Project Gamma"));

        // Act & Assert - Filter with no results
        mockMvc.perform(get("/projects")
                        .param("name", "Alpha")
                        .param("workspaceId", workspace2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("PUT /projects/{id} - should update an existing project")
    void updateProject_shouldSucceed() throws Exception {
        // Arrange
        Project project = Project.create("Initial Name", "Initial Desc", workspace1.getId());
        projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(project));

        var updateDto = new ProjectCreateUpdate("Updated Name", "Updated Desc", workspace1.getId());
        var requestJson = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/projects/{id}", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify in DB
        Optional<ProjectJpaEntity> updatedProject = projectJpaRepository.findById(project.getId());
        assertThat(updatedProject).isPresent();
        assertThat(updatedProject.get().getName()).isEqualTo("Updated Name");
        assertThat(updatedProject.get().getDescription()).isEqualTo("Updated Desc");
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("PATCH /projects/{id}/workspace - should change the workspace of a project")
    void changeProjectWorkspace_shouldSucceed() throws Exception {
        // Arrange
        Project project = Project.create("Movable Project", "Desc", workspace1.getId());
        projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(project));

        // Act & Assert
        mockMvc.perform(patch("/projects/{id}/workspace/{workspaceId}",
                        project.getId(), workspace2.getId()))
                .andExpect(status().isNoContent());

        // Verify in DB
        Optional<ProjectJpaEntity> updatedProject = projectJpaRepository.findById(project.getId());
        assertThat(updatedProject).isPresent();
        assertThat(updatedProject.get().getWorkspace().getId()).isEqualTo(workspace2.getId());
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("DELETE /projects/{id} - should delete an existing project")
    void deleteProject_shouldSucceed() throws Exception {
        // Arrange
        Project project = Project.create("Deletable Project", "Desc", workspace1.getId());
        projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(project));
        String projectId = project.getId();

        assertThat(projectJpaRepository.existsById(projectId)).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/projects/{id}", projectId))
                .andExpect(status().isBadRequest());

    }
}
