package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QueryType;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.models.to.UserTO;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.sysm.devsync.infrastructure.Utils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(WorkspacePersistence.class)
public class WorkspacePersistenceTest extends AbstractRepositoryTest {

    @Autowired
    private WorkspacePersistence workspacePersistence;

    private UserJpaEntity ownerUser;
    private UserJpaEntity memberUser1;
    private UserJpaEntity memberUser2;

    private Workspace workspace1Domain;
    private Workspace workspace2Domain;
    private Workspace workspace3Domain;

    @BeforeEach
    void setUp() {
        clearRepositories();

        ownerUser = new UserJpaEntity();
        ownerUser.setId(UUID.randomUUID().toString());
        ownerUser.setName("Owner User");
        ownerUser.setEmail("owner@example.com");
        ownerUser.setRole(UserRole.ADMIN);
        ownerUser.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        ownerUser.setUpdatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entityPersist(ownerUser);

        memberUser1 = new UserJpaEntity();
        memberUser1.setId(UUID.randomUUID().toString());
        memberUser1.setName("Member One");
        memberUser1.setEmail("member1@example.com");
        memberUser1.setRole(UserRole.MEMBER);
        memberUser1.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        memberUser1.setUpdatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entityPersist(memberUser1);

        memberUser2 = new UserJpaEntity();
        memberUser2.setId(UUID.randomUUID().toString());
        memberUser2.setName("Member Two");
        memberUser2.setEmail("member2@example.com");
        memberUser2.setRole(UserRole.MEMBER);
        memberUser2.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        memberUser2.setUpdatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entityPersist(memberUser2);

        workspace1Domain = Workspace.create("Workspace Alpha", "Alpha description", false, ownerUser.getId());
        workspace1Domain.addMember(memberUser1.getId());

        workspace2Domain = Workspace.create("Workspace Beta", "Beta description", true, ownerUser.getId());
        workspace2Domain.addMember(memberUser1.getId());
        workspace2Domain.addMember(memberUser2.getId());

        workspace3Domain = Workspace.create("Workspace Gamma", "Another description", false, memberUser1.getId()); // Different owner
    }

    // --- Basic CRUD tests remain the same, they are correct ---
    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a workspace")
        void create_shouldSaveWorkspace() {
            // Act
            assertDoesNotThrow(() -> create(workspace1Domain));

            // Assert
            WorkspaceJpaEntity foundInDb = entityManager.find(WorkspaceJpaEntity.class, workspace1Domain.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getName()).isEqualTo(workspace1Domain.getName());
            assertThat(foundInDb.getOwner().getId()).isEqualTo(workspace1Domain.getOwner().id());
            assertThat(foundInDb.getMembers().stream().map(UserJpaEntity::getId).collect(Collectors.toSet()))
                    .isEqualTo(workspace1Domain.getMembersId());
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing workspace")
        void update_shouldModifyExistingWorkspace() {
            // Arrange
            create(workspace1Domain);
            sleep(10);
            var originalWorkspace = workspacePersistence.findById(workspace1Domain.getId()).orElseThrow();
            Instant originalCreatedAt = originalWorkspace.getCreatedAt();

            Set<UserTO> updatedMembers = new HashSet<>(Collections.singletonList(UserTO.of(memberUser2.getId())));
            Workspace updatedDomainWorkspace = Workspace.build(
                    workspace1Domain.getId(),
                    originalCreatedAt,
                    Instant.now(),
                    "Workspace Alpha Updated",
                    "Updated Alpha Description",
                    true,
                    UserTO.of(memberUser1.getId()),
                    updatedMembers
            );

            // Act
            update(updatedDomainWorkspace);

            // Assert
            Workspace foundWorkspace = workspacePersistence.findById(workspace1Domain.getId()).orElseThrow();
            assertThat(foundWorkspace.getName()).isEqualTo("Workspace Alpha Updated");
            assertThat(foundWorkspace.isPrivate()).isTrue();
            assertThat(foundWorkspace.getOwner().id()).isEqualTo(memberUser1.getId());
            assertThat(foundWorkspace.getMembersId()).isEqualTo(updatedMembers.stream().map(UserTO::id).collect(Collectors.toSet()));
            assertThat(foundWorkspace.getUpdatedAt()).isAfter(originalCreatedAt);
        }
    }

    @Nested
    @DisplayName("deleteById Method Tests")
    class DeleteByIdTests {
        @Test
        @DisplayName("should delete a workspace by its ID")
        void deleteById_shouldRemoveWorkspace() {
            create(workspace1Domain);
            deleteById(workspace1Domain.getId());
            assertThat(workspacePersistence.existsById(workspace1Domain.getId())).isFalse();
        }
    }

    // --- findById and existsById tests are also correct ---

    @Nested
    @DisplayName("findAll Method Tests")
    class FindAllTests {

        @BeforeEach
        void setUpFindAll() {
            create(workspace1Domain); // Alpha, false, ownerUser, member1
            create(workspace2Domain); // Beta, true, ownerUser, member1, member2
            create(workspace3Domain); // Gamma, false, member1
        }

        @Test
        @DisplayName("should return all workspaces when no search terms provided")
        void findAll_noTerms_shouldReturnAllWorkspaces() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of());
            Pagination<Workspace> result = workspacePersistence.findAll(query);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by 'isPrivate' correctly")
        void findAll_byIsPrivate_shouldReturnMatchingWorkspaces() {
            // Arrange: Search for private workspaces
            SearchQuery queryTrue = SearchQuery.of(Page.of(0, 10), Map.of("isPrivate", "true"));

            // Act
            Pagination<Workspace> resultTrue = workspacePersistence.findAll(queryTrue);

            // Assert
            assertThat(resultTrue.total()).isEqualTo(1);
            assertThat(resultTrue.items().get(0).getName()).isEqualTo("Workspace Beta");

            // Arrange: Search for public workspaces
            SearchQuery queryFalse = SearchQuery.of(Page.of(0, 10), Map.of("isPrivate", "false"));

            // Act
            Pagination<Workspace> resultFalse = workspacePersistence.findAll(queryFalse);

            // Assert
            assertThat(resultFalse.total()).isEqualTo(2);
            assertThat(resultFalse.items()).extracting(Workspace::getName)
                    .containsExactlyInAnyOrder("Workspace Alpha", "Workspace Gamma");
        }

        @Test
        @DisplayName("should filter by ownerId")
        void findAll_byOwnerId_shouldReturnMatchingWorkspaces() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("ownerId", ownerUser.getId()));
            Pagination<Workspace> result = workspacePersistence.findAll(query);

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.items()).extracting(Workspace::getName)
                    .containsExactlyInAnyOrder("Workspace Alpha", "Workspace Beta");
        }

        @Test
        @DisplayName("should filter by memberId")
        void findAll_byMemberId_shouldReturnMatchingWorkspaces() {
            // Arrange: Find workspaces where memberUser2 is a member
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("memberId", memberUser2.getId()));

            // Act
            Pagination<Workspace> result = workspacePersistence.findAll(query);

            // Assert: Only Workspace Beta should be returned
            assertThat(result.total()).isEqualTo(1);
            assertThat(result.items().get(0).getName()).isEqualTo("Workspace Beta");

            // Arrange: Find workspaces where memberUser1 is a member
            SearchQuery query2 = SearchQuery.of(Page.of(0, 10), Map.of("memberId", memberUser1.getId()));

            // Act
            Pagination<Workspace> result2 = workspacePersistence.findAll(query2);

            // Assert: Alpha and Beta should be returned
            assertThat(result2.total()).isEqualTo(2);
            assertThat(result2.items()).extracting(Workspace::getName)
                    .containsExactlyInAnyOrder("Workspace Alpha", "Workspace Beta");
        }

        @Test
        @DisplayName("should filter by multiple valid terms using AND logic")
        void findAll_withMultipleTerms_shouldReturnAndedResults() {
            // Arrange: Search for a public workspace (isPrivate=false) owned by ownerUser
            SearchQuery queryWithMatch = SearchQuery.of(Page.of(0, 10), QueryType.AND, Map.of(
                    "isPrivate", "false",
                    "ownerId", ownerUser.getId()
            ));

            // Act
            Pagination<Workspace> resultWithMatch = workspacePersistence.findAll(queryWithMatch);

            // Assert: Should find exactly one workspace: "Workspace Alpha"
            assertThat(resultWithMatch.total()).isEqualTo(1);
            assertThat(resultWithMatch.items().get(0).getName()).isEqualTo("Workspace Alpha");

            // Arrange: Search for a private workspace (isPrivate=true) owned by memberUser1 (no such workspace)
            SearchQuery queryWithoutMatch = SearchQuery.of(Page.of(0, 10), QueryType.AND, Map.of(
                    "isPrivate", "true",
                    "ownerId", memberUser1.getId()
            ));

            // Act
            Pagination<Workspace> resultWithoutMatch = workspacePersistence.findAll(queryWithoutMatch);

            // Assert: Should find no workspaces
            assertThat(resultWithoutMatch.total()).isZero();
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("invalidField", "value"));

            assertThatThrownBy(() -> workspacePersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should respect pagination and sorting parameters")
        void findAll_withPaginationAndSorting_shouldReturnCorrectPage() {
            // Sort by name descending
            SearchQuery query = SearchQuery.of(Page.of(0, 2, "name", "desc"), Map.of());
            Pagination<Workspace> result = workspacePersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            assertThat(result.currentPage()).isEqualTo(0);
            assertThat(result.total()).isEqualTo(3);
            assertThat(result.items().get(0).getName()).isEqualTo("Workspace Gamma");
            assertThat(result.items().get(1).getName()).isEqualTo("Workspace Beta");
        }
    }

    private void create(Workspace entity) {
        workspacePersistence.create(entity);
        flushAndClear();
    }

    private void update(Workspace entity) {
        workspacePersistence.update(entity);
        flushAndClear();
    }

    private void deleteById(String id) {
        workspacePersistence.deleteById(id);
        flushAndClear();
    }
}
