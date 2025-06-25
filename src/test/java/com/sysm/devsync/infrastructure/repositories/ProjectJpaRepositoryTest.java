package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.ProjectJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.sysm.devsync.infrastructure.Utils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProjectJpaRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private WorkspaceJpaEntity workspace1;
    private WorkspaceJpaEntity workspace2;

    private ProjectJpaEntity project1;
    private ProjectJpaEntity project2;
    private ProjectJpaEntity project3;

    @BeforeEach
    void setUp() {
        clearRepositories();

        // Create and persist Workspaces first
        workspace1 = new WorkspaceJpaEntity();
        workspace1.setId(UUID.randomUUID().toString());
        workspace1.setName("Test Workspace 1");
        workspace1.setDescription("Workspace for testing projects");
        UserJpaEntity owner = new UserJpaEntity(UUID.randomUUID().toString());
        owner.setName("Default Owner");
        owner.setEmail("owner@example.com");
        owner.setRole(UserRole.ADMIN);
        owner.setCreatedAt(Instant.now());
        owner.setUpdatedAt(Instant.now());

        entityManager.persist(owner);
        workspace1.setOwner(owner);
        workspace1.setCreatedAt(Instant.now());
        workspace1.setUpdatedAt(Instant.now());
        entityManager.persist(workspace1);

        workspace2 = new WorkspaceJpaEntity();
        workspace2.setId(UUID.randomUUID().toString());
        workspace2.setName("Test Workspace 2");
        workspace2.setDescription("Another workspace for testing");
        workspace2.setOwner(owner); // if owner is required
        workspace2.setCreatedAt(Instant.now());
        workspace2.setUpdatedAt(Instant.now());
        entityManager.persist(workspace2);

        entityManager.flush(); // Ensure workspaces are in DB

        // Create Project Entities, ensuring they link to *persisted* workspaces
        // Timestamps for projects will be set by their @PrePersist
        project1 = new ProjectJpaEntity();
        project1.setId(UUID.randomUUID().toString());
        project1.setName("Project Alpha");
        project1.setDescription("First test project");
        project1.setWorkspace(workspace1); // Link to persisted workspace1
        project1.setCreatedAt(Instant.now());
        project1.setUpdatedAt(Instant.now());

        project2 = new ProjectJpaEntity();
        project2.setId(UUID.randomUUID().toString());
        project2.setName("Project Beta");
        project2.setDescription("Second test project, different workspace");
        project2.setWorkspace(workspace2); // Link to persisted workspace2
        project2.setCreatedAt(Instant.now());
        project2.setUpdatedAt(Instant.now());

        project3 = new ProjectJpaEntity();
        project3.setId(UUID.randomUUID().toString());
        project3.setName("Project Gamma");
        project3.setDescription("Third test project, same workspace as Alpha");
        project3.setWorkspace(workspace1); // Link to persisted workspace1
        project3.setCreatedAt(Instant.now());
        project3.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("should save a project and find it by id")
    void saveAndFindById() {
        // Act
        ProjectJpaEntity savedProject = projectJpaRepository.save(project1);
        entityManager.flush(); // Ensure save is committed
        entityManager.clear(); // Ensure we fetch fresh from DB

        Optional<ProjectJpaEntity> foundProjectOpt = projectJpaRepository.findById(savedProject.getId());

        // Assert
        assertThat(foundProjectOpt).isPresent();
        ProjectJpaEntity foundProject = foundProjectOpt.get();
        assertThat(foundProject.getName()).isEqualTo(project1.getName());
        assertThat(foundProject.getDescription()).isEqualTo(project1.getDescription());
        assertThat(foundProject.getWorkspace()).isNotNull();
        assertThat(foundProject.getWorkspace().getId()).isEqualTo(workspace1.getId());
        assertThat(foundProject.getCreatedAt()).isNotNull(); // Set by @PrePersist
        assertThat(foundProject.getUpdatedAt()).isNotNull(); // Set by @PrePersist
    }

    @Test
    @DisplayName("should fail to save project with null workspace if workspace is non-nullable")
    void save_withNullWorkspace_shouldFail() {
        // Arrange
        ProjectJpaEntity projectWithNullWorkspace = new ProjectJpaEntity();
        projectWithNullWorkspace.setId(UUID.randomUUID().toString());
        projectWithNullWorkspace.setName("Project NoSpace");
        projectWithNullWorkspace.setDescription("This project has no workspace");
        projectWithNullWorkspace.setWorkspace(null); // Explicitly set to null

        // Act & Assert
        // This relies on @JoinColumn(name = "workspace_id", nullable = false) in ProjectJpaEntity
        assertThatThrownBy(() -> {
            projectJpaRepository.save(projectWithNullWorkspace);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class); // Or potentially ConstraintViolationException
    }

    @Test
    @DisplayName("should fail to save project with duplicate name due to unique constraint")
    void save_withDuplicateName_shouldFail() {
        // Arrange
        projectJpaRepository.save(project1); // Save the first project
        entityManager.flush();

        ProjectJpaEntity duplicateNameProject = new ProjectJpaEntity();
        duplicateNameProject.setId(UUID.randomUUID().toString());
        duplicateNameProject.setName(project1.getName()); // Same name
        duplicateNameProject.setDescription("Another description for duplicate name project");
        duplicateNameProject.setWorkspace(workspace2); // Different workspace is fine

        // Act & Assert
        assertThatThrownBy(() -> {
            projectJpaRepository.save(duplicateNameProject);
            entityManager.flush(); // This will trigger the constraint violation
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("should return empty optional when finding non-existent project by id")
    void findById_whenProjectDoesNotExist_shouldReturnEmpty() {
        // Act
        Optional<ProjectJpaEntity> foundProject = projectJpaRepository.findById(UUID.randomUUID().toString());

        // Assert
        assertThat(foundProject).isNotPresent();
    }

    @Test
    @DisplayName("should find all saved projects")
    void findAll_shouldReturnAllProjects() {
        // Arrange
        projectJpaRepository.save(project1);
        projectJpaRepository.save(project2);
        entityManager.flush();

        // Act
        List<ProjectJpaEntity> projects = projectJpaRepository.findAll();

        // Assert
        assertThat(projects).hasSize(2);
        assertThat(projects).extracting(ProjectJpaEntity::getName).containsExactlyInAnyOrder("Project Alpha", "Project Beta");
    }

    @Test
    @DisplayName("should return empty list when no projects are saved")
    void findAll_whenNoProjects_shouldReturnEmptyList() {
        // Act
        List<ProjectJpaEntity> projects = projectJpaRepository.findAll();

        // Assert
        assertThat(projects).isEmpty();
    }

    @Test
    @DisplayName("should update an existing project")
    void updateProject() {
        // Arrange
        ProjectJpaEntity persistedProject = projectJpaRepository.save(project1);
        entityManager.flush();
        Instant originalUpdatedAt = persistedProject.getUpdatedAt(); // Capture before update

        sleep(100); // Ensure a noticeable time difference for updatedAt

        // Act
        // Fetch, modify, and save
        Optional<ProjectJpaEntity> projectToUpdateOpt = projectJpaRepository.findById(persistedProject.getId());
        assertThat(projectToUpdateOpt).isPresent();

        ProjectJpaEntity projectToUpdate = projectToUpdateOpt.get();
        projectToUpdate.setName("Project Alpha Updated");
        projectToUpdate.setDescription("Updated description for Alpha");
        projectToUpdate.setWorkspace(workspace2); // Change workspace to another persisted one
        projectToUpdate.setUpdatedAt(Instant.now());
        projectJpaRepository.save(projectToUpdate); // This save should trigger @PreUpdate

        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<ProjectJpaEntity> updatedProjectOpt = projectJpaRepository.findById(persistedProject.getId());
        assertThat(updatedProjectOpt).isPresent();
        ProjectJpaEntity updatedProject = updatedProjectOpt.get();

        assertThat(updatedProject.getName()).isEqualTo("Project Alpha Updated");
        assertThat(updatedProject.getDescription()).isEqualTo("Updated description for Alpha");
        assertThat(updatedProject.getWorkspace()).isNotNull();
        assertThat(updatedProject.getWorkspace().getId()).isEqualTo(workspace2.getId());
        assertThat(updatedProject.getUpdatedAt()).isNotNull();
        assertThat(updatedProject.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("should delete a project by id")
    void deleteById() {
        // Arrange
        ProjectJpaEntity persistedProject = projectJpaRepository.save(project1);
        entityManager.flush();
        String idToDelete = persistedProject.getId();

        // Act
        projectJpaRepository.deleteById(idToDelete);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<ProjectJpaEntity> deletedProject = projectJpaRepository.findById(idToDelete);
        assertThat(deletedProject).isNotPresent();
    }

    @Test
    @DisplayName("existsById should return true for existing project")
    void existsById_whenProjectExists_shouldReturnTrue() {
        // Arrange
        ProjectJpaEntity persistedProject = projectJpaRepository.save(project1);
        entityManager.flush();

        // Act
        boolean exists = projectJpaRepository.existsById(persistedProject.getId());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById should return false for non-existing project")
    void existsById_whenProjectDoesNotExist_shouldReturnFalse() {
        // Act
        boolean exists = projectJpaRepository.existsById(UUID.randomUUID().toString());

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findAll with spec should find by project name")
    void findAll_withSpecification_byProjectName() {
        projectJpaRepository.save(project1); // Alpha
        projectJpaRepository.save(project2); // Beta
        entityManager.flush();

        Specification<ProjectJpaEntity> spec = (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%alpha%");
        Pageable pageable = PageRequest.of(0, 10);

        Page<ProjectJpaEntity> result = projectJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Project Alpha");
    }

    @Test
    @DisplayName("findAll with spec should find by project description")
    void findAll_withSpecification_byProjectDescription() {
        projectJpaRepository.save(project1); // "First test project"
        projectJpaRepository.save(project2); // "Second test project, different workspace"
        entityManager.flush();

        Specification<ProjectJpaEntity> spec = (root, query, cb) ->
                cb.like(cb.lower(root.get("description")), "%second test project%");
        Pageable pageable = PageRequest.of(0, 10);

        Page<ProjectJpaEntity> result = projectJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Project Beta");
    }

    @Test
    @DisplayName("findAll with spec should find by workspace name")
    void findAll_withSpecification_byWorkspaceName() {
        projectJpaRepository.save(project1); // Workspace1: "Test Workspace 1"
        projectJpaRepository.save(project2); // Workspace2: "Test Workspace 2"
        projectJpaRepository.save(project3); // Workspace1: "Test Workspace 1"
        entityManager.flush();

        Specification<ProjectJpaEntity> spec = (root, query, cb) ->
                // Join with workspace and filter by its name
                cb.equal(root.join("workspace").get("name"), "Test Workspace 1");
        Pageable pageable = PageRequest.of(0, 10);

        Page<ProjectJpaEntity> result = projectJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(ProjectJpaEntity::getName)
                .containsExactlyInAnyOrder("Project Alpha", "Project Gamma");
    }

    @Test
    @DisplayName("findAll with specification should return empty page if no matches")
    void findAll_withSpecification_noMatches() {
        projectJpaRepository.save(project1);
        entityManager.flush();

        Specification<ProjectJpaEntity> spec = (root, query, cb) ->
                cb.equal(root.get("name"), "NonExistentProject");
        Pageable pageable = PageRequest.of(0, 10);

        Page<ProjectJpaEntity> result = projectJpaRepository.findAll(spec, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("findAll with spec should respect pagination")
    void findAll_withSpecification_withPagination() {
        projectJpaRepository.save(project1); // Alpha
        projectJpaRepository.save(project2); // Beta
        projectJpaRepository.save(project3); // Gamma
        entityManager.flush();

        Specification<ProjectJpaEntity> spec = (root, query, cb) -> cb.conjunction(); // Matches all
        Pageable firstPage = PageRequest.of(0, 2);

        Page<ProjectJpaEntity> resultPage1 = projectJpaRepository.findAll(spec, firstPage);
        assertThat(resultPage1.getContent()).hasSize(2);
        assertThat(resultPage1.getTotalElements()).isEqualTo(3);
        assertThat(resultPage1.getTotalPages()).isEqualTo(2);
        assertThat(resultPage1.getNumber()).isEqualTo(0);

        Pageable secondPage = PageRequest.of(1, 2);
        Page<ProjectJpaEntity> resultPage2 = projectJpaRepository.findAll(spec, secondPage);
        assertThat(resultPage2.getContent()).hasSize(1);
        assertThat(resultPage2.getNumber()).isEqualTo(1);
    }
}
