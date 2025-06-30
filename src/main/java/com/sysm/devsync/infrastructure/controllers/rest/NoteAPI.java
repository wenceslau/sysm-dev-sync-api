package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.controllers.dto.request.NoteCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.NoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/notes")
@Tag(name = "Notes")
public interface NoteAPI {

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Create a new note")
    @ApiResponse(responseCode = "201", description = "Note created successfully")
    ResponseEntity<?> createNote(@RequestBody NoteCreateUpdate request);

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Get a note by its ID")
    @ApiResponse(responseCode = "200", description = "Note found")
    ResponseEntity<NoteResponse> getNoteById(@PathVariable("id") String id);

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Search for notes with pagination and filters")
    @ApiResponse(responseCode = "200", description = "Notes found")
    Pagination<NoteResponse> searchNotes(
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam Map<String, String> filters
    );

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Update a note's title and content")
    @ApiResponse(responseCode = "204", description = "Note updated successfully")
    ResponseEntity<?> updateNote(@PathVariable("id") String id, @RequestBody NoteCreateUpdate request);

    @PatchMapping("/{id}/content")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Partially update a note's content")
    @ApiResponse(responseCode = "204", description = "Note content updated successfully")
    ResponseEntity<?> updateNoteContent(@PathVariable("id") String id, @RequestBody NoteCreateUpdate request);

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete a note")
    @ApiResponse(responseCode = "204", description = "Note deleted successfully")
    @ApiResponse(responseCode = "204", description = "Note deleted successfully")
    ResponseEntity<?> deleteNote(@PathVariable("id") String id);

    @PostMapping("/{id}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Add a tag to a note")
    @ApiResponse(responseCode = "204", description = "Tag added successfully")
    ResponseEntity<?> addTag(@PathVariable("id") String id, @PathVariable("tagId") String tagId);

    @DeleteMapping("/{id}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Remove a tag from a note")
    @ApiResponse(responseCode = "204", description = "Tag removed successfully")
    ResponseEntity<?> removeTag(@PathVariable("id") String id, @PathVariable("tagId") String tagId);
}
