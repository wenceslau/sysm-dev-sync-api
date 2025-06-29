package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.controllers.dto.request.NoteCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.NoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/notes")
@Tag(name = "Notes")
public interface NoteAPI {

    @PostMapping
    @Operation(summary = "Create a new note")
    ResponseEntity<?> createNote(@RequestBody NoteCreateUpdate request);

    @GetMapping("/{id}")
    @Operation(summary = "Get a note by its ID")
    ResponseEntity<NoteResponse> getNoteById(@PathVariable("id") String id);

    @GetMapping
    @Operation(summary = "Search for notes with pagination and filters")
    Pagination<NoteResponse> searchNotes(
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam Map<String, String> filters
    );

    @PutMapping("/{id}")
    @Operation(summary = "Update a note's title and content")
    ResponseEntity<?> updateNote(@PathVariable("id") String id, @RequestBody NoteCreateUpdate request);

    @PatchMapping("/{id}/content")
    @Operation(summary = "Partially update a note's content")
    ResponseEntity<?> updateNoteContent(@PathVariable("id") String id, @RequestBody NoteCreateUpdate request);

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a note")
    @ApiResponse(responseCode = "204", description = "Note deleted successfully")
    ResponseEntity<?> deleteNote(@PathVariable("id") String id);

    @PostMapping("/{id}/tags/{tagId}")
    @Operation(summary = "Add a tag to a note")
    @ApiResponse(responseCode = "204", description = "Tag added successfully")
    ResponseEntity<?> addTag(@PathVariable("id") String id, @PathVariable("tagId") String tagId);

    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "Remove a tag from a note")
    @ApiResponse(responseCode = "204", description = "Tag removed successfully")
    ResponseEntity<?> removeTag(@PathVariable("id") String id, @PathVariable("tagId") String tagId);
}
