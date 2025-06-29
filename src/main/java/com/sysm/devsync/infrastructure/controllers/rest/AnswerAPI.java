package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.controllers.dto.request.AnswerCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.AnswerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("answers")
@Tag(name = "Answers")
public interface AnswerAPI {

    @PostMapping("/questions/{questionId}")
    @Operation(summary = "Create a new answer for a question")
    @ApiResponse(responseCode = "201", description = "Answer created successfully")
    ResponseEntity<?> createAnswer(
            @PathVariable("questionId") String questionId,
            @RequestBody AnswerCreateUpdate request
    );

    @GetMapping("/{answerId}")
    @Operation(summary = "Get an answer by its ID")
    ResponseEntity<AnswerResponse> getAnswerById(@PathVariable("answerId") String answerId);


    @GetMapping
    @Operation(summary = "Search for answers with various filters")
    Pagination<AnswerResponse> searchAnswers(
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam Map<String, String> filters
    );

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Get all answers for a specific question")
    Pagination<AnswerResponse> getAnswersByQuestionId(
            @PathVariable("questionId") String questionId,
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "asc") String direction
    );

    @PutMapping("/{answerId}")
    @Operation(summary = "Update an answer's content")
    ResponseEntity<?> updateAnswer(
            @PathVariable("answerId") String answerId,
            @RequestBody AnswerCreateUpdate request
    );

    @PatchMapping("/{answerId}/accept")
    @Operation(summary = "Mark an answer as accepted")
    @ApiResponse(responseCode = "204", description = "Answer marked as accepted")
    ResponseEntity<?> acceptAnswer(@PathVariable("answerId") String answerId);

    @PatchMapping("/{answerId}/reject")
    @Operation(summary = "Un-mark an answer as accepted")
    @ApiResponse(responseCode = "204", description = "Answer acceptance removed")
    ResponseEntity<?> rejectAnswer(@PathVariable("answerId") String answerId);

    @DeleteMapping("/{answerId}")
    @Operation(summary = "Delete an answer")
    @ApiResponse(responseCode = "204", description = "Answer deleted successfully")
    ResponseEntity<?> deleteAnswer(@PathVariable("answerId") String answerId);
}
