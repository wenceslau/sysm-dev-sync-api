package com.sysm.devsync.application;

import com.sysm.devsync.domain.*;
import com.sysm.devsync.infrastructure.controllers.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controllers.dto.request.ProjectCreateUpdate;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.persistence.ProjectPersistencePort;
import com.sysm.devsync.domain.persistence.WorkspacePersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectPersistencePort projectPersistencePort;

    @Mock
    private WorkspacePersistencePort workspacePersistencePort;

    @InjectMocks
    private ProjectService projectService;

    private String projectId;
    private String workspaceId;
    private ProjectCreateUpdate projectCreateUpdateDto;
    private Project mockProject;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID().toString();
        workspaceId = UUID.randomUUID().toString();
        projectCreateUpdateDto = new ProjectCreateUpdate(
                "Test Project",
                "A project for testing",
                workspaceId
        );
        mockProject = mock(Project.class); // Used for methods that find and then operate on a project
    }

    @Test
    @DisplayName("createProject should create and save project when workspace exists")
    void createProject_shouldCreateAndSaveProject_whenWorkspaceExists() {
        // Arrange
        when(workspacePersistencePort.existsById(workspaceId)).thenReturn(true);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        // Assuming Project.create() returns a project with a generated ID
        // For simplicity, we'll let the real Project.create happen.
        // If Project.create had complex logic to mock, we'd use PowerMockito or refactor.

        // Act
        CreateResponse response = projectService.createProject(projectCreateUpdateDto);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());

        verify(workspacePersistencePort, times(1)).existsById(workspaceId);
        verify(projectPersistencePort, times(1)).create(projectCaptor.capture());

        Project capturedProject = projectCaptor.getValue();
        assertEquals(projectCreateUpdateDto.name(), capturedProject.getName());
        assertEquals(projectCreateUpdateDto.description(), capturedProject.getDescription());
        assertEquals(projectCreateUpdateDto.workspaceId(), capturedProject.getWorkspaceId());
        assertEquals(response.id(), capturedProject.getId()); // Ensure ID from response matches created project
    }

    @Test
    @DisplayName("createProject should throw IllegalArgumentException when workspace does not exist")
    void createProject_shouldThrowException_whenWorkspaceDoesNotExist() {
        // Arrange
        when(workspacePersistencePort.existsById(workspaceId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            projectService.createProject(projectCreateUpdateDto);
        });

        assertEquals("Workspace not found", exception.getMessage());
        verify(workspacePersistencePort, times(1)).existsById(workspaceId);
        verify(projectPersistencePort, never()).create(any(Project.class));
    }

    @Test
    @DisplayName("createProject should propagate IllegalArgumentException from Project.create for invalid data")
    void createProject_shouldPropagateException_forInvalidProjectData() {
        // Arrange
        ProjectCreateUpdate invalidDto = new ProjectCreateUpdate(null, "Description", workspaceId); // Invalid name
        when(workspacePersistencePort.existsById(workspaceId)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            projectService.createProject(invalidDto);
        });

        assertTrue(exception.getMessage().contains("Project name cannot be null or blank")); // Or exact message
        verify(workspacePersistencePort, times(1)).existsById(workspaceId);
        verify(projectPersistencePort, never()).create(any(Project.class));
    }

    @Test
    @DisplayName("updateProject should update existing project")
    void updateProject_shouldUpdateExistingProject() {
        // Arrange
        ProjectCreateUpdate updateDto = new ProjectCreateUpdate("Updated Name", "Updated Desc", workspaceId);
        when(projectPersistencePort.findById(projectId)).thenReturn(Optional.of(mockProject));

        // Act
        projectService.updateProject(projectId, updateDto);

        // Assert
        verify(projectPersistencePort, times(1)).findById(projectId);
        verify(mockProject, times(1)).update(updateDto.name(), updateDto.description());
        verify(projectPersistencePort, times(1)).update(mockProject);
    }

    @Test
    @DisplayName("updateProject should throw IllegalArgumentException when project not found")
    void updateProject_shouldThrowException_whenProjectNotFound() {
        // Arrange
        ProjectCreateUpdate updateDto = new ProjectCreateUpdate("Updated Name", "Updated Desc", workspaceId);
        when(projectPersistencePort.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            projectService.updateProject(projectId, updateDto);
        });

        assertEquals("Project not found", exception.getMessage());
        verify(projectPersistencePort, times(1)).findById(projectId);
        verify(projectPersistencePort, never()).update(any(Project.class));
    }

    @Test
    @DisplayName("changeWorkspace should update project's workspace when project and new workspace exist")
    void changeWorkspace_shouldUpdateProjectWorkspace_whenAllExist() {
        // Arrange
        String newWorkspaceId = UUID.randomUUID().toString();
        when(projectPersistencePort.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(workspacePersistencePort.existsById(newWorkspaceId)).thenReturn(true);

        // Act
        projectService.changeWorkspace(projectId, newWorkspaceId);

        // Assert
        verify(projectPersistencePort, times(1)).findById(projectId);
        verify(workspacePersistencePort, times(1)).existsById(newWorkspaceId);
        verify(mockProject, times(1)).changeWorkspace(newWorkspaceId);
        verify(projectPersistencePort, times(1)).update(mockProject);
    }

    @Test
    @DisplayName("changeWorkspace should throw IllegalArgumentException when project not found")
    void changeWorkspace_shouldThrowException_whenProjectNotFound() {
        // Arrange
        String newWorkspaceId = UUID.randomUUID().toString();
        when(projectPersistencePort.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            projectService.changeWorkspace(projectId, newWorkspaceId);
        });

        assertEquals("Project not found", exception.getMessage());
        verify(projectPersistencePort, times(1)).findById(projectId);
        verify(workspacePersistencePort, never()).existsById(anyString());
        verify(projectPersistencePort, never()).update(any(Project.class));
    }

    @Test
    @DisplayName("changeWorkspace should throw IllegalArgumentException when new workspace not found")
    void changeWorkspace_shouldThrowException_whenNewWorkspaceNotFound() {
        // Arrange
        String newWorkspaceId = UUID.randomUUID().toString();
        when(projectPersistencePort.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(workspacePersistencePort.existsById(newWorkspaceId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            projectService.changeWorkspace(projectId, newWorkspaceId);
        });

        assertEquals("Workspace not found", exception.getMessage());
        verify(projectPersistencePort, times(1)).findById(projectId);
        verify(workspacePersistencePort, times(1)).existsById(newWorkspaceId);
        verify(mockProject, never()).changeWorkspace(anyString());
        verify(projectPersistencePort, never()).update(any(Project.class));
    }

    @Test
    @DisplayName("deleteProject should call persistence port deleteById")
    void deleteProject_shouldCallPersistenceDeleteById() {
        // Arrange
        when(projectPersistencePort.existsById(projectId)).thenReturn(true);

        // Act
        assertThrows(BusinessException.class, () -> projectService.deleteProject(projectId));

        // Assert
        verify(projectPersistencePort, times(1)).existsById(projectId);
    }

    @Test
    @DisplayName("getProjectById should return project when found")
    void getProjectById_shouldReturnProject_whenFound() {
        // Arrange
        // Using a real project instance here for simplicity in assertion
        Project expectedProject = Project.create("Found Project", "Desc", workspaceId);
        when(projectPersistencePort.findById(projectId)).thenReturn(Optional.of(expectedProject));

        // Act
        Project actualProject = projectService.getProjectById(projectId);

        // Assert
        assertNotNull(actualProject);
        assertSame(expectedProject, actualProject); // Or assertEquals if Project has a proper equals method
        verify(projectPersistencePort, times(1)).findById(projectId);
    }

    @Test
    @DisplayName("getProjectById should throw IllegalArgumentException when project not found")
    void getProjectById_shouldThrowException_whenProjectNotFound() {
        // Arrange
        when(projectPersistencePort.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            projectService.getProjectById(projectId);
        });

        assertEquals("Project not found", exception.getMessage());
        verify(projectPersistencePort, times(1)).findById(projectId);
    }

    @Test
    @DisplayName("getAllProjects should return pagination result from persistence port")
    void getAllProjects_shouldReturnPaginationResult_fromPersistence() {
        // Arrange
        SearchQuery query = SearchQuery.of(new Page(1,10,  "asc", "search"),  Map.of());
        Pagination<Project> expectedPagination = new Pagination<>(1, 10, 0, Collections.emptyList());
        when(projectPersistencePort.findAll(query)).thenReturn(expectedPagination);

        // Act
        Pagination<Project> actualPagination = projectService.getAllProjects(query);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(projectPersistencePort, times(1)).findAll(query);
    }
}
