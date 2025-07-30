package com.sysm.devsync.domain.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagTest {

    private String validName;
    private String validColor;

    @BeforeEach
    void setUp() {
        validName = "Java";
        validColor = "#007396"; // A common Java blue
    }

    // --- Static Factory Method: create() ---
    @Test
    @DisplayName("create() should successfully create a tag with valid arguments")
    void create_shouldSucceed_withValidArguments() {
        Tag tag = Tag.create(validName, validColor);

        assertNotNull(tag.getId(), "ID should be generated and not null");
        try {
            UUID.fromString(tag.getId()); // Validate UUID format
        } catch (IllegalArgumentException e) {
            fail("Generated ID is not a valid UUID: " + tag.getId());
        }

        assertEquals(validName, tag.getName());
        assertEquals(validColor, tag.getColor());
        assertNull(tag.getDescription(), "Description should be null on creation");
        assertNull(tag.getCategory(), "Category should be null on creation");
        assertEquals(0, tag.getAmountUsed(), "CountUsage should be 0 on creation");
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null name")
    void create_shouldThrowException_whenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Tag.create(null, validColor);
        });
        assertEquals("Name cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty name")
    void create_shouldThrowException_whenNameIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Tag.create("", validColor);
        });
        assertEquals("Name cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for null color")
    void create_shouldThrowException_whenColorIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Tag.create(validName, null);
        });
        assertEquals("Color cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("create() should throw IllegalArgumentException for empty color")
    void create_shouldThrowException_whenColorIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Tag.create(validName, "");
        });
        assertEquals("Color cannot be null or empty", exception.getMessage());
    }

    // --- Static Factory Method: build() ---
    @Test
    @DisplayName("build() should successfully create a tag with all arguments")
    void build_shouldSucceed_withAllArguments() {
        String id = UUID.randomUUID().toString();
        String description = "A tag for Java programming language topics.";
        String category = "Programming Languages";
        int countUsage = 150;

        Tag tag = Tag.build(id, validName, validColor, description, category, countUsage);

        assertEquals(id, tag.getId());
        assertEquals(validName, tag.getName());
        assertEquals(validColor, tag.getColor());
        assertEquals(description, tag.getDescription());
        assertEquals(category, tag.getCategory());
        assertEquals(countUsage, tag.getAmountUsed());
    }

    @Test
    @DisplayName("build() should allow null description and category, and zero countUsage")
    void build_shouldAllowNullOptionalFieldsAndZeroCount() {
        String id = UUID.randomUUID().toString();
        Tag tag = Tag.build(id, validName, validColor, null, null, 0);

        assertNull(tag.getDescription());
        assertNull(tag.getCategory());
        assertEquals(0, tag.getAmountUsed());
    }


    @Test
    @DisplayName("build() should throw IllegalArgumentException for null id (via constructor)")
    void build_shouldThrowException_whenIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Tag.build(null, validName, validColor, "Desc", "Cat", 5);
        });
        assertEquals("ID cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for null name (via constructor)")
    void build_shouldThrowException_whenNameIsNullViaConstructor() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Tag.build(UUID.randomUUID().toString(), null, validColor, "Desc", "Cat", 5);
        });
        assertEquals("Name cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("build() should throw IllegalArgumentException for null color (via constructor)")
    void build_shouldThrowException_whenColorIsNullViaConstructor() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Tag.build(UUID.randomUUID().toString(), validName, null, "Desc", "Cat", 5);
        });
        assertEquals("Color cannot be null or empty", exception.getMessage());
    }

    // --- Instance Method: update() ---
    @Test
    @DisplayName("update() should modify name and color")
    void update_shouldModifyNameAndColor() {
        Tag tag = Tag.create(validName, validColor);
        String newName = "Python";
        String newColor = "#3572A5"; // Python blue

        Tag updatedTag = tag.update(newName, newColor);

        assertSame(tag, updatedTag, "Update method should return the same instance");
        assertEquals(newName, tag.getName());
        assertEquals(newColor, tag.getColor());
        // Ensure other fields are not changed
        assertNull(tag.getDescription());
        assertEquals(0, tag.getAmountUsed());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new name")
    void update_shouldThrowException_whenNewNameIsNull() {
        Tag tag = Tag.create(validName, validColor);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tag.update(null, "#FF0000");
        });
        assertEquals("Name cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("update() should throw IllegalArgumentException for null new color")
    void update_shouldThrowException_whenNewColorIsNull() {
        Tag tag = Tag.create(validName, validColor);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tag.update("NewName", null);
        });
        assertEquals("Color cannot be null or empty", exception.getMessage());
    }

    // --- Instance Method: updateDescription() ---
    @Test
    @DisplayName("updateDescription() should modify description")
    void updateDescription_shouldModifyDescription() {
        Tag tag = Tag.create(validName, validColor);
        String newDescription = "All about Java programming.";

        tag.updateDescription(newDescription);

        assertEquals(newDescription, tag.getDescription());
        // Ensure other fields are not changed
        assertEquals(validName, tag.getName());
    }

    @Test
    @DisplayName("updateDescription() should allow null description")
    void updateDescription_shouldAllowNullDescription() {
        Tag tag = Tag.create(validName, validColor);
        tag.updateDescription("Initial Description"); // Set a non-null description first
        assertNotNull(tag.getDescription());

        tag.updateDescription(null);
        assertNull(tag.getDescription());
    }

    @Test
    @DisplayName("updateDescription() should allow blank description")
    void updateDescription_shouldAllowBlankDescription() {
        Tag tag = Tag.create(validName, validColor);
        tag.updateDescription("  ");
        assertEquals("  ", tag.getDescription());
    }

    // --- Instance Method: updateCategory() ---
    @Test
    @DisplayName("updateCategory() should modify category")
    void updateCategory_shouldModifyCategory() {
        Tag tag = Tag.create(validName, validColor);
        String newCategory = "Backend";

        tag.updateCategory(newCategory);

        assertEquals(newCategory, tag.getCategory());
        // Ensure other fields are not changed
        assertEquals(validName, tag.getName());
    }

    @Test
    @DisplayName("updateCategory() should allow null category")
    void updateCategory_shouldAllowNullCategory() {
        Tag tag = Tag.create(validName, validColor);
        tag.updateCategory("Initial Category"); // Set a non-null category first
        assertNotNull(tag.getCategory());

        tag.updateCategory(null);
        assertNull(tag.getCategory());
    }

    @Test
    @DisplayName("updateCategory() should allow blank category")
    void updateCategory_shouldAllowBlankCategory() {
        Tag tag = Tag.create(validName, validColor);
        tag.updateCategory("  ");
        assertEquals("  ", tag.getCategory());
    }

    // --- Instance Method: incrementUsage() ---
    @Test
    @DisplayName("incrementUsage() should increment countUsage")
    void incrementUsage_shouldIncrementCountUse() {
        Tag tag = Tag.create(validName, validColor);
        assertEquals(0, tag.getAmountUsed());

        tag.incrementUse();
        assertEquals(1, tag.getAmountUsed());

        tag.incrementUse();
        assertEquals(2, tag.getAmountUsed());
    }

    // --- Getters (Basic check, mostly covered by other tests) ---
    @Test
    @DisplayName("Getters should return correct values after construction via build")
    void getters_shouldReturnCorrectValues() {
        String id = UUID.randomUUID().toString();
        String name = "JavaScript";
        String color = "#F7DF1E"; // JS Yellow
        String description = "For client-side and server-side scripting.";
        String category = "Web Development";
        int countUsage = 250;

        Tag tag = Tag.build(id, name, color, description, category, countUsage);

        assertEquals(id, tag.getId());
        assertEquals(name, tag.getName());
        assertEquals(color, tag.getColor());
        assertEquals(description, tag.getDescription());
        assertEquals(category, tag.getCategory());
        assertEquals(countUsage, tag.getAmountUsed());
    }
}
