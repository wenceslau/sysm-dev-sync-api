package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.infrastructure.PersistenceTest;
import com.sysm.devsync.infrastructure.Utils;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.sysm.devsync.infrastructure.Utils.ldtNow;
import static com.sysm.devsync.infrastructure.Utils.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@PersistenceTest
public class UserJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserJpaEntity user1;
    private UserJpaEntity user2;
    private UserJpaEntity user3;

    @BeforeEach
    void setUp() {
        // While @DataJpaTest handles rollbacks, explicit deleteAll can ensure a clean slate
        // if there are any doubts or specific test environment behaviors.
        userJpaRepository.deleteAllInBatch();

        user1 = new UserJpaEntity();
        user1.setId(UUID.randomUUID().toString());
        user1.setName("John Doe");
        user1.setEmail("john.doe@example.com");
        user1.setPasswordHash("hashedpassword1");
        user1.setRole(UserRole.MEMBER);
        user1.setCreatedAt(LocalDateTime.now().minusDays(2));
        user1.setUpdatedAt(LocalDateTime.now().minusDays(1));

        user2 = new UserJpaEntity();
        user2.setId(UUID.randomUUID().toString());
        user2.setName("Alice Smith");
        user2.setEmail("alice.smith@example.com");
        user2.setPasswordHash("hashedpassword2");
        user2.setRole(UserRole.ADMIN);
        user2.setCreatedAt(LocalDateTime.now().minusDays(3));
        user2.setUpdatedAt(LocalDateTime.now().minusDays(2));

        user3 = new UserJpaEntity();
        user3.setId(UUID.randomUUID().toString());
        user3.setName("Bob Johnson");
        user3.setEmail("bob.johnson@example.com");
        user3.setPasswordHash("hashedpassword3");
        user3.setRole(UserRole.MEMBER);
        user3.setCreatedAt(LocalDateTime.now().minusDays(1));
        user3.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("should save a user and find it by id")
    void saveAndFindById() {
        // Arrange
        UserJpaEntity savedUser = entityManager.persistAndFlush(user1);

        // Act
        Optional<UserJpaEntity> foundUser = userJpaRepository.findById(savedUser.getId());

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo(user1.getName());
        assertThat(foundUser.get().getEmail()).isEqualTo(user1.getEmail());
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("should return empty optional when finding non-existent user by id")
    void findById_whenUserDoesNotExist_shouldReturnEmpty() {
        // Act
        Optional<UserJpaEntity> foundUser = userJpaRepository.findById(UUID.randomUUID().toString());

        // Assert
        assertThat(foundUser).isNotPresent();
    }

    @Test
    @DisplayName("should find all saved users")
    void findAll_shouldReturnAllUsers() {
        // Arrange
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        // Act
        List<UserJpaEntity> users = userJpaRepository.findAll();

        // Assert
        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserJpaEntity::getName).containsExactlyInAnyOrder("John Doe", "Alice Smith");
    }

    @Test
    @DisplayName("should return empty list when no users are saved")
    void findAll_whenNoUsers_shouldReturnEmptyList() {
        // Act
        List<UserJpaEntity> users = userJpaRepository.findAll();

        // Assert
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("should update an existing user")
    void updateUser() {
        // Arrange
        UserJpaEntity persistedUser = entityManager.persistAndFlush(user1);
        String updatedEmail = "john.doe.updated@example.com";
        UserRole updatedRole = UserRole.ADMIN;

        // Act
        Optional<UserJpaEntity> userToUpdateOpt = userJpaRepository.findById(persistedUser.getId());
        assertThat(userToUpdateOpt).isPresent();

        UserJpaEntity userToUpdate = userToUpdateOpt.get();
        userToUpdate.setEmail(updatedEmail);
        userToUpdate.setRole(updatedRole);
        userToUpdate.setUpdatedAt(ldtNow().plusDays(1));
        userJpaRepository.save(userToUpdate);
        entityManager.flush();
        entityManager.clear();

        sleep(100); // Ensure updatedAt is different

        // Assert
        Optional<UserJpaEntity> updatedUserOpt = userJpaRepository.findById(persistedUser.getId());
        assertThat(updatedUserOpt).isPresent();
        assertThat(updatedUserOpt.get().getEmail()).isEqualTo(updatedEmail);
        assertThat(updatedUserOpt.get().getRole()).isEqualTo(updatedRole);
        assertThat(updatedUserOpt.get().getUpdatedAt()).isAfter(persistedUser.getUpdatedAt());
    }

    @Test
    @DisplayName("should delete a user by id")
    void deleteById() {
        // Arrange
        UserJpaEntity persistedUser = entityManager.persistAndFlush(user1);
        String idToDelete = persistedUser.getId();

        // Act
        userJpaRepository.deleteById(idToDelete);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<UserJpaEntity> deletedUser = userJpaRepository.findById(idToDelete);
        assertThat(deletedUser).isNotPresent();
    }

    @Test
    @DisplayName("existsById should return true for existing user")
    void existsById_whenUserExists_shouldReturnTrue() {
        // Arrange
        UserJpaEntity persistedUser = entityManager.persistAndFlush(user1);

        // Act
        boolean exists = userJpaRepository.existsById(persistedUser.getId());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById should return false for non-existing user")
    void existsById_whenUserDoesNotExist_shouldReturnFalse() {
        // Act
        boolean exists = userJpaRepository.existsById(UUID.randomUUID().toString());

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("should find all users matching specification with pagination")
    void findAll_withSpecificationAndPageable() {
        // Arrange
        entityManager.persist(user1); // John Doe, MEMBER
        entityManager.persist(user2); // Alice Smith, ADMIN
        entityManager.persist(user3); // Bob Johnson, MEMBER
        entityManager.flush();

        // Specification to find users with role ADMIN or name containing "Doe"
        Specification<UserJpaEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("role"), UserRole.ADMIN));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%doe%"));
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(0, 2); // First page, 2 items per page

        // Act
        Page<UserJpaEntity> resultPage = userJpaRepository.findAll(spec, pageable);

        // Assert
        assertThat(resultPage).isNotNull();
        // Expecting "John Doe" (name match) and "Alice Smith" (role match)
        assertThat(resultPage.getContent()).hasSize(2);
        assertThat(resultPage.getContent()).extracting(UserJpaEntity::getName).containsExactlyInAnyOrder("John Doe", "Alice Smith");
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getTotalPages()).isEqualTo(1);
        assertThat(resultPage.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("findAll with specification should return empty page if no matches")
    void findAll_withSpecification_noMatches() {
        // Arrange
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        Specification<UserJpaEntity> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("email"), "nonexistent@example.com");
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserJpaEntity> resultPage = userJpaRepository.findAll(spec, pageable);

        // Assert
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getContent()).isEmpty();
        assertThat(resultPage.getTotalElements()).isEqualTo(0);
    }
}
