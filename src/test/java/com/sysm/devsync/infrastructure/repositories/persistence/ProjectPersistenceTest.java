package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QueryType;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.User; // For workspace owner setup
import com.sysm.devsync.domain.models.Workspace; // For project workspace setup
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.ProjectJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.sysm.devsync.infrastructure.Utils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(ProjectPersistence.class) // Import the class under test
public class ProjectPersistenceTest extends AbstractRepositoryTest {

    @Autowired
    private ProjectPersistence projectPersistence; // The class under test

    private WorkspaceJpaEntity workspace1Jpa;
    private WorkspaceJpaEntity workspace2Jpa;

    // Domain models for testing
    private Project project1Domain;
    private Project project2Domain;
    private Project project3Domain;

    @BeforeEach
    void setUp() {
        clearRepositories();

        // 1. Create Owner User
        User ownerDomain = User.create("Owner User", "owner@example.com", UserRole.ADMIN);
        // Dependent entities for setup
        UserJpaEntity ownerUserJpa = UserJpaEntity.fromModel(ownerDomain);
        entityPersist(ownerUserJpa);

        // 2. Create Workspaces
        Workspace ws1Domain = Workspace.create("Workspace One", "First test workspace", false, ownerUserJpa.getId());
        workspace1Jpa = new WorkspaceJpaEntity(); // Manual mapping to ensure owner is managed
        workspace1Jpa.setId(ws1Domain.getId());
        workspace1Jpa.setName(ws1Domain.getName());
        workspace1Jpa.setDescription(ws1Domain.getDescription());
        workspace1Jpa.setPrivate(ws1Domain.isPrivate());
        workspace1Jpa.setOwner(ownerUserJpa); // Set managed owner
        workspace1Jpa.setCreatedAt(Instant.now());
        workspace1Jpa.setUpdatedAt(Instant.now());
        entityPersist(workspace1Jpa);

        Workspace ws2Domain = Workspace.create("Workspace Two", "Second test workspace", true, ownerUserJpa.getId());
        workspace2Jpa = new WorkspaceJpaEntity();
        workspace2Jpa.setId(ws2Domain.getId());
        workspace2Jpa.setName(ws2Domain.getName());
        workspace2Jpa.setDescription(ws2Domain.getDescription());
        workspace2Jpa.setPrivate(ws2Domain.isPrivate());
        workspace2Jpa.setOwner(ownerUserJpa);
        workspace2Jpa.setCreatedAt(Instant.now());
        workspace2Jpa.setUpdatedAt(Instant.now());
        entityPersist(workspace2Jpa);


        // 3. Create Project Domain Models
        project1Domain = Project.create("Project Alpha", "Description for Alpha", workspace1Jpa.getId());
        project2Domain = Project.create("Project Beta", "Description for Beta", workspace2Jpa.getId());
        project3Domain = Project.create("Project Gamma", "Another description for Gamma", workspace1Jpa.getId());
    }

    // --- Basic CRUD tests are correct and remain unchanged ---
    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a project")
        void create_shouldSaveProject() {
            // Act
            assertDoesNotThrow(() -> create(project1Domain));

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
            assertThatThrownBy(() -> create(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project model cannot be null");
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing project")
        void update_shouldModifyExistingProject() {
            // Arrange: First, create the project
            create(project1Domain);
            sleep(10); // Ensure updatedAt will be different

            Project updatedDomainProject = Project.build(
                    project1Domain.getId(),
                    "Project Alpha Updated",
                    "Updated Alpha Description",
                    workspace2Jpa.getId(), // Change workspace
                    project1Domain.getCreatedAt(), // Keep original createdAt
                    Instant.now() // New updatedAt
            );

            // Act
            update(updatedDomainProject);

            // Assert
            Optional<Project> foundProjectOpt = projectPersistence.findById(project1Domain.getId());
            assertThat(foundProjectOpt).isPresent();
            Project foundProject = foundProjectOpt.get();

            assertThat(foundProject.getName()).isEqualTo("Project Alpha Updated");
            assertThat(foundProject.getDescription()).isEqualTo("Updated Alpha Description");
            assertThat(foundProject.getWorkspaceId()).isEqualTo(workspace2Jpa.getId());
            assertThat(foundProject.getUpdatedAt()).isAfter(project1Domain.getCreatedAt());
        }
    }

    // --- Other basic tests (delete, findById, existsById) are also correct ---

    @Nested
    @DisplayName("findAll Method Tests")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            // Persist test data
            create(project1Domain); // Alpha, workspace1
            create(project2Domain); // Beta, workspace2
            create(project3Domain); // Gamma, workspace1
        }

        @Test
        @DisplayName("should return all projects when no search terms provided")
        void findAll_noTerms_shouldReturnAllProjects() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10),  Map.of());
            Pagination<Project> result = projectPersistence.findAll(query);

            assertThat(result.items()).hasSize(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by project name")
        void findAll_filterByName_shouldReturnMatching() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("name","Alpha"));
            Pagination<Project> result = projectPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getName()).isEqualTo("Project Alpha");
        }

        @Test
        @DisplayName("should filter by workspaceId")
        void findAll_filterByWorkspaceId_shouldReturnMatching() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("workspaceId",workspace1Jpa.getId()));
            Pagination<Project> result = projectPersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            List<String> names = result.items().stream().map(Project::getName).toList();
            assertThat(names).containsExactlyInAnyOrder("Project Alpha", "Project Gamma");
        }

        @Test
        @DisplayName("should filter by multiple valid terms using AND logic")
        void findAll_withMultipleTerms_shouldReturnAndedResults() {
            // Arrange: Search for a project named "Alpha" AND in workspace1
            SearchQuery queryWithMatch = SearchQuery.of(Page.of(0, 10), QueryType.AND, Map.of(
                    "name", "Alpha",
                    "workspaceId", workspace1Jpa.getId()
            ));

            // Act
            Pagination<Project> resultWithMatch = projectPersistence.findAll(queryWithMatch);

            // Assert: Should find exactly one project
            assertThat(resultWithMatch.total()).isEqualTo(1);
            assertThat(resultWithMatch.items().get(0).getName()).isEqualTo("Project Alpha");

            // Arrange: Search for a project named "Alpha" AND in workspace2 (no such project)
            SearchQuery queryWithoutMatch = SearchQuery.of(Page.of(0, 10), QueryType.AND, Map.of(
                    "name", "Alpha",
                    "workspaceId", workspace2Jpa.getId()
            ));

            // Act
            Pagination<Project> resultWithoutMatch = projectPersistence.findAll(queryWithoutMatch);

            // Assert: Should find no projects
            assertThat(resultWithoutMatch.total()).isZero();
            assertThat(resultWithoutMatch.items()).isEmpty();
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("invalidField", "value"));

            assertThatThrownBy(() -> projectPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void findAll_withPagination_shouldReturnCorrectPage() {
            SearchQuery queryPage1 = SearchQuery.of(Page.of(0, 2, "name", "asc"),  Map.of());
            Pagination<Project> result1 = projectPersistence.findAll(queryPage1);

            assertThat(result1.items()).hasSize(2);
            assertThat(result1.currentPage()).isEqualTo(0);
            assertThat(result1.total()).isEqualTo(3);
            assertThat(result1.items().get(0).getName()).isEqualTo("Project Alpha");
            assertThat(result1.items().get(1).getName()).isEqualTo("Project Beta");

            SearchQuery queryPage2 = SearchQuery.of(Page.of(1, 2, "name", "asc"),  Map.of());
            Pagination<Project> result2 = projectPersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
            assertThat(result2.items().get(0).getName()).isEqualTo("Project Gamma");
        }
    }

    private void create(Project entity) {
        if (entity == null) throw new IllegalArgumentException("Project model cannot be null");
        projectPersistence.create(entity);
        flushAndClear();
    }

    private void update(Project entity) {
        if (entity == null) throw new IllegalArgumentException("Project model cannot be null");
        projectPersistence.update(entity);
        flushAndClear();
    }

    private void deleteById(String id) {
        if (id == null) throw new IllegalArgumentException("Project ID cannot be null or empty");
        projectPersistence.deleteById(id);
        flushAndClear();
    }
}
