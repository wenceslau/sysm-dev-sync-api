package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.PersistenceTest;
// Import your UserJpaEntity if it's in a different package
// import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.UserJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(UserPersistence.class)
public class UserPersistenceTest extends AbstractRepositoryTest {

    @Autowired
    private UserPersistence userPersistence;

    private User user1Domain;

    private UserJpaEntity user1Jpa;
    private UserJpaEntity user2Jpa;
    private UserJpaEntity user3Jpa;

    @BeforeEach
    void setUp() {
        userJpaRepository.deleteAllInBatch();

        // Using User.create for initial setup, password and profile picture are null
        user1Domain = User.create("John Doe", "john.doe@example.com", UserRole.MEMBER);
        User user2Domain = User.create("Alice Smith", "alice.smith@example.com", UserRole.ADMIN);
        User user3Domain = User.create("Bob Johnson", "bob.johnson@example.com", UserRole.MEMBER);

        // Manually set IDs if User.create doesn't return the created user with ID immediately
        // or if we need predictable IDs for testing (though UUIDs are fine for most cases)
        // For this example, we'll assume User.create assigns an ID that we can retrieve.

        user1Jpa = UserJpaEntity.fromModel(user1Domain);
        user2Jpa = UserJpaEntity.fromModel(user2Domain);
        user3Jpa = UserJpaEntity.fromModel(user3Domain);
    }

    private UserJpaEntity persistUser(UserJpaEntity userJpa) {
        return entityManager.persistAndFlush(userJpa);
    }

    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a user")
        void create_shouldSaveUser() {
            User newUser = User.create("Test User", "test.user@example.com", UserRole.MEMBER);
            // Act
            assertDoesNotThrow(() -> userPersistence.create(newUser));

            // Assert
            UserJpaEntity foundInDb = entityManager.find(UserJpaEntity.class, newUser.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getName()).isEqualTo(newUser.getName());
            assertThat(foundInDb.getEmail()).isEqualTo(newUser.getEmail());

            Optional<User> foundUser = userPersistence.findById(newUser.getId());
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getName()).isEqualTo(newUser.getName());
            assertThat(foundUser.get().getEmail()).isEqualTo(newUser.getEmail());
            assertThat(foundUser.get().getRole()).isEqualTo(UserRole.MEMBER);
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing user")
        void update_shouldModifyExistingUser() {
            // Arrange
            persistUser(user1Jpa); // Persist initial user1
            entityManager.detach(user1Jpa); // Detach to simulate fetching and updating

            // Create an updated version of user1Domain using User.build to include all fields
            // We need to fetch the original createdAt time.
            Instant originalCreatedAt = user1Domain.getCreatedAt();
            User updatedDomainUser = User.build(
                    user1Domain.getId(),
                    originalCreatedAt, // Keep original creation time
                    Instant.now(),    // New update time
                    "Johnathan Doe Updated",
                    "john.doe.new@example.com",
                    "newPasswordHash", // Example password hash
                    "newProfilePic.jpg", // Example profile pic
                    UserRole.ADMIN      // Example role change
            );


            // Act
            userPersistence.update(updatedDomainUser);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<User> foundUser = userPersistence.findById(user1Domain.getId());
            assertThat(foundUser).isPresent();
            User retrievedUser = foundUser.get();
            assertThat(retrievedUser.getName()).isEqualTo("Johnathan Doe Updated");
            assertThat(retrievedUser.getEmail()).isEqualTo("john.doe.new@example.com");
            assertThat(retrievedUser.getPasswordHash()).isEqualTo("newPasswordHash");
            assertThat(retrievedUser.getProfilePictureUrl()).isEqualTo("newProfilePic.jpg");
            assertThat(retrievedUser.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(retrievedUser.getCreatedAt()).isEqualTo(originalCreatedAt); // CreatedAt should not change
            assertThat(retrievedUser.getUpdatedAt()).isAfter(originalCreatedAt); // UpdatedAt should be newer
        }
    }

    @Nested
    @DisplayName("deleteById Method Tests")
    class DeleteByIdTests {
        @Test
        @DisplayName("should delete a user by its ID")
        void deleteById_shouldRemoveUser() {
            // Arrange
            UserJpaEntity persistedUser = persistUser(user1Jpa);

            // Act
            userPersistence.deleteById(persistedUser.getId());
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<User> foundUser = userPersistence.findById(persistedUser.getId());
            assertThat(foundUser).isNotPresent();
            assertThat(userPersistence.existsById(persistedUser.getId())).isFalse();
            assertThat(entityManager.find(UserJpaEntity.class, persistedUser.getId())).isNull();
        }
    }

    @Nested
    @DisplayName("findById Method Tests")
    class FindByIdTests {
        @Test
        @DisplayName("should return user when found")
        void findById_whenUserExists_shouldReturnUser() {
            // Arrange
            persistUser(user1Jpa);

            // Act
            Optional<User> foundUser = userPersistence.findById(user1Domain.getId());

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getId()).isEqualTo(user1Domain.getId());
            assertThat(foundUser.get().getName()).isEqualTo(user1Domain.getName());
            assertThat(foundUser.get().getEmail()).isEqualTo(user1Domain.getEmail());
        }

        @Test
        @DisplayName("should return empty optional when user not found")
        void findById_whenUserDoesNotExist_shouldReturnEmpty() {
            // Act
            Optional<User> foundUser = userPersistence.findById(UUID.randomUUID().toString());

            // Assert
            assertThat(foundUser).isNotPresent();
        }
    }

    @Nested
    @DisplayName("existsById Method Tests")
    class ExistsByIdTests {
        @Test
        @DisplayName("should return true when user exists")
        void existsById_whenUserExists_shouldReturnTrue() {
            // Arrange
            persistUser(user1Jpa);

            // Act
            boolean exists = userPersistence.existsById(user1Domain.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when user does not exist")
        void existsById_whenUserDoesNotExist_shouldReturnFalse() {
            // Act
            boolean exists = userPersistence.existsById(UUID.randomUUID().toString());

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findAll Method Tests")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            // Data for these specific tests.
            // The main setUp already cleared the DB and re-initialized userXDomain/userXJpa instances.
            persistUser(user1Jpa); // John Doe, john.doe@example.com, MEMBER
            persistUser(user2Jpa); // Alice Smith, alice.smith@example.com, ADMIN
            persistUser(user3Jpa); // Bob Johnson, bob.johnson@example.com, MEMBER
        }

        @Test
        @DisplayName("should return all users when no search terms provided")
        void findAll_noTerms_shouldReturnAllUsers() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "");

            Pagination<User> result = userPersistence.findAll(query);

            assertThat(result.items()).hasSize(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by a single valid term (name as username)")
        void findAll_singleValidTermName_shouldReturnMatchingUsers() {
            // Assuming "username" in VALID_SEARCHABLE_FIELDS maps to User's "name" field
            SearchQuery query = new SearchQuery(Page.of(0, 10), "name=John Doe");

            Pagination<User> result = userPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getName()).isEqualTo("John Doe");
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("should filter by a single valid term (email) case-insensitive")
        void findAll_singleValidTermEmail_shouldReturnMatchingUsers() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "email=ALICE.SMITH@EXAMPLE.COM"); // Mixed case

            Pagination<User> result = userPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getEmail()).isEqualTo("alice.smith@example.com");
        }

        @Test
        @DisplayName("should filter by a single valid term (role)")
        void findAll_singleValidTermRole_shouldReturnMatchingUsers() {
            // Assuming "role" in VALID_SEARCHABLE_FIELDS maps to User's "userRole" field
            SearchQuery query = new SearchQuery(Page.of(0, 10), "role=ADMIN");

            Pagination<User> result = userPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(result.items().get(0).getName()).isEqualTo("Alice Smith");
        }


        @Test
        @DisplayName("should filter by multiple valid terms (OR logic)")
        void findAll_multipleValidTerms_OR_Logic_shouldReturnMatchingUsers() {
            // Assuming "username" (maps to name) and "email" are valid searchable fields
            SearchQuery query = new SearchQuery(Page.of(0, 10), "name=John Doe#email=alice.smith@example.com");

            Pagination<User> result = userPersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            assertThat(result.items()).extracting(User::getName).containsExactlyInAnyOrder("John Doe", "Alice Smith");
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "nonExistentField=testValue");

            assertThatThrownBy(() -> userPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'nonExistentField'");
        }

        @Test
        @DisplayName("should handle terms with no matches")
        void findAll_termWithNoMatches_shouldReturnEmptyPage() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "name=NoOneLikeThis");

            Pagination<User> result = userPersistence.findAll(query);

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void findAll_withPagination_shouldReturnCorrectPage() {
            SearchQuery query = new SearchQuery(Page.of(0, 2, "name", "asc"), ""); // Sort by name (maps to username)

            Pagination<User> result = userPersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            assertThat(result.currentPage()).isEqualTo(0);
            assertThat(result.perPage()).isEqualTo(2);
            assertThat(result.total()).isEqualTo(3);

            SearchQuery queryPage2 = new SearchQuery(Page.of(1, 2, "name", "asc"), "");
            Pagination<User> result2 = userPersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
        }

        @Test
        @DisplayName("should respect sorting parameters (name ascending)")
        void findAll_withSortingNameAsc_shouldReturnSortedUsers() {
            User extraUserDomain = User.create("Aaron Aardvark", "aaron@example.com", UserRole.MEMBER);
            persistUser(UserJpaEntity.fromModel(extraUserDomain));

            SearchQuery query = new SearchQuery(Page.of(0, 10, "name", "asc"), ""); // Sort by name
            Pagination<User> result = userPersistence.findAll(query);

            List<String> names = result.items().stream().map(User::getName).toList();
            assertThat(names).isSorted();
            // Expected order: Aaron Aardvark, Alice Smith, Bob Johnson, John Doe
            assertThat(names).containsExactly("Aaron Aardvark", "Alice Smith", "Bob Johnson", "John Doe");
        }
    }
}
