package com.sysm.devsync.application;

import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.controller.dto.CreateResponse;
import com.sysm.devsync.controller.dto.request.TagCreateUpdate;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagPersistencePort tagPersistence;

    @InjectMocks
    private TagService tagService;

    private TagCreateUpdate validTagCreateUpdateDto;
    private String tagId;

    @BeforeEach
    void setUp() {
        tagId = UUID.randomUUID().toString();
        validTagCreateUpdateDto = new TagCreateUpdate("Java", "#007396", "For Java programming", "Programming");
    }

    // --- createTag Tests ---

    @Test
    @DisplayName("createTag should create and save tag with all details")
    void createTag_shouldCreateAndSaveTag_withAllDetails() {
        // Arrange
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        // Mocking repository.create to simulate saving and allow capturing
        doNothing().when(tagPersistence).create(tagCaptor.capture());

        // Act
        CreateResponse response = tagService.createTag(validTagCreateUpdateDto);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());

        verify(tagPersistence, times(1)).create(any(Tag.class));
        Tag capturedTag = tagCaptor.getValue();

        assertEquals(validTagCreateUpdateDto.name(), capturedTag.getName());
        assertEquals(validTagCreateUpdateDto.color(), capturedTag.getColor());
        assertEquals(validTagCreateUpdateDto.description(), capturedTag.getDescription());
        assertEquals(validTagCreateUpdateDto.category(), capturedTag.getCategory());
        assertEquals(0, capturedTag.getCountUsage()); // Default from Tag.create
        assertEquals(response.id(), capturedTag.getId());
    }

    @Test
    @DisplayName("createTag should create and save tag with only name and color")
    void createTag_shouldCreateAndSaveTag_withOnlyNameAndColor() {
        // Arrange
        TagCreateUpdate minimalDto = new TagCreateUpdate("Python", "#3572A5", null, null);
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        doNothing().when(tagPersistence).create(tagCaptor.capture());

        // Act
        CreateResponse response = tagService.createTag(minimalDto);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());

        verify(tagPersistence, times(1)).create(any(Tag.class));
        Tag capturedTag = tagCaptor.getValue();

        assertEquals(minimalDto.name(), capturedTag.getName());
        assertEquals(minimalDto.color(), capturedTag.getColor());
        assertNull(capturedTag.getDescription());
        assertNull(capturedTag.getCategory());
        assertEquals(response.id(), capturedTag.getId());
    }

    @Test
    @DisplayName("createTag should propagate IllegalArgumentException from Tag.create for invalid name")
    void createTag_shouldPropagateException_forInvalidName() {
        // Arrange
        TagCreateUpdate invalidDto = new TagCreateUpdate(null, "#FF0000", "Desc", "Cat");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tagService.createTag(invalidDto);
        });
        assertEquals("Name cannot be null or empty", exception.getMessage());
        verify(tagPersistence, never()).create(any()); // Ensure repository create is not called
    }

    // --- updateTag Tests ---

    @Test
    @DisplayName("updateTag should update existing tag with all details")
    void updateTag_shouldUpdateExistingTag_withAllDetails() {
        // Arrange
        Tag existingTag = Tag.create("OldName", "#OldColor"); // ID will be generated
        existingTag.updateDescription("Old Description");
        existingTag.updateCategory("Old Category");

        when(tagPersistence.findById(tagId)).thenReturn(Optional.of(existingTag));
        doNothing().when(tagPersistence).update(any(Tag.class));
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);

        TagCreateUpdate updateDto = new TagCreateUpdate("NewName", "#NewColor", "New Description", "New Category");

        // Act
        tagService.updateTag(tagId, updateDto);

        // Assert

        verify(tagPersistence, times(1)).findById(tagId);
        verify(tagPersistence, times(1)).update(tagCaptor.capture());

        Tag capturedTag = tagCaptor.getValue();
        assertEquals(updateDto.name(), capturedTag.getName());
        assertEquals(updateDto.color(), capturedTag.getColor());
        assertEquals(updateDto.description(), capturedTag.getDescription());
        assertEquals(updateDto.category(), capturedTag.getCategory());
        assertSame(existingTag, capturedTag, "Should be the same instance being updated");
    }

    @Test
    @DisplayName("updateTag should update existing tag, keeping old description/category if DTO fields are blank/null")
    void updateTag_shouldKeepOldDetails_ifDtoFieldsAreBlank() {
        // Arrange
        String originalDescription = "Original Description";
        String originalCategory = "Original Category";
        Tag existingTag = Tag.build(tagId, "OldName", "#OldColor", originalDescription, originalCategory, 0);


        when(tagPersistence.findById(tagId)).thenReturn(Optional.of(existingTag));
        doNothing().when(tagPersistence).update(any(Tag.class));
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);

        TagCreateUpdate updateDto = new TagCreateUpdate("NewName", "#NewColor", " ", null); // Blank description, null category

        // Act
        tagService.updateTag(tagId, updateDto);

        // Assert
        verify(tagPersistence, times(1)).update(tagCaptor.capture());
        Tag capturedTag = tagCaptor.getValue();

        assertEquals("NewName", capturedTag.getName());
        assertEquals("#NewColor", capturedTag.getColor());
        assertEquals(originalDescription, capturedTag.getDescription(), "Description should not change if DTO description is blank");
        assertEquals(originalCategory, capturedTag.getCategory(), "Category should not change if DTO category is null");
    }


    @Test
    @DisplayName("updateTag should throw IllegalArgumentException if tag not found")
    void updateTag_shouldThrowException_ifTagNotFound() {
        // Arrange
        when(tagPersistence.findById(tagId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tagService.updateTag(tagId, validTagCreateUpdateDto);
        });
        assertEquals("Tag not found", exception.getMessage());
        verify(tagPersistence, never()).update(any());
    }

    @Test
    @DisplayName("updateTag should propagate IllegalArgumentException from Tag.update for invalid name")
    void updateTag_shouldPropagateException_forInvalidNameInUpdate() {
        // Arrange
        Tag existingTag = Tag.create("OldName", "#OldColor");
        when(tagPersistence.findById(tagId)).thenReturn(Optional.of(existingTag));
        TagCreateUpdate invalidUpdateDto = new TagCreateUpdate(null, "#NewColor", "Desc", "Cat");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tagService.updateTag(tagId, invalidUpdateDto);
        });
        assertEquals("Name cannot be null or empty", exception.getMessage());
        verify(tagPersistence, never()).update(any());
    }

    // --- deleteTag Tests ---

    @Test
    @DisplayName("deleteTag should call repository deleteById")
    void deleteTag_shouldCallRepositoryDeleteById() {
        // Arrange
        doNothing().when(tagPersistence).deleteById(tagId);

        // Act
        tagService.deleteTag(tagId);

        // Assert
        verify(tagPersistence, times(1)).deleteById(tagId);
    }

    // --- getTagById Tests ---

    @Test
    @DisplayName("getTagById should return tag if found")
    void getTagById_shouldReturnTag_ifFound() {
        // Arrange
        Tag expectedTag = Tag.create("TestTag", "#TestColor");
        when(tagPersistence.findById(tagId)).thenReturn(Optional.of(expectedTag));

        // Act
        Tag actualTag = tagService.getTagById(tagId);

        // Assert
        assertNotNull(actualTag);
        assertSame(expectedTag, actualTag);
        verify(tagPersistence, times(1)).findById(tagId);
    }

    @Test
    @DisplayName("getTagById should throw IllegalArgumentException if tag not found")
    void getTagById_shouldThrowException_ifTagNotFound() {
        // Arrange
        when(tagPersistence.findById(tagId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tagService.getTagById(tagId);
        });
        assertEquals("Tag not found", exception.getMessage());
        verify(tagPersistence, times(1)).findById(tagId);
    }

    // --- getAllTags Tests ---

    @Test
    @DisplayName("getAllTags should return pagination result from repository")
    void getAllTags_shouldReturnPaginationResult_fromRepository() {
        // Arrange
        SearchQuery query = new SearchQuery(new Pageable(1, 10,  "asc", "search"), "name");
        Page<Tag> expectedPage = new Page<>(1, 10, 0,  Collections.emptyList());
        when(tagPersistence.findAll(query)).thenReturn(expectedPage);

        // Act
        Page<Tag> actualPage = tagService.getAllTags(query);

        // Assert
        assertNotNull(actualPage);
        assertSame(expectedPage, actualPage);
        verify(tagPersistence, times(1)).findAll(query);
    }
}
