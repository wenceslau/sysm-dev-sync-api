package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionStatusUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.QuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/questions")
@Tag(name = "Questions")
public interface QuestionAPI {

    @PostMapping
    @Operation(summary = "Create a new question")
    ResponseEntity<?> createQuestion(@RequestBody QuestionCreateUpdate request);

    @GetMapping("/{id}")
    @Operation(summary = "Get a question by its ID")
    ResponseEntity<QuestionResponse> getQuestionById(@PathVariable("id") String id);

    @GetMapping
    @Operation(summary = "Search for questions with pagination and filters")
    Pagination<QuestionResponse> searchQuestions(
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "terms", required = false) String terms
    );

    @PutMapping("/{id}")
    @Operation(summary = "Update a question's title and description")
    ResponseEntity<?> updateQuestion(@PathVariable("id") String id, @RequestBody QuestionCreateUpdate request);

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update a question's status")
    ResponseEntity<?> updateQuestionStatus(@PathVariable("id") String id, @RequestBody QuestionStatusUpdate request);

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a question")
    @ApiResponse(responseCode = "204", description = "Question deleted successfully")
    ResponseEntity<?> deleteQuestion(@PathVariable("id") String id);

    @PostMapping("/{id}/tags/{tagId}")
    @Operation(summary = "Add a tag to a question")
    @ApiResponse(responseCode = "204", description = "Tag added successfully")
    ResponseEntity<?> addTag(@PathVariable("id") String id, @PathVariable("tagId") String tagId);

    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "Remove a tag from a question")
    @ApiResponse(responseCode = "204", description = "Tag removed successfully")
    ResponseEntity<?> removeTag(@PathVariable("id") String id, @PathVariable("tagId") String tagId);
}
