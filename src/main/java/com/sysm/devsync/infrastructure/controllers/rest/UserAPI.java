package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.config.security.IsMemberOrAdmin;
import com.sysm.devsync.infrastructure.controllers.dto.request.UserCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping(value = "users")
@Tag(name = "Users")
public interface UserAPI {

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Create a new user")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    ResponseEntity<?> create(
            @RequestBody UserCreateUpdate request
    );

    @PutMapping(value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Update a user's details (full update)")
    @ApiResponse(responseCode = "204", description = "User updated successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    ResponseEntity<?> update(
            @PathVariable("id") String id,
            @RequestBody UserCreateUpdate request
    );

    @PatchMapping(value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Partially update a user's details")
    @ApiResponse(responseCode = "204", description = "User updated successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    ResponseEntity<?> updatePatch(
            @PathVariable("id") String id,
            @RequestBody UserCreateUpdate request
    );

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete a user by their ID")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    ResponseEntity<?> delete(
            @PathVariable("id") String id
    );

    @IsMemberOrAdmin
    @GetMapping(value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get a user by their ID")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    ResponseEntity<UserResponse> getById(
            @PathVariable("id") String id
    );

    @IsMemberOrAdmin
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search for users with pagination")
    Pagination<UserResponse> search(
            @RequestParam(name = "pageNumber", required = false, defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(name = "direction", required = false, defaultValue = "asc") String direction,
            @RequestParam(name = "queryType", required = false, defaultValue = "or") String queryType,
            @RequestParam Map<String, String> filters
    );

}
