package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.TagJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(TagPersistence.class)
public class TagPersistenceTest extends AbstractRepositoryTest {

    @Autowired
    private TagPersistence tagPersistence; // The class under test

    private Tag tag1Domain;

    // JPA entities are useful for setup with TestEntityManager
    private TagJpaEntity tag1Jpa;
    private TagJpaEntity tag2Jpa;
    private TagJpaEntity tag3Jpa;

    @BeforeEach
    void setUp() {
        clearRepositories();

        tag1Domain = Tag.build(UUID.randomUUID().toString(), "Java", "Blue", "Java Programming Language", "Programming", 0);
        Tag tag2Domain = Tag.build(UUID.randomUUID().toString(), "Spring", "Green", "Spring Framework", "Framework", 0);
        Tag tag3Domain = Tag.build(UUID.randomUUID().toString(), "Testing", "Red", "Software Testing", "Programming", 0);

        tag1Jpa = TagJpaEntity.fromModel(tag1Domain);
        tag2Jpa = TagJpaEntity.fromModel(tag2Domain);
        tag3Jpa = TagJpaEntity.fromModel(tag3Domain);
    }

    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a tag")
        void create_shouldSaveTag() {
            // Act
            assertDoesNotThrow(() -> create(tag1Domain));

            // Assert
            // Verify directly with the repository or TestEntityManager if needed,
            // or through the persistence port method as you are doing.
            TagJpaEntity foundInDb = entityManager.find(TagJpaEntity.class, tag1Domain.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getName()).isEqualTo(tag1Domain.getName());

            Optional<Tag> foundTag = tagPersistence.findById(tag1Domain.getId());
            assertThat(foundTag).isPresent();
            assertThat(foundTag.get().getName()).isEqualTo(tag1Domain.getName());
            assertThat(foundTag.get().getColor()).isEqualTo(tag1Domain.getColor());
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing tag")
        void update_shouldModifyExistingTag() {
            // Arrange
            entityPersist(tag1Jpa); // tag1Jpa is now persisted

            Tag updatedDomainTag = Tag.build(
                    tag1Domain.getId(), // Use the ID of the persisted tag
                    "Java Updated",
                    "Dark Blue",
                    "Updated Desc",
                    "Tech",
                    1
            );

            // Act
            update(updatedDomainTag);

            // Assert
            Optional<Tag> foundTag = tagPersistence.findById(tag1Domain.getId());
            assertThat(foundTag).isPresent();
            assertThat(foundTag.get().getName()).isEqualTo("Java Updated");
            assertThat(foundTag.get().getColor()).isEqualTo("Dark Blue");
            // ... other assertions
        }
    }

    @Nested
    @DisplayName("deleteById Method Tests")
    class DeleteByIdTests {
        @Test
        @DisplayName("should delete a tag by its ID")
        void deleteById_shouldRemoveTag() {
            // Arrange
            entityPersist(tag1Jpa);

            // Act
            deleteById(tag1Jpa.getId());

            // Assert
            Optional<Tag> foundTag = tagPersistence.findById(tag1Jpa.getId());
            assertThat(foundTag).isNotPresent();
            assertThat(tagPersistence.existsById(tag1Jpa.getId())).isFalse();
            assertThat(entityManager.find(TagJpaEntity.class, tag1Jpa.getId())).isNull();
        }
    }

    @Nested
    @DisplayName("findById Method Tests")
    class FindByIdTests {
        @Test
        @DisplayName("should return tag when found")
        void findById_whenTagExists_shouldReturnTag() {
            // Arrange
            entityPersist(tag1Jpa);

            // Act
            Optional<Tag> foundTag = tagPersistence.findById(tag1Domain.getId());

            // Assert
            assertThat(foundTag).isPresent();
            assertThat(foundTag.get().getId()).isEqualTo(tag1Domain.getId());
            assertThat(foundTag.get().getName()).isEqualTo(tag1Domain.getName());
        }

        @Test
        @DisplayName("should return empty optional when tag not found")
        void findById_whenTagDoesNotExist_shouldReturnEmpty() {
            // Act
            Optional<Tag> foundTag = tagPersistence.findById(UUID.randomUUID().toString());

            // Assert
            assertThat(foundTag).isNotPresent();
        }
    }

    @Nested
    @DisplayName("existsById Method Tests")
    class ExistsByIdTests {
        @Test
        @DisplayName("should return true when tag exists")
        void existsById_whenTagExists_shouldReturnTrue() {
            // Arrange
            entityPersist(tag1Jpa);

            // Act
            boolean exists = tagPersistence.existsById(tag1Domain.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when tag does not exist")
        void existsById_whenTagDoesNotExist_shouldReturnFalse() {
            // Act
            boolean exists = tagPersistence.existsById(UUID.randomUUID().toString());

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findAll Method Tests")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            // Persist test data using TestEntityManager
            entityPersist(tag1Jpa); // Java, Blue, Programming
            entityPersist(tag2Jpa); // Spring, Green, Framework
            entityPersist(tag3Jpa); // Testing, Red, Programming
        }

        @Test
        @DisplayName("should return all tags when no search terms provided")
        void findAll_noTerms_shouldReturnAllTags() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), ""); // Empty terms

            Pagination<Tag> result = tagPersistence.findAll(query);

            assertThat(result.items()).hasSize(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by a single valid term (name)")
        void findAll_singleValidTermName_shouldReturnMatchingTags() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "name=Java");

            Pagination<Tag> result = tagPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getName()).isEqualTo("Java");
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("should filter by a single valid term (category) case-insensitive")
        void findAll_singleValidTermCategory_shouldReturnMatchingTags() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "category=programming"); // lowercase

            Pagination<Tag> result = tagPersistence.findAll(query);

            assertThat(result.items()).hasSize(2); // Java and Testing
            assertThat(result.items()).extracting(Tag::getCategory).containsOnly("Programming");
        }

        @Test
        @DisplayName("should filter by multiple valid terms (OR logic)")
        void findAll_multipleValidTerms_OR_Logic_shouldReturnMatchingTags() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "name=Java#color=Green");

            Pagination<Tag> result = tagPersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            assertThat(result.items()).extracting(Tag::getName).containsExactlyInAnyOrder("Java", "Spring");
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "invalidField=test");

            assertThatThrownBy(() -> tagPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should handle terms with no matches")
        void findAll_termWithNoMatches_shouldReturnEmptyPage() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "name=NonExistent");

            Pagination<Tag> result = tagPersistence.findAll(query);

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void findAll_withPagination_shouldReturnCorrectPage() {
            SearchQuery query = new SearchQuery(Page.of(0, 2, "name", "asc"), "");

            Pagination<Tag> result = tagPersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            assertThat(result.currentPage()).isEqualTo(0);
            assertThat(result.perPage()).isEqualTo(2);
            assertThat(result.total()).isEqualTo(3);

            SearchQuery queryPage2 = new SearchQuery(Page.of(1, 2, "name", "asc"), "");
            Pagination<Tag> result2 = tagPersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
        }

        @Test
        @DisplayName("should respect sorting parameters (name ascending)")
        void findAll_withSortingNameAsc_shouldReturnSortedTags() {
            // Persist an additional tag for a more robust sort test
            TagJpaEntity appleTagJpa = TagJpaEntity.fromModel(Tag.create("Apple", "Red"));
            entityPersist(appleTagJpa); // Persisted "Apple"

            // Now we have "Java", "Spring", "Testing", "Apple"
            SearchQuery query = new SearchQuery(Page.of(0, 10, "name", "asc"), "");
            Pagination<Tag> result = tagPersistence.findAll(query);

            List<String> names = result.items().stream().map(Tag::getName).toList();
            assertThat(names).isSorted();
            assertThat(names).containsExactly("Apple", "Java", "Spring", "Testing");
        }
    }

    private void create(Tag entity) {
        tagPersistence.create(entity);
        flushAndClear();
    }

    private void update(Tag entity) {
        tagPersistence.update(entity);
        flushAndClear();
    }

    private void deleteById(String id) {
        tagPersistence.deleteById(id);
        flushAndClear();
    }
}
