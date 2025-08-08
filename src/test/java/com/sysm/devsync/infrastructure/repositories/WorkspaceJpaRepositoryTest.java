package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.Utils;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import com.sysm.devsync.domain.enums.UserRole; // For creating UserJpaEntity
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WorkspaceJpaRepositoryTest extends AbstractRepositoryTest {

    private UserJpaEntity ownerUser1;
    private UserJpaEntity memberUser1;
    private UserJpaEntity memberUser2;

    private WorkspaceJpaEntity workspace1;
    private WorkspaceJpaEntity workspace2;
    private WorkspaceJpaEntity workspace3;

    @BeforeEach
    void setUp() {
        clearRepositories();

        // Create Users
        ownerUser1 = new UserJpaEntity();
        ownerUser1.setId(UUID.randomUUID().toString());
        ownerUser1.setName("Owner One");
        ownerUser1.setEmail("owner1@example.com");
        ownerUser1.setRole(UserRole.ADMIN);
        ownerUser1.setCreatedAt(LocalDateTime.now().minusHours(2).toInstant(ZoneOffset.UTC)); // Set explicitly for clarity
        ownerUser1.setUpdatedAt(LocalDateTime.now().minusHours(1).toInstant(ZoneOffset.UTC));
        entityManager.persist(ownerUser1);

        memberUser1 = new UserJpaEntity();
        memberUser1.setId(UUID.randomUUID().toString());
        memberUser1.setName("Member One");
        memberUser1.setEmail("member1@example.com");
        memberUser1.setRole(UserRole.MEMBER);
        memberUser1.setCreatedAt(LocalDateTime.now().minusHours(2).toInstant(ZoneOffset.UTC));
        memberUser1.setUpdatedAt(LocalDateTime.now().minusHours(1).toInstant(ZoneOffset.UTC));
        entityManager.persist(memberUser1);

        memberUser2 = new UserJpaEntity();
        memberUser2.setId(UUID.randomUUID().toString());
        memberUser2.setName("Member Two");
        memberUser2.setEmail("member2@example.com");
        memberUser2.setRole(UserRole.MEMBER);
        memberUser2.setCreatedAt(LocalDateTime.now().minusHours(2).toInstant(ZoneOffset.UTC));
        memberUser2.setUpdatedAt(LocalDateTime.now().minusHours(1).toInstant(ZoneOffset.UTC));
        entityManager.persist(memberUser2);

        entityManager.flush(); // Ensure users are in DB before workspaces reference them

        // Create Workspace DTOs for setup (timestamps will be set by @PrePersist)
        workspace1 = new WorkspaceJpaEntity();
        workspace1.setId(UUID.randomUUID().toString());
        workspace1.setName("Workspace Alpha");
        workspace1.setDescription("Description for Alpha");
        workspace1.setOwner(ownerUser1);
        workspace1.setPrivate(false);
        workspace1.getMembers().add(memberUser1);
        workspace1.setCreatedAt(Instant.now());
        workspace1.setUpdatedAt(Instant.now());

        workspace2 = new WorkspaceJpaEntity();
        workspace2.setId(UUID.randomUUID().toString());
        workspace2.setName("Workspace Beta");
        workspace2.setDescription("Description for Beta");
        workspace2.setOwner(ownerUser1);
        workspace2.setPrivate(true);
        workspace2.getMembers().add(memberUser1);
        workspace2.getMembers().add(memberUser2);
        workspace2.setCreatedAt(Instant.now());
        workspace2.setUpdatedAt(Instant.now());

        workspace3 = new WorkspaceJpaEntity();
        workspace3.setId(UUID.randomUUID().toString());
        workspace3.setName("Workspace Gamma");
        workspace3.setDescription("Public workspace Gamma");
        workspace3.setOwner(memberUser1); // Different owner
        workspace3.setPrivate(false);
        workspace3.setCreatedAt(Instant.now());
        workspace3.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("should save a workspace and find it by id")
    void saveAndFindById() {
        // Act
        WorkspaceJpaEntity savedWorkspace = workspaceJpaRepository.save(workspace1);
        entityManager.flush(); // Ensure save is committed
        entityManager.clear(); // Ensure we fetch fresh from DB

        Optional<WorkspaceJpaEntity> foundWorkspaceOpt = workspaceJpaRepository.findById(savedWorkspace.getId());

        // Assert
        assertThat(foundWorkspaceOpt).isPresent();
        WorkspaceJpaEntity foundWorkspace = foundWorkspaceOpt.get();
        assertThat(foundWorkspace.getName()).isEqualTo(workspace1.getName());
        assertThat(foundWorkspace.getOwner().getId()).isEqualTo(ownerUser1.getId());
        assertThat(foundWorkspace.getMembers()).hasSize(1);
        assertThat(foundWorkspace.getMembers().iterator().next().getId()).isEqualTo(memberUser1.getId());
        assertThat(foundWorkspace.getCreatedAt()).isNotNull(); // Set by @PrePersist
        assertThat(foundWorkspace.getUpdatedAt()).isNotNull(); // Set by @PrePersist
    }

    @Test
    @DisplayName("should fail to save workspace with duplicate name due to unique constraint")
    void save_withDuplicateName_shouldFail() {
        // Arrange
        workspaceJpaRepository.save(workspace1); // Save the first workspace
        entityManager.flush();

        WorkspaceJpaEntity duplicateNameWorkspace = new WorkspaceJpaEntity();
        duplicateNameWorkspace.setId(UUID.randomUUID().toString());
        duplicateNameWorkspace.setName(workspace1.getName()); // Same name
        duplicateNameWorkspace.setDescription("Another description");
        duplicateNameWorkspace.setOwner(memberUser1); // Different owner is fine

        // Act & Assert
        assertThatThrownBy(() -> {
            workspaceJpaRepository.save(duplicateNameWorkspace);
            entityManager.flush(); // This will trigger the constraint violation
        }).isInstanceOf(ConstraintViolationException.class);
    }


    @Test
    @DisplayName("should return empty optional when finding non-existent workspace by id")
    void findById_whenWorkspaceDoesNotExist_shouldReturnEmpty() {
        // Act
        Optional<WorkspaceJpaEntity> foundWorkspace = workspaceJpaRepository.findById(UUID.randomUUID().toString());

        // Assert
        assertThat(foundWorkspace).isNotPresent();
    }

    @Test
    @DisplayName("should find all saved workspaces")
    void findAll_shouldReturnAllWorkspaces() {
        // Arrange
        workspaceJpaRepository.save(workspace1);
        workspaceJpaRepository.save(workspace2);
        entityManager.flush();

        // Act
        List<WorkspaceJpaEntity> workspaces = workspaceJpaRepository.findAll();

        // Assert
        assertThat(workspaces).hasSize(2);
        assertThat(workspaces).extracting(WorkspaceJpaEntity::getName).containsExactlyInAnyOrder("Workspace Alpha", "Workspace Beta");
    }

    @Test
    @DisplayName("should return empty list when no workspaces are saved")
    void findAll_whenNoWorkspaces_shouldReturnEmptyList() {
        // Act
        List<WorkspaceJpaEntity> workspaces = workspaceJpaRepository.findAll();

        // Assert
        assertThat(workspaces).isEmpty();
    }

    @Test
    @DisplayName("should update an existing workspace")
    void updateWorkspace() {
        // Arrange
        WorkspaceJpaEntity persistedWorkspace = workspaceJpaRepository.save(workspace1);
        entityManager.flush();

        Utils.sleep(100);

        // Act
        // Fetch, modify, and save
        Optional<WorkspaceJpaEntity> workspaceToUpdateOpt = workspaceJpaRepository.findById(persistedWorkspace.getId());
        assertThat(workspaceToUpdateOpt).isPresent();

        WorkspaceJpaEntity workspaceToUpdate = workspaceToUpdateOpt.get();
        workspaceToUpdate.setName("Workspace Alpha Updated");
        workspaceToUpdate.setDescription("Updated Description Alpha");
        workspaceToUpdate.setPrivate(true);
        workspaceToUpdate.getMembers().remove(memberUser1);
        workspaceToUpdate.getMembers().add(memberUser2);
        workspaceToUpdate.setOwner(memberUser2); // Change owner

        WorkspaceJpaEntity updatedAndSavedWorkspace = workspaceJpaRepository.save(workspaceToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<WorkspaceJpaEntity> foundAfterUpdateOpt = workspaceJpaRepository.findById(persistedWorkspace.getId());
        assertThat(foundAfterUpdateOpt).isPresent();
        WorkspaceJpaEntity foundAfterUpdate = foundAfterUpdateOpt.get();

        assertThat(foundAfterUpdate.getName()).isEqualTo("Workspace Alpha Updated");
        assertThat(foundAfterUpdate.getDescription()).isEqualTo("Updated Description Alpha");
        assertThat(foundAfterUpdate.isPrivate()).isTrue();
        assertThat(foundAfterUpdate.getOwner().getId()).isEqualTo(memberUser2.getId());
        assertThat(foundAfterUpdate.getMembers()).hasSize(1);
        assertThat(foundAfterUpdate.getMembers().iterator().next().getId()).isEqualTo(memberUser2.getId());

    }

    @Test
    @DisplayName("should delete a workspace by id")
    void deleteById() {
        // Arrange
        WorkspaceJpaEntity persistedWorkspace = workspaceJpaRepository.save(workspace1);
        entityManager.flush();
        String idToDelete = persistedWorkspace.getId();

        // Act
        workspaceJpaRepository.deleteById(idToDelete);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<WorkspaceJpaEntity> deletedWorkspace = workspaceJpaRepository.findById(idToDelete);
        assertThat(deletedWorkspace).isNotPresent();
    }

    @Test
    @DisplayName("existsById should return true for existing workspace")
    void existsById_whenWorkspaceExists_shouldReturnTrue() {
        // Arrange
        WorkspaceJpaEntity persistedWorkspace = workspaceJpaRepository.save(workspace1);
        entityManager.flush();

        // Act
        boolean exists = workspaceJpaRepository.existsById(persistedWorkspace.getId());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById should return false for non-existing workspace")
    void existsById_whenWorkspaceDoesNotExist_shouldReturnFalse() {
        // Act
        boolean exists = workspaceJpaRepository.existsById(UUID.randomUUID().toString());

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findAll with spec should find by name")
    void findAll_withSpecification_byName() {
        workspaceJpaRepository.save(workspace1); // Alpha
        workspaceJpaRepository.save(workspace2); // Beta
        entityManager.flush();

        Specification<WorkspaceJpaEntity> spec = (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%alpha%");
        Pageable pageable = PageRequest.of(0, 10);

        Page<WorkspaceJpaEntity> result = workspaceJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Workspace Alpha");
    }

    @Test
    @DisplayName("findAll with spec should find by owner's name")
    void findAll_withSpecification_byOwnerName() {
        workspaceJpaRepository.save(workspace1); // Owner: ownerUser1 (Owner One)
        workspaceJpaRepository.save(workspace3); // Owner: memberUser1 (Member One)
        entityManager.flush();

        Specification<WorkspaceJpaEntity> spec = (root, query, cb) ->
                cb.equal(root.join("owner").get("name"), "Owner One");
        Pageable pageable = PageRequest.of(0, 10);

        Page<WorkspaceJpaEntity> result = workspaceJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Workspace Alpha");
    }

    @Test
    @DisplayName("findAll with spec should find by member's name")
    void findAll_withSpecification_byMemberName() {
        workspaceJpaRepository.save(workspace1); // Members: memberUser1 (Member One)
        workspaceJpaRepository.save(workspace2); // Members: memberUser1 (Member One), memberUser2 (Member Two)
        workspaceJpaRepository.save(workspace3); // Members: (none)
        entityManager.flush();

        Specification<WorkspaceJpaEntity> spec = (root, query, cb) -> {
            Assertions.assertNotNull(query);
            if (query.getResultType() != Long.class && query.getResultType() != long.class) { // Avoid fetching for projectCount queries
                // root.fetch("members", JoinType.LEFT); // Optional: Eager fetch if needed, can cause Cartesian product
                query.distinct(true); // Crucial for ManyToMany joins to avoid duplicate root entities
            }
            return cb.equal(root.join("members").get("name"), "Member One");
        };
        Pageable pageable = PageRequest.of(0, 10);

        Page<WorkspaceJpaEntity> result = workspaceJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(WorkspaceJpaEntity::getName)
                .containsExactlyInAnyOrder("Workspace Alpha", "Workspace Beta");
    }

    @Test
    @DisplayName("findAll with spec should find by isPrivate status")
    void findAll_withSpecification_byIsPrivate() {
        workspaceJpaRepository.save(workspace1); // isPrivate = false
        workspaceJpaRepository.save(workspace2); // isPrivate = true
        entityManager.flush();

        Specification<WorkspaceJpaEntity> specTrue = (root, query, cb) -> cb.isTrue(root.get("isPrivate"));
        Specification<WorkspaceJpaEntity> specFalse = (root, query, cb) -> cb.isFalse(root.get("isPrivate"));
        Pageable pageable = PageRequest.of(0, 10);

        Page<WorkspaceJpaEntity> privateResults = workspaceJpaRepository.findAll(specTrue, pageable);
        assertThat(privateResults.getContent()).hasSize(1);
        assertThat(privateResults.getContent().get(0).getName()).isEqualTo("Workspace Beta");

        Page<WorkspaceJpaEntity> publicResults = workspaceJpaRepository.findAll(specFalse, pageable);
        assertThat(publicResults.getContent()).hasSize(1);
        assertThat(publicResults.getContent().get(0).getName()).isEqualTo("Workspace Alpha");
    }

    @Test
    @DisplayName("findAll with specification should return empty page if no matches")
    void findAll_withSpecification_noMatches() {
        workspaceJpaRepository.save(workspace1);
        entityManager.flush();

        Specification<WorkspaceJpaEntity> spec = (root, query, cb) ->
                cb.equal(root.get("name"), "NonExistentWorkspace");
        Pageable pageable = PageRequest.of(0, 10);

        Page<WorkspaceJpaEntity> result = workspaceJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("findAll with spec should respect pagination")
    void findAll_withSpecification_withPagination() {
        workspaceJpaRepository.save(workspace1); // Alpha
        workspaceJpaRepository.save(workspace2); // Beta
        workspaceJpaRepository.save(workspace3); // Gamma
        entityManager.flush();

        Specification<WorkspaceJpaEntity> spec = (root, query, cb) -> cb.conjunction(); // Matches all
        Pageable firstPage = PageRequest.of(0, 2);

        Page<WorkspaceJpaEntity> resultPage1 = workspaceJpaRepository.findAll(spec, firstPage);
        assertThat(resultPage1.getContent()).hasSize(2);
        assertThat(resultPage1.getTotalElements()).isEqualTo(3);
        assertThat(resultPage1.getTotalPages()).isEqualTo(2);
        assertThat(resultPage1.getNumber()).isEqualTo(0);

        Pageable secondPage = PageRequest.of(1, 2);
        Page<WorkspaceJpaEntity> resultPage2 = workspaceJpaRepository.findAll(spec, secondPage);
        assertThat(resultPage2.getContent()).hasSize(1);
        assertThat(resultPage2.getNumber()).isEqualTo(1);
    }
}
