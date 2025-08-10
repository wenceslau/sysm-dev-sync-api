package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.config.security.IsQuestionOwnerOrAdmin;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionStatusUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.QuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/questions")
@Tag(name = "Questions")
public interface QuestionAPI {

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Create a new question")
    @ApiResponse(responseCode = "201", description = "Question created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Project not found")
    ResponseEntity<?> createQuestion(@RequestBody QuestionCreateUpdate request);

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Get a question by its ID")
    @ApiResponse(responseCode = "200", description = "Question found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Question not found")
    ResponseEntity<QuestionResponse> getQuestionById(@PathVariable("id") String id);

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Search for questions with pagination and filters")
    @ApiResponse(responseCode = "200", description = "Questions found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    Pagination<QuestionResponse> searchQuestions(
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "queryType", required = false, defaultValue = "or") String queryType,
            @RequestParam Map<String, String> filters
    );

    @IsQuestionOwnerOrAdmin
    @PutMapping("/{questionId}")
    @Operation(summary = "Update a question's title and description")
    @ApiResponse(responseCode = "204", description = "Question updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Question not found")
    ResponseEntity<?> updateQuestion(@PathVariable("questionId") String questionId, @RequestBody QuestionCreateUpdate request);

    @IsQuestionOwnerOrAdmin
    @PatchMapping("/{questionId}/status")
    @Operation(summary = "Update a question's status")
    @ApiResponse(responseCode = "204", description = "Question status updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Question not found")
    ResponseEntity<?> updateQuestionStatus(@PathVariable("questionId") String questionId, @RequestBody QuestionStatusUpdate request);

    @IsQuestionOwnerOrAdmin
    @DeleteMapping("/{questionId}")
    @Operation(summary = "Delete a question")
    @ApiResponse(responseCode = "204", description = "Question deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Question not found")
    ResponseEntity<?> deleteQuestion(@PathVariable("questionId") String questionId);

    @IsQuestionOwnerOrAdmin
    @PostMapping("/{questionId}/tags/{tagId}")
    @Operation(summary = "Add a tag to a question")
    @ApiResponse(responseCode = "204", description = "Tag added successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Question or tag not found")
    ResponseEntity<?> addTag(@PathVariable("questionId") String questionId, @PathVariable("tagId") String tagId);

    @IsQuestionOwnerOrAdmin
    @DeleteMapping("/{questionId}/tags/{tagId}")
    @Operation(summary = "Remove a tag from a question")
    @ApiResponse(responseCode = "204", description = "Tag removed successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Question or tag not found")
    ResponseEntity<?> removeTag(@PathVariable("questionId") String questionId, @PathVariable("tagId") String tagId);
}
