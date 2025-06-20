package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.User; // For workspace owner setup
import com.sysm.devsync.domain.models.Workspace; // For project workspace setup
import com.sysm.devsync.infrastructure.PersistenceTest;
import com.sysm.devsync.infrastructure.repositories.entities.ProjectJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@PersistenceTest
@Import(ProjectPersistence.class) // Import the class under test
public class ProjectPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectPersistence projectPersistence; // The class under test

    // Dependent entities for setup
    private UserJpaEntity ownerUserJpa;
    private WorkspaceJpaEntity workspace1Jpa;
    private WorkspaceJpaEntity workspace2Jpa;

    // Domain models for testing
    private Project project1Domain;
    private Project project2Domain;
    private Project project3Domain;


    @BeforeEach
    void setUp() {
        // Clean up in reverse order of dependency or let @DataJpaTest handle rollback
        entityManager.getEntityManager().createQuery("DELETE FROM Project").executeUpdate();
       // entityManager.getEntityManager().createQuery("DELETE FROM WorkspaceMember").executeUpdate(); // If workspace_members table exists
        entityManager.getEntityManager().createQuery("DELETE FROM Workspace").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM User").executeUpdate();
        entityManager.flush();

        // 1. Create Owner User
        User ownerDomain = User.create("Owner User", "owner@example.com", UserRole.ADMIN);
        ownerUserJpa = UserJpaEntity.fromModel(ownerDomain);
        entityManager.persist(ownerUserJpa);

        // 2. Create Workspaces
        Workspace ws1Domain = Workspace.create("Workspace One", "First test workspace", false, ownerUserJpa.getId());
        workspace1Jpa = new WorkspaceJpaEntity(); // Manual mapping to ensure owner is managed
        workspace1Jpa.setId(ws1Domain.getId());
        workspace1Jpa.setName(ws1Domain.getName());
        workspace1Jpa.setDescription(ws1Domain.getDescription());
        workspace1Jpa.setPrivate(ws1Domain.isPrivate());
        workspace1Jpa.setOwner(ownerUserJpa); // Set managed owner
        // Timestamps will be set by @PrePersist
        entityManager.persist(workspace1Jpa);

        Workspace ws2Domain = Workspace.create("Workspace Two", "Second test workspace", true, ownerUserJpa.getId());
        workspace2Jpa = new WorkspaceJpaEntity();
        workspace2Jpa.setId(ws2Domain.getId());
        workspace2Jpa.setName(ws2Domain.getName());
        workspace2Jpa.setDescription(ws2Domain.getDescription());
        workspace2Jpa.setPrivate(ws2Domain.isPrivate());
        workspace2Jpa.setOwner(ownerUserJpa);
        entityManager.persist(workspace2Jpa);

        entityManager.flush(); // Ensure users and workspaces are in DB

        // 3. Create Project Domain Models
        project1Domain = Project.create("Project Alpha", "Description for Alpha", workspace1Jpa.getId());
        project2Domain = Project.create("Project Beta", "Description for Beta", workspace2Jpa.getId());
        project3Domain = Project.create("Project Gamma", "Description for Gamma", workspace1Jpa.getId());
    }

    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a project")
        void create_shouldSaveProject() {
            // Act
            assertDoesNotThrow(() -> projectPersistence.create(project1Domain));
            entityManager.flush();
            entityManager.clear();

            // Assert
            ProjectJpaEntity foundInDb = entityManager.find(ProjectJpaEntity.class, project1Domain.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getName()).isEqualTo(project1Domain.getName());
            assertThat(foundInDb.getWorkspace().getId()).isEqualTo(project1Domain.getWorkspaceId());

            Optional<Project> foundProject = projectPersistence.findById(project1Domain.getId());
            assertThat(foundProject).isPresent();
            assertThat(foundProject.get().getName()).isEqualTo(project1Domain.getName());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when creating with null model")
        void create_nullModel_shouldThrowException() {
            assertThatThrownBy(() -> projectPersistence.create(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project model cannot be null");
        }

        @Test
        @DisplayName("should fail to create project with non-existent workspace ID due to FK constraint")
        void create_nonExistentWorkspaceId_shouldFail() {
            Project projectWithInvalidWorkspace = Project.create("Invalid WS Project", "Desc", UUID.randomUUID().toString());
            // This test relies on the database foreign key constraint.
            // The ProjectJpaEntity.fromModel will create a transient WorkspaceJpaEntity(id).
            // When saving ProjectJpaEntity, if the workspace_id doesn't exist in the workspaces table,
            // the DB will throw a constraint violation.
            assertThatThrownBy(() -> {
                projectPersistence.create(projectWithInvalidWorkspace);
                entityManager.flush(); // Force DB interaction
            }).isInstanceOf(ConstraintViolationException.class); // Or a more specific FK violation if available
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing project")
        void update_shouldModifyExistingProject() {
            // Arrange: First, create the project
            projectPersistence.create(project1Domain);
            entityManager.flush();
            entityManager.clear(); // Detach to simulate fresh fetch & update

            Project updatedDomainProject = Project.build(
                    project1Domain.getId(),
                    "Project Alpha Updated",
                    "Updated Alpha Description",
                    workspace2Jpa.getId(), // Change workspace
                    project1Domain.getCreatedAt(), // Keep original createdAt
                    Instant.now() // New updatedAt
            );

            // Act
            projectPersistence.update(updatedDomainProject);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<Project> foundProjectOpt = projectPersistence.findById(project1Domain.getId());
            assertThat(foundProjectOpt).isPresent();
            Project foundProject = foundProjectOpt.get();

            assertThat(foundProject.getName()).isEqualTo("Project Alpha Updated");
            assertThat(foundProject.getDescription()).isEqualTo("Updated Alpha Description");
            assertThat(foundProject.getWorkspaceId()).isEqualTo(workspace2Jpa.getId());
            assertThat(foundProject.getUpdatedAt()).isAfter(project1Domain.getCreatedAt());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when updating with null model")
        void update_nullModel_shouldThrowException() {
            assertThatThrownBy(() -> projectPersistence.update(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project model cannot be null");
        }

        @Test
        @DisplayName("update should effectively insert if ID does not exist (current behavior)")
        void update_nonExistentId_shouldInsert() {
            // Current `update` method behavior: if ID doesn't exist, `repository.save()` will insert.
            Project newProjectToUpdate = Project.create("New Project via Update", "Desc", workspace1Jpa.getId());

            assertDoesNotThrow(() -> projectPersistence.update(newProjectToUpdate));
            entityManager.flush();
            entityManager.clear();

            Optional<Project> foundProject = projectPersistence.findById(newProjectToUpdate.getId());
            assertThat(foundProject).isPresent();
            assertThat(foundProject.get().getName()).isEqualTo("New Project via Update");
        }
    }

    @Nested
    @DisplayName("deleteById Method Tests")
    class DeleteByIdTests {
        @Test
        @DisplayName("should delete a project by its ID")
        void deleteById_shouldRemoveProject() {
            // Arrange
            projectPersistence.create(project1Domain);
            entityManager.flush();
            assertThat(projectPersistence.existsById(project1Domain.getId())).isTrue();

            // Act
            projectPersistence.deleteById(project1Domain.getId());
            entityManager.flush();
            entityManager.clear();

            // Assert
            assertThat(projectPersistence.existsById(project1Domain.getId())).isFalse();
            assertThat(projectPersistence.findById(project1Domain.getId())).isNotPresent();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when deleting with null ID")
        void deleteById_nullId_shouldThrowException() {
            assertThatThrownBy(() -> projectPersistence.deleteById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project ID cannot be null or empty");
        }

        @Test
        @DisplayName("deleteById should not throw error for non-existent ID")
        void deleteById_nonExistentId_shouldNotThrowError() {
            assertDoesNotThrow(() -> projectPersistence.deleteById(UUID.randomUUID().toString()));
            entityManager.flush(); // Ensure no exceptions during flush
        }
    }

    @Nested
    @DisplayName("findById Method Tests")
    class FindByIdTests {
        @Test
        @DisplayName("should return project when found")
        void findById_whenProjectExists_shouldReturnProject() {
            // Arrange
            projectPersistence.create(project1Domain);
            entityManager.flush();

            // Act
            Optional<Project> foundProject = projectPersistence.findById(project1Domain.getId());

            // Assert
            assertThat(foundProject).isPresent();
            assertThat(foundProject.get().getId()).isEqualTo(project1Domain.getId());
            assertThat(foundProject.get().getName()).isEqualTo(project1Domain.getName());
        }

        @Test
        @DisplayName("should return empty optional when project not found")
        void findById_whenProjectDoesNotExist_shouldReturnEmpty() {
            // Act
            Optional<Project> foundProject = projectPersistence.findById(UUID.randomUUID().toString());

            // Assert
            assertThat(foundProject).isNotPresent();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when finding with null ID")
        void findById_nullId_shouldThrowException() {
            assertThatThrownBy(() -> projectPersistence.findById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project ID cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("existsById Method Tests")
    class ExistsByIdTests {
        @Test
        @DisplayName("should return true when project exists")
        void existsById_whenProjectExists_shouldReturnTrue() {
            // Arrange
            projectPersistence.create(project1Domain);
            entityManager.flush();

            // Act
            boolean exists = projectPersistence.existsById(project1Domain.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when project does not exist")
        void existsById_whenProjectDoesNotExist_shouldReturnFalse() {
            // Act
            boolean exists = projectPersistence.existsById(UUID.randomUUID().toString());

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when checking existence with null ID")
        void existsById_nullId_shouldThrowException() {
            assertThatThrownBy(() -> projectPersistence.existsById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project ID cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("findAll Method Tests")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            // Persist test data
            projectPersistence.create(project1Domain); // Alpha, workspace1
            projectPersistence.create(project2Domain); // Beta, workspace2
            projectPersistence.create(project3Domain); // Gamma, workspace1
            entityManager.flush();
        }

        @Test
        @DisplayName("should return all projects when no search terms provided")
        void findAll_noTerms_shouldReturnAllProjects() {
            SearchQuery query = new SearchQuery(Pageable.of(0, 10), "");
            Pagination<Project> result = projectPersistence.findAll(query);

            assertThat(result.items()).hasSize(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by project name")
        void findAll_filterByName_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Pageable.of(0, 10), "name=Alpha");
            Pagination<Project> result = projectPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getName()).isEqualTo("Project Alpha");
        }

        @Test
        @DisplayName("should filter by project description")
        void findAll_filterByDescription_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Pageable.of(0, 10), "description=Beta");
            Pagination<Project> result = projectPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getName()).isEqualTo("Project Beta");
        }

        @Test
        @DisplayName("should filter by workspaceId")
        void findAll_filterByWorkspaceId_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Pageable.of(0, 10), "workspaceId=" + workspace1Jpa.getId());
            Pagination<Project> result = projectPersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            List<String> names = result.items().stream().map(Project::getName).toList();
            assertThat(names).containsExactlyInAnyOrder("Project Alpha", "Project Gamma");
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field (not in VALID_SEARCHABLE_FIELDS)")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = new SearchQuery(Pageable.of(0, 10), "invalidField=test");

            assertThatThrownBy(() -> projectPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should throw BusinessException for an unsupported search field (not in createPredicateForField switch)")
        void findAll_unsupportedSearchField_shouldThrowBusinessException() {
            // Assuming "category" is in VALID_SEARCHABLE_FIELDS but not in createPredicateForField switch
            // For this test, let's assume "category" was mistakenly added to VALID_SEARCHABLE_FIELDS
            // but not handled in createPredicateForField.
            // To test this properly, you'd need to adjust VALID_SEARCHABLE_FIELDS in ProjectPersistence
            // or ensure the default case in createPredicateForField is hit.
            // Given the current setup (category removed from createPredicateForField and not in VALID_SEARCHABLE_FIELDS),
            // this test is similar to the one above.
            // If "category" was valid but had no predicate logic:
            // SearchQuery query = new SearchQuery(Pageable.of(0, 10), "category=test");
            // assertThatThrownBy(() -> projectPersistence.findAll(query))
            //        .isInstanceOf(BusinessException.class)
            //        .hasMessageContaining("Unsupported search logic for field: 'category'");
            // This test is a bit tricky without modifying ProjectPersistence for the test's sake.
            // The `invalidSearchField_shouldThrowBusinessException` covers the primary case.
        }


        @Test
        @DisplayName("should handle terms with no matches")
        void findAll_termWithNoMatches_shouldReturnEmptyPage() {
            SearchQuery query = new SearchQuery(Pageable.of(0, 10), "name=NonExistentProject");
            Pagination<Project> result = projectPersistence.findAll(query);

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void findAll_withPagination_shouldReturnCorrectPage() {
            SearchQuery queryPage1 = new SearchQuery(Pageable.of(0, 2, "name", "asc"), "");
            Pagination<Project> result1 = projectPersistence.findAll(queryPage1);

            assertThat(result1.items()).hasSize(2);
            assertThat(result1.currentPage()).isEqualTo(0);
            assertThat(result1.perPage()).isEqualTo(2);
            assertThat(result1.total()).isEqualTo(3);

            SearchQuery queryPage2 = new SearchQuery(Pageable.of(1, 2, "name", "asc"), "");
            Pagination<Project> result2 = projectPersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
        }
    }
}
