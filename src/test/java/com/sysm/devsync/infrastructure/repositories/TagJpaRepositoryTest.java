package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.TagJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat; // Using AssertJ for fluent assertions

public class TagJpaRepositoryTest extends AbstractRepositoryTest {

    private TagJpaEntity tag1;
    private TagJpaEntity tag2;
    private TagJpaEntity tag3;

    @BeforeEach
    void setUp() {

        clearRepositories();

        tag1 = new TagJpaEntity();
        tag1.setId(UUID.randomUUID().toString());
        tag1.setName("Java");
        tag1.setColor("#FF0000");

        tag2 = new TagJpaEntity();
        tag2.setId(UUID.randomUUID().toString());
        tag2.setName("Spring Boot");
        tag2.setColor("#00FF00");

        tag3 = new TagJpaEntity();
        tag3.setId(UUID.randomUUID().toString());
        tag3.setName("Testing");
        tag3.setColor("#0000FF");
    }

    @Test
    @DisplayName("should save a tag and find it by id")
    void saveAndFindById() {
        // Arrange
        TagJpaEntity savedTag = entityManager.persistAndFlush(tag1); // Use entityManager to ensure it's persisted

        // Act
        Optional<TagJpaEntity> foundTag = tagJpaRepository.findById(savedTag.getId());

        // Assert
        assertThat(foundTag).isPresent();
        assertThat(foundTag.get().getName()).isEqualTo(tag1.getName());
        assertThat(foundTag.get().getId()).isEqualTo(savedTag.getId());
    }

    @Test
    @DisplayName("should return empty optional when finding non-existent tag by id")
    void findById_whenTagDoesNotExist_shouldReturnEmpty() {
        // Act
        Optional<TagJpaEntity> foundTag = tagJpaRepository.findById(UUID.randomUUID().toString());

        // Assert
        assertThat(foundTag).isNotPresent();
    }

    @Test
    @DisplayName("should find all saved tags")
    void findAll_shouldReturnAllTags() {
        // Arrange
        entityManager.persist(tag1);
        entityManager.persist(tag2);
        entityManager.flush();

        // Act
        List<TagJpaEntity> tags = tagJpaRepository.findAll();

        // Assert
        assertThat(tags).hasSize(2);
        assertThat(tags).extracting(TagJpaEntity::getName).containsExactlyInAnyOrder("Java", "Spring Boot");
    }

    @Test
    @DisplayName("should return empty list when no tags are saved")
    void findAll_whenNoTags_shouldReturnEmptyList() {
        // Act
        List<TagJpaEntity> tags = tagJpaRepository.findAll();

        // Assert
        assertThat(tags).isEmpty();
    }

    @Test
    @DisplayName("should update an existing tag")
    void updateTag() {
        // Arrange
        TagJpaEntity persistedTag = entityManager.persistAndFlush(tag1);
        String updatedName = "Java Programming";

        // Act
        // Retrieve, modify, and save
        Optional<TagJpaEntity> tagToUpdateOpt = tagJpaRepository.findById(persistedTag.getId());
        assertThat(tagToUpdateOpt).isPresent();
        TagJpaEntity tagToUpdate = tagToUpdateOpt.get();
        tagToUpdate.setName(updatedName);
        tagJpaRepository.save(tagToUpdate); // save can also be used for updates
        entityManager.flush(); // Ensure changes are written to DB for subsequent find
        entityManager.clear(); // Clear persistence context to ensure fresh load

        // Assert
        Optional<TagJpaEntity> updatedTagOpt = tagJpaRepository.findById(persistedTag.getId());
        assertThat(updatedTagOpt).isPresent();
        assertThat(updatedTagOpt.get().getName()).isEqualTo(updatedName);
    }

    @Test
    @DisplayName("should delete a tag by id")
    void deleteById() {
        // Arrange
        TagJpaEntity persistedTag = entityManager.persistAndFlush(tag1);
        String idToDelete = persistedTag.getId();

        // Act
        tagJpaRepository.deleteById(idToDelete);
        entityManager.flush(); // Ensure delete is processed
        entityManager.clear(); // Clear context

        // Assert
        Optional<TagJpaEntity> deletedTag = tagJpaRepository.findById(idToDelete);
        assertThat(deletedTag).isNotPresent();
    }

    @Test
    @DisplayName("existsById should return true for existing tag")
    void existsById_whenTagExists_shouldReturnTrue() {
        // Arrange
        TagJpaEntity persistedTag = entityManager.persistAndFlush(tag1);

        // Act
        boolean exists = tagJpaRepository.existsById(persistedTag.getId());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById should return false for non-existing tag")
    void existsById_whenTagDoesNotExist_shouldReturnFalse() {
        // Act
        boolean exists = tagJpaRepository.existsById(UUID.randomUUID().toString());

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("should find all tags matching specification with pagination")
    void findAll_withSpecificationAndPageable() {
        // Arrange
        entityManager.persist(tag1); // Name: Java
        entityManager.persist(tag2); // Name: Spring Boot
        entityManager.persist(tag3); // Name: Testing
        entityManager.flush();

        // Specification to find tags with name containing "Test" or "Java"
        Specification<TagJpaEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%test%"));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%java%"));
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(0, 2); // First page, 2 items per page

        // Act
        Page<TagJpaEntity> resultPage = tagJpaRepository.findAll(spec, pageable);

        // Assert
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getContent()).hasSize(2); // Should find "Java" and "Testing"
        assertThat(resultPage.getContent()).extracting(TagJpaEntity::getName).containsExactlyInAnyOrder("Java", "Testing");
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getTotalPages()).isEqualTo(1);
        assertThat(resultPage.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("findAll with specification should return empty page if no matches")
    void findAll_withSpecification_noMatches() {
        // Arrange
        entityManager.persist(tag1); // Name: Java
        entityManager.persist(tag2); // Name: Spring Boot
        entityManager.flush();

        Specification<TagJpaEntity> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("name"), "NonExistentTag");
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<TagJpaEntity> resultPage = tagJpaRepository.findAll(spec, pageable);

        // Assert
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getContent()).isEmpty();
        assertThat(resultPage.getTotalElements()).isEqualTo(0);
    }
}
