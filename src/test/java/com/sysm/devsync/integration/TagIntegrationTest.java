package com.sysm.devsync.integration;

import com.sysm.devsync.domain.models.Tag; // Import Tag domain model
import com.sysm.devsync.infrastructure.controller.dto.request.TagCreateUpdate;
import com.sysm.devsync.infrastructure.repositories.TagJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.TagJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TagIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TagJpaRepository tagJpaRepository; // Inject repository for DB assertions

    @Test
    @DisplayName("POST /tags - should create a new tag successfully")
    void createTag_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new TagCreateUpdate("Java", "#F89820", "Java programming language", "Programming");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        var responseContent = mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Extract the ID from the response
        var createdTagId = objectMapper.readTree(responseContent).get("id").asText();

        // Verify the state in the database
        Optional<TagJpaEntity> createdTagEntity = tagJpaRepository.findById(createdTagId);
        assertThat(createdTagEntity).isPresent();
        assertThat(createdTagEntity.get().getName()).isEqualTo("Java");
        assertThat(createdTagEntity.get().getColor()).isEqualTo("#F89820");
        assertThat(createdTagEntity.get().getDescription()).isEqualTo("Java programming language");
    }

    @Test
    @DisplayName("POST /tags - should fail with 400 Bad Request for invalid data")
    void createTag_withInvalidData_shouldFail() throws Exception {
        // Arrange: Name is blank, which violates the @NotBlank constraint
        var requestDto = new TagCreateUpdate("", "#F89820", "Description", "Category");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /tags/{id} - should retrieve an existing tag")
    void getTagById_shouldSucceed() throws Exception {
        // Arrange: First, create a tag directly in the DB for the test
        // OLD: var existingTag = new TagJpaEntity(UUID.randomUUID().toString(), "Python");
        // OLD: existingTag.setColor("#3572A5");
        // NEW: Create domain model and convert
        Tag existingTagDomain = Tag.create("Python", "#3572A5");
        TagJpaEntity existingTagJpa = TagJpaEntity.fromModel(existingTagDomain);
        tagJpaRepository.saveAndFlush(existingTagJpa);

        // Act & Assert
        mockMvc.perform(get("/tags/{id}", existingTagJpa.getId())) // Use the ID from the JPA entity
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(existingTagJpa.getId()))
                .andExpect(jsonPath("$.name").value("Python"))
                .andExpect(jsonPath("$.color").value("#3572A5"));
    }

    @Test
    @DisplayName("GET /tags/{id} - should return 404 Not Found for non-existent tag")
    void getTagById_whenNotFound_shouldFail() throws Exception {
        // Arrange
        var nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        // This expects a 404. Your service throws IllegalArgumentException.
        // This test will pass once you have a @ControllerAdvice to map that exception to a 404 status.
        mockMvc.perform(get("/tags/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tags - should return paginated list of tags")
    void searchTags_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        // The "Go" tag was already correct, keeping it as an example.
        Tag tagGo = Tag.create("Go", "#F89820");
        tagJpaRepository.save(TagJpaEntity.fromModel(tagGo));

        // NEW: Create domain models and convert for "Rust" and "TypeScript"
        Tag tagRust = Tag.create("Rust", "#FFA500");
        tagJpaRepository.save(TagJpaEntity.fromModel(tagRust));

        Tag tagTypeScript = Tag.create("TypeScript", "#007ACC");
        tagJpaRepository.save(TagJpaEntity.fromModel(tagTypeScript));

        tagJpaRepository.flush(); // Ensure all are persisted before query

        // Act & Assert
        mockMvc.perform(get("/tags")
                        .param("pageNumber", "0")
                        .param("pageSize", "2")
                        .param("sort", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Go"))
                .andExpect(jsonPath("$.items[1].name").value("Rust"));
    }

    @Test
    @DisplayName("PUT /tags/{id} - should update an existing tag")
    void updateTag_shouldSucceed() throws Exception {
        // Arrange: Create the initial tag using domain model and convert
        // OLD: var existingTag = new TagJpaEntity(UUID.randomUUID().toString(), "Old Name");
        // OLD: existingTag.setColor("#OldColor");
        Tag existingTagDomain = Tag.create("Old Name", "#OldColor");
        TagJpaEntity existingTagJpa = TagJpaEntity.fromModel(existingTagDomain);
        tagJpaRepository.saveAndFlush(existingTagJpa);

        // Prepare the update request
        var updateDto = new TagCreateUpdate("New Name", "#NewColor", "New Description", "New Category");
        var requestJson = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/tags/{id}", existingTagJpa.getId()) // Use the ID from the JPA entity
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify the state in the database
        Optional<TagJpaEntity> updatedTagEntity = tagJpaRepository.findById(existingTagJpa.getId()); // Use the ID from the JPA entity
        assertThat(updatedTagEntity).isPresent();
        assertThat(updatedTagEntity.get().getName()).isEqualTo("New Name");
        assertThat(updatedTagEntity.get().getColor()).isEqualTo("#NewColor");
        assertThat(updatedTagEntity.get().getDescription()).isEqualTo("New Description");
    }

    @Test
    @DisplayName("DELETE /tags/{id} - should delete an existing tag")
    void deleteTag_shouldSucceed() throws Exception {
        // Arrange: Create the tag to be deleted using domain model and convert
        // OLD: var tagToDelete = new TagJpaEntity(UUID.randomUUID().toString(), "Ephemeral");
        // OLD: tagToDelete.setColor("#FFFFFF");
        Tag tagToDeleteDomain = Tag.create("Ephemeral", "#FFFFFF");
        TagJpaEntity tagToDeleteJpa = TagJpaEntity.fromModel(tagToDeleteDomain);
        tagJpaRepository.saveAndFlush(tagToDeleteJpa);

        // Verify it exists before deletion
        assertThat(tagJpaRepository.existsById(tagToDeleteJpa.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/tags/{id}", tagToDeleteJpa.getId())) // Use the ID from the JPA entity
                .andExpect(status().isNoContent());

        // Verify it no longer exists in the database
        assertThat(tagJpaRepository.existsById(tagToDeleteJpa.getId())).isFalse();
    }
}
