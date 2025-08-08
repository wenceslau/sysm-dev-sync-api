package com.sysm.devsync.application;

import com.sysm.devsync.domain.*;
import com.sysm.devsync.domain.persistence.ProjectPersistencePort;
import com.sysm.devsync.infrastructure.controllers.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controllers.dto.request.WorkspaceCreateUpdate;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import com.sysm.devsync.domain.persistence.WorkspacePersistencePort;
import com.sysm.devsync.infrastructure.controllers.dto.response.WorkspaceResponse;
import com.sysm.devsync.infrastructure.repositories.objects.CountProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspacePersistencePort workspacePersistence;

    @Mock
    private UserPersistencePort userPersistence;

    @Mock
    private ProjectPersistencePort projectPersistence;

    @InjectMocks
    private WorkspaceService workspaceService;

    private WorkspaceCreateUpdate validWorkspaceCreateUpdateDto;
    private String workspaceId;
    private String ownerId;
    private String memberId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID().toString();
        ownerId = UUID.randomUUID().toString();
        memberId = UUID.randomUUID().toString();
        validWorkspaceCreateUpdateDto = new WorkspaceCreateUpdate(
                "Test Workspace",
                "A workspace for testing purposes",
                false
        );
    }

    // --- createWorkspace Tests ---
    @Test
    @DisplayName("createWorkspace should create and save workspace successfully")
    void createWorkspace_shouldCreateAndSaveWorkspaceSuccessfully() {
        // Arrange
        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        doNothing().when(workspacePersistence).create(workspaceCaptor.capture());
        when(userPersistence.existsById(ownerId)).thenReturn(true);

        // Act
        CreateResponse response = workspaceService.createWorkspace(validWorkspaceCreateUpdateDto, ownerId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());

        verify(workspacePersistence, times(1)).create(any(Workspace.class));
        Workspace capturedWorkspace = workspaceCaptor.getValue();

        assertEquals(validWorkspaceCreateUpdateDto.name(), capturedWorkspace.getName());
        assertEquals(validWorkspaceCreateUpdateDto.description(), capturedWorkspace.getDescription());
        assertEquals(validWorkspaceCreateUpdateDto.isPrivate(), capturedWorkspace.isPrivate());
        assertEquals(ownerId, capturedWorkspace.getOwnerId());
        assertEquals(response.id(), capturedWorkspace.getId());
    }

    @Test
    @DisplayName("createWorkspace should propagate IllegalArgumentException from Workspace.create for invalid name")
    void createWorkspace_shouldPropagateException_forInvalidName() {
        // Arrange
        WorkspaceCreateUpdate invalidDto = new WorkspaceCreateUpdate(null, "Desc", false);
        when(userPersistence.existsById(ownerId)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            workspaceService.createWorkspace(invalidDto, ownerId);
        });
        assertEquals("Workspace name cannot be null or blank", exception.getMessage());
        verify(workspacePersistence, never()).create(any());
    }

    // --- updateWorkspace Tests ---
    @Test
    @DisplayName("updateWorkspace should update existing workspace successfully")
    void updateWorkspace_shouldUpdateExistingWorkspaceSuccessfully() {
        // Arrange
        Workspace mockExistingWorkspace = mock(Workspace.class);
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(mockExistingWorkspace));
        doNothing().when(workspacePersistence).update(any(Workspace.class));

        WorkspaceCreateUpdate updateDto = new WorkspaceCreateUpdate("New Name", "New Description", true); // isPrivate is not used by update

        // Act
        workspaceService.updateWorkspace(workspaceId, updateDto);

        // Assert
        verify(workspacePersistence, times(1)).findById(workspaceId);
        verify(mockExistingWorkspace, times(1)).update(
                updateDto.name(),
                updateDto.description()
        );
        verify(workspacePersistence, times(1)).update(mockExistingWorkspace);
    }

    @Test
    @DisplayName("updateWorkspace should throw IllegalArgumentException if workspace not found")
    void updateWorkspace_shouldThrowException_ifWorkspaceNotFound() {
        // Arrange
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.updateWorkspace(workspaceId, validWorkspaceCreateUpdateDto);
        });
        assertEquals("Workspace not found", exception.getMessage());
        verify(workspacePersistence, never()).update(any());
    }

    // --- changeWorkspacePrivacy Tests ---
    @Test
    @DisplayName("changeWorkspacePrivacy should update privacy and save workspace")
    void changeWorkspacePrivacy_shouldUpdatePrivacyAndSaveWorkspace() {
        // Arrange
        Workspace mockExistingWorkspace = mock(Workspace.class);
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(mockExistingWorkspace));
        doNothing().when(workspacePersistence).update(any(Workspace.class));

        // Act
        workspaceService.changeWorkspacePrivacy(workspaceId, true);

        // Assert
        verify(workspacePersistence, times(1)).findById(workspaceId);
        verify(mockExistingWorkspace, times(1)).setPrivate(true);
        verify(workspacePersistence, times(1)).update(mockExistingWorkspace);
    }

    @Test
    @DisplayName("changeWorkspacePrivacy should throw IllegalArgumentException if workspace not found")
    void changeWorkspacePrivacy_shouldThrowException_ifWorkspaceNotFound() {
        // Arrange
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.changeWorkspacePrivacy(workspaceId, true);
        });
        assertEquals("Workspace not found", exception.getMessage());
        verify(workspacePersistence, never()).update(any());
    }

    // --- addMemberToWorkspace Tests ---
    @Test
    @DisplayName("addMemberToWorkspace should add member and save workspace")
    void addMemberToWorkspace_shouldAddMemberAndSaveWorkspace() {
        // Arrange
        Workspace mockExistingWorkspace = mock(Workspace.class);
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(mockExistingWorkspace));
        when(userPersistence.existsById(memberId)).thenReturn(true); // Simulate user exists
        doNothing().when(workspacePersistence).update(any(Workspace.class));

        // Act
        workspaceService.addMemberToWorkspace(workspaceId, memberId);

        // Assert
        verify(workspacePersistence, times(1)).findById(workspaceId);
        verify(userPersistence, times(1)).existsById(memberId);
        verify(mockExistingWorkspace, times(1)).addMember(memberId);
        verify(workspacePersistence, times(1)).update(mockExistingWorkspace);
    }

    @Test
    @DisplayName("addMemberToWorkspace should throw if workspace not found")
    void addMemberToWorkspace_shouldThrow_ifWorkspaceNotFound() {
        // Arrange
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.addMemberToWorkspace(workspaceId, memberId);
        });
        assertEquals("Workspace not found", exception.getMessage());
        verify(userPersistence, never()).existsById(any());
        verify(workspacePersistence, never()).update(any());
    }

    @Test
    @DisplayName("addMemberToWorkspace should throw if member not found")
    void addMemberToWorkspace_shouldThrow_ifMemberNotFound() {
        // Arrange
        Workspace mockExistingWorkspace = mock(Workspace.class);
        when(workspacePersistence.findById(anyString())).thenReturn(Optional.of(mockExistingWorkspace));
        when(userPersistence.existsById(memberId)).thenReturn(false); // Simulate user does not exist

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.addMemberToWorkspace(workspaceId, memberId);
        });
        assertEquals("Member not found", exception.getMessage());
        verify(mockExistingWorkspace, never()).addMember(any());
        verify(workspacePersistence, never()).update(any());
    }

    // --- removeMemberFromWorkspace Tests ---
    @Test
    @DisplayName("removeMemberFromWorkspace should remove member and save workspace")
    void removeMemberFromWorkspace_shouldRemoveMemberAndSaveWorkspace() {
        // Arrange
        // Rewrite the test
    }

    @Test
    @DisplayName("removeMemberFromWorkspace should throw if workspace not found")
    void removeMemberFromWorkspace_shouldThrow_ifWorkspaceNotFound() {
        // Arrange
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.removeMemberFromWorkspace(workspaceId, memberId);
        });
        assertEquals("Workspace not found", exception.getMessage());
        verify(workspacePersistence, never()).update(any());
    }

    @Test
    @DisplayName("removeMemberFromWorkspace should throw if member not found in workspace")
    void removeMemberFromWorkspace_shouldThrow_ifMemberNotInWorkspace() {
        // Arrange
        Workspace mockExistingWorkspace = mock(Workspace.class);
        when(mockExistingWorkspace.getMembersId()).thenReturn(new HashSet<>(Set.of("otherMember"))); // Member not present
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(mockExistingWorkspace));

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.removeMemberFromWorkspace(workspaceId, memberId);
        });
        assertEquals("Member not found in workspace", exception.getMessage());
        verify(workspacePersistence, never()).update(any());
    }

    // --- changeOwnerOfWorkspace Tests ---
    @Test
    @DisplayName("changeOwnerOfWorkspace should change owner and save workspace")
    void changeOwnerOfWorkspace_shouldChangeOwnerAndSaveWorkspace() {
        // Arrange
        Workspace mockExistingWorkspace = mock(Workspace.class);
        String newOwnerId = UUID.randomUUID().toString();
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(mockExistingWorkspace));
        when(userPersistence.existsById(newOwnerId)).thenReturn(true);
        doNothing().when(workspacePersistence).update(any(Workspace.class));

        // Act
        workspaceService.changeOwnerOfWorkspace(workspaceId, newOwnerId);

        // Assert
        verify(workspacePersistence, times(1)).findById(workspaceId);
        verify(userPersistence, times(1)).existsById(newOwnerId);
        verify(mockExistingWorkspace, times(1)).changeOwner(newOwnerId);
        verify(workspacePersistence, times(1)).update(mockExistingWorkspace);
    }

    @Test
    @DisplayName("changeOwnerOfWorkspace should throw if workspace not found")
    void changeOwnerOfWorkspace_shouldThrow_ifWorkspaceNotFound() {
        // Arrange
        String newOwnerId = UUID.randomUUID().toString();
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.changeOwnerOfWorkspace(workspaceId, newOwnerId);
        });
        assertEquals("Workspace not found", exception.getMessage());
        verify(userPersistence, never()).existsById(any());
        verify(workspacePersistence, never()).update(any());
    }

    @Test
    @DisplayName("changeOwnerOfWorkspace should throw if new owner not found")
    void changeOwnerOfWorkspace_shouldThrow_ifNewOwnerNotFound() {
        // Arrange
        Workspace mockExistingWorkspace = mock(Workspace.class);
        String newOwnerId = UUID.randomUUID().toString();
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(mockExistingWorkspace));
        when(userPersistence.existsById(newOwnerId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.changeOwnerOfWorkspace(workspaceId, newOwnerId);
        });
        assertEquals("New owner not found", exception.getMessage());
        verify(mockExistingWorkspace, never()).changeOwner(any());
        verify(workspacePersistence, never()).update(any());
    }

    // --- deleteWorkspace Tests ---
    @Test
    @DisplayName("deleteWorkspace should call repository deleteById")
    void deleteWorkspace_shouldCallRepositoryDeleteById() {
        // Arrange
        when(workspacePersistence.existsById(workspaceId)).thenReturn(true);
        when(workspacePersistence.hasMembers(workspaceId)).thenReturn(false);
        when(projectPersistence.existsByWorkspaceId(workspaceId)).thenReturn(false);
        doNothing().when(workspacePersistence).deleteById(workspaceId);

        // Act
        workspaceService.deleteWorkspace(workspaceId);

        // Assert
        verify(workspacePersistence, times(1)).deleteById(workspaceId);
    }

    // --- getWorkspaceById Tests ---
    @Test
    @DisplayName("getWorkspaceById should return workspace if found")
    void getWorkspaceById_shouldReturnWorkspace_ifFound() {
        // Arrange
        Workspace expectedWorkspace = Workspace.create("Test", "Desc", false, ownerId);
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(expectedWorkspace));

        // Act
        Workspace actualWorkspace = workspaceService.getWorkspaceById(workspaceId);

        // Assert
        assertNotNull(actualWorkspace);
        assertSame(expectedWorkspace, actualWorkspace);
        verify(workspacePersistence, times(1)).findById(workspaceId);
    }

    @Test
    @DisplayName("getWorkspaceById should throw IllegalArgumentException if workspace not found")
    void getWorkspaceById_shouldThrowException_ifWorkspaceNotFound() {
        // Arrange
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            workspaceService.getWorkspaceById(workspaceId);
        });
        assertEquals("Workspace not found", exception.getMessage());
        verify(workspacePersistence, times(1)).findById(workspaceId);
    }

    // --- getAllWorkspaces Tests ---
    @Test
    @DisplayName("getAllWorkspaces should return pagination result from repository")
    void getAllWorkspaces_shouldReturnPaginationResult_fromRepository() {
        // Arrange
        SearchQuery query = SearchQuery.of(new Page(1, 10, "asc", "search"),  Map.of());
        Pagination<Workspace> expectedPagination = new Pagination<>(1, 10, 0, Collections.emptyList());
        when(workspacePersistence.findAll(query)).thenReturn(expectedPagination);

        // Act
        Pagination<WorkspaceResponse> actualPagination = workspaceService.getAllWorkspaces(query);

        // Assert
        assertNotNull(actualPagination);
        assertEquals(expectedPagination.total(), actualPagination.total());
        verify(workspacePersistence, times(1)).findAll(query);
    }

    @Test
    @DisplayName("getWorkspaceById should return workspace when found")
    void getWorkspaceById_shouldReturnWorkspace_whenFound() {

        workspaceId = UUID.randomUUID().toString();
        var workspace = Workspace.build(workspaceId, Instant.now(), Instant.now(), "Test Workspace", "A test workspace", false, "owner123", Collections.emptySet());

        // Arrange
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(workspace));

        // Act
        Workspace foundWorkspace = workspaceService.getWorkspaceById(workspaceId);

        // Assert
        assertNotNull(foundWorkspace);
        assertEquals(workspaceId, foundWorkspace.getId());
        assertEquals("Test Workspace", foundWorkspace.getName());
        verify(workspacePersistence, times(1)).findById(workspaceId);
    }

    @Test
    @DisplayName("getWorkspaceById should throw NotFoundException when not found")
    void getWorkspaceById_shouldThrowNotFoundException_whenNotFound() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(workspacePersistence.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> workspaceService.getWorkspaceById(nonExistentId));

        assertEquals("Workspace not found", exception.getMessage());
        assertEquals(nonExistentId, exception.getId());
        verify(workspacePersistence, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAllWorkspaces should return paginated workspaces with project counts")
    void getAllWorkspaces_shouldReturnPaginatedWorkspaces_withProjectCounts() {
        // Arrange
        workspaceId = UUID.randomUUID().toString();
        var workspace = Workspace.build(workspaceId, Instant.now(), Instant.now(), "Test Workspace", "A test workspace", false, "owner123", Collections.emptySet());

        SearchQuery query = SearchQuery.of(Page.of(0, 10, "name", "asc"), Collections.emptyMap());
        List<Workspace> workspaces = List.of(workspace);
        Pagination<Workspace> workspacePage = new Pagination<>(0, 10, 1, workspaces);
        List<CountProject> projectCounts = List.of(new CountProject(workspace.getId(), 5));


        when(workspacePersistence.findAll(query)).thenReturn(workspacePage);
        when(projectPersistence.countProjectsByWorkspaceIdIn(List.of(workspace.getId()))).thenReturn(projectCounts);

        // Act
        Pagination<WorkspaceResponse> result = workspaceService.getAllWorkspaces(query);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.total());
        assertEquals(1, result.items().size());
        assertEquals(5, result.items().get(0).projectCount());
        assertEquals(workspace.getName(), result.items().get(0).name());

        verify(workspacePersistence, times(1)).findAll(query);
        verify(projectPersistence, times(1)).countProjectsByWorkspaceIdIn(List.of(workspace.getId()));
    }

    @Test
    @DisplayName("getAllWorkspaces should return empty pagination and not query for counts when no workspaces are found")
    void getAllWorkspaces_shouldReturnEmptyPagination_whenNoWorkspacesFound() {
        // Arrange
        SearchQuery query = SearchQuery.of(Page.of(0, 10, "name", "asc"), Collections.emptyMap());
        Pagination<Workspace> emptyPage = new Pagination<>(0, 10, 0, Collections.emptyList());
        when(workspacePersistence.findAll(query)).thenReturn(emptyPage);

        // Act
        Pagination<WorkspaceResponse> result = workspaceService.getAllWorkspaces(query);

        // Assert
        assertNotNull(result);
        assertTrue(result.items().isEmpty());
        verify(workspacePersistence, times(1)).findAll(query);
        verify(projectPersistence, never()).countProjectsByWorkspaceIdIn(any());
    }


    @Nested
    @DisplayName("deleteWorkspace Tests")
    class DeleteWorkspaceTests {

        @Test
        @DisplayName("should throw BusinessException when workspace has members")
        void deleteWorkspace_shouldThrowBusinessException_whenWorkspaceHasMembers() {
            // Arrange
            when(workspacePersistence.existsById(workspaceId)).thenReturn(true);
            when(workspacePersistence.hasMembers(workspaceId)).thenReturn(true); // Simulate members exist

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                workspaceService.deleteWorkspace(workspaceId);
            });

            assertEquals("Cannot delete a workspace that has members. Please remove all members first.", exception.getMessage());
            verify(workspacePersistence, never()).deleteById(anyString()); // Ensure delete is never called
        }

        @Test
        @DisplayName("should throw BusinessException when workspace has projects")
        void deleteWorkspace_shouldThrowBusinessException_whenWorkspaceHasProjects() {
            // Arrange
            when(workspacePersistence.existsById(workspaceId)).thenReturn(true);
            when(workspacePersistence.hasMembers(workspaceId)).thenReturn(false); // No members
            when(projectPersistence.existsByWorkspaceId(workspaceId)).thenReturn(true); // Simulate projects exist

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                workspaceService.deleteWorkspace(workspaceId);
            });

            assertEquals("Cannot delete a workspace that has associated projects. Please move or delete them first.", exception.getMessage());
            verify(workspacePersistence, never()).deleteById(anyString()); // Ensure delete is never called
        }

        @Test
        @DisplayName("should call repository deleteById when workspace is empty")
        void deleteWorkspace_shouldCallRepositoryDeleteById_whenWorkspaceIsEmpty() {
            // Arrange
            when(workspacePersistence.existsById(workspaceId)).thenReturn(true);
            when(workspacePersistence.hasMembers(workspaceId)).thenReturn(false);
            when(projectPersistence.existsByWorkspaceId(workspaceId)).thenReturn(false);
            doNothing().when(workspacePersistence).deleteById(workspaceId);

            // Act
            workspaceService.deleteWorkspace(workspaceId);

            // Assert
            verify(workspacePersistence, times(1)).deleteById(workspaceId);
        }
    }
}
