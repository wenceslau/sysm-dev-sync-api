package com.sysm.devsync.integration;

import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.infrastructure.controllers.dto.request.TagCreateUpdate;
import com.sysm.devsync.infrastructure.repositories.TagJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.TagJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TagIntegrationTest extends AbstractIntegrationTest {

    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    @Autowired
    private TagJpaRepository tagJpaRepository;

    @BeforeEach
    void setUp() {
        // This ensures each test runs with a clean database, preventing side effects.
        tagJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
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
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("POST /tags - should fail with 400 Bad Request for invalid data")
    void createTag_withInvalidData_shouldFail() throws Exception {
        // Arrange: Name is blank, which violates the @NotBlank constraint
        var requestDto = new TagCreateUpdate("", "#F89820", "Description", "Category");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Location", nullValue()))
                .andExpect(jsonPath("$.validationErrors.name[0]", equalTo("Tag name must not be blank")));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /tags/{id} - should retrieve an existing tag")
    void getTagById_shouldSucceed() throws Exception {
        // Arrange
        Tag existingTagDomain = Tag.create("Python", "#3572A5");
        TagJpaEntity existingTagJpa = TagJpaEntity.fromModel(existingTagDomain);
        tagJpaRepository.saveAndFlush(existingTagJpa);

        // Act & Assert
        mockMvc.perform(get("/tags/{id}", existingTagJpa.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(existingTagJpa.getId()))
                .andExpect(jsonPath("$.name").value("Python"))
                .andExpect(jsonPath("$.color").value("#3572A5"));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /tags/{id} - should return 404 Not Found for non-existent tag")
    void getTagById_whenNotFound_shouldFail() throws Exception {
        // Arrange
        var nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(get("/tags/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /tags - should return paginated list of tags")
    void searchTags_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        tagJpaRepository.save(TagJpaEntity.fromModel(Tag.create("Go", "#F89820")));
        tagJpaRepository.save(TagJpaEntity.fromModel(Tag.create("Rust", "#FFA500")));
        tagJpaRepository.save(TagJpaEntity.fromModel(Tag.create("TypeScript", "#007ACC")));
        tagJpaRepository.flush();

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
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /tags - should return tags filtered by query parameters")
    void searchTags_withFilters_shouldReturnFilteredResults() throws Exception {
        // Arrange
        // We need to add the 'category' to the Tag model and persistence for this test to pass
        // Assuming Tag.create can take name, color, description, category
        tagJpaRepository.save(TagJpaEntity.fromModel(Tag.create("Java", "#F89820", "Programming")));
        tagJpaRepository.save(TagJpaEntity.fromModel(Tag.create("JavaScript", "#F7DF1E", "Programming")));
        tagJpaRepository.save(TagJpaEntity.fromModel(Tag.create("Docker", "#2496ED", "DevOps")));
        tagJpaRepository.flush();

        // Act & Assert - Filter by a single field (name)
        mockMvc.perform(get("/tags")
                        .param("name", "Docker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Docker"));

        // Act & Assert - Filter by another single field (category)
        // This now includes pagination parameters to prove they are ignored by the filter logic
        mockMvc.perform(get("/tags")
                        .param("category", "Programming")
                        .param("pageNumber", "0")
                        .param("pageSize", "1")
                        .param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items", hasSize(1))) // We expect only 1 due to pageSize
                .andExpect(jsonPath("$.items[0].name").value("Java")); // 'Java' comes before 'JavaScript'

        // Act & Assert - Filter by multiple fields (name and category)
        mockMvc.perform(get("/tags")
                        .param("name", "Docker")
                        .param("category", "DevOps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Docker"));

        // Act & Assert - Filter with no results
        mockMvc.perform(get("/tags")
                        .param("name", "JavaScripts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("PUT /tags/{id} - should update an existing tag")
    void updateTag_shouldSucceed() throws Exception {
        // Arrange
        Tag existingTagDomain = Tag.create("Old Name", "#OldColor");
        TagJpaEntity existingTagJpa = TagJpaEntity.fromModel(existingTagDomain);
        tagJpaRepository.saveAndFlush(existingTagJpa);

        // Prepare the update request
        var updateDto = new TagCreateUpdate("New Name", "#NewColor", "New Description", "New Category");
        var requestJson = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/tags/{id}", existingTagJpa.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify the state in the database
        Optional<TagJpaEntity> updatedTagEntity = tagJpaRepository.findById(existingTagJpa.getId());
        assertThat(updatedTagEntity).isPresent();
        assertThat(updatedTagEntity.get().getName()).isEqualTo("New Name");
        assertThat(updatedTagEntity.get().getColor()).isEqualTo("#NewColor");
        assertThat(updatedTagEntity.get().getDescription()).isEqualTo("New Description");
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("DELETE /tags/{id} - should delete an existing tag")
    void deleteTag_shouldSucceed() throws Exception {
        // Arrange
        Tag tagToDeleteDomain = Tag.create("Ephemeral", "#FFFFFF");
        TagJpaEntity tagToDeleteJpa = TagJpaEntity.fromModel(tagToDeleteDomain);
        tagJpaRepository.saveAndFlush(tagToDeleteJpa);

        // Verify it exists before deletion
        assertThat(tagJpaRepository.existsById(tagToDeleteJpa.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/tags/{id}", tagToDeleteJpa.getId()))
                .andExpect(status().isNoContent());

        // Verify it no longer exists in the database
        assertThat(tagJpaRepository.existsById(tagToDeleteJpa.getId())).isFalse();
    }
}
