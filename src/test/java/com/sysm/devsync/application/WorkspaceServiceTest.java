package com.sysm.devsync.application;

import com.sysm.devsync.infrastructure.controller.dto.CreateResponse;
import com.sysm.devsync.infrastructure.controller.dto.request.WorkspaceCreateUpdate;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import com.sysm.devsync.domain.persistence.WorkspacePersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspacePersistencePort workspacePersistence;

    @Mock
    private UserPersistencePort userPersistence;

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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        Workspace mockExistingWorkspace = mock(Workspace.class);
        Set<String> members = new HashSet<>(Set.of(memberId, "otherMember"));
        when(mockExistingWorkspace.getMembersId()).thenReturn(members); // Return a modifiable set for the test
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.of(mockExistingWorkspace));
        doNothing().when(workspacePersistence).update(any(Workspace.class));

        // Act
        workspaceService.removeMemberFromWorkspace(workspaceId, memberId);

        // Assert
        verify(workspacePersistence, times(1)).findById(workspaceId);
        assertTrue(members.contains("otherMember"), "Other members should remain");
        assertFalse(members.contains(memberId), "Target member should be removed");
        verify(workspacePersistence, times(1)).update(mockExistingWorkspace);
    }

    @Test
    @DisplayName("removeMemberFromWorkspace should throw if workspace not found")
    void removeMemberFromWorkspace_shouldThrow_ifWorkspaceNotFound() {
        // Arrange
        when(workspacePersistence.findById(workspaceId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
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
        SearchQuery query = new SearchQuery(new Pageable(1, 10, "asc", "search"), "name");
        Pagination<Workspace> expectedPagination = new Pagination<>(1, 10, 0, Collections.emptyList());
        when(workspacePersistence.findAll(query)).thenReturn(expectedPagination);

        // Act
        Pagination<Workspace> actualPagination = workspaceService.getAllWorkspaces(query);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(workspacePersistence, times(1)).findAll(query);
    }
}
