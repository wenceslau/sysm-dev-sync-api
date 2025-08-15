package com.sysm.devsync.infrastructure.controllers.rest.impl;

import com.sysm.devsync.application.UserService;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QueryType;
import com.sysm.devsync.domain.models.to.UserTO;
import com.sysm.devsync.infrastructure.controllers.dto.request.UserCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.UserResponse;
import com.sysm.devsync.infrastructure.controllers.rest.UserAPI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserController extends AbstractController implements UserAPI {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<?> create(@Valid UserCreateUpdate request) {
        var response = userService.createUser(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest() // Starts with http://.../users
                .path("/{id}")       // Appends /{id}
                .buildAndExpand(response.id()) // Replaces {id} with the actual ID
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @Override
    public ResponseEntity<?> update(String id, @Valid UserCreateUpdate request) {
        userService.updateUser(id, request);
        return ResponseEntity
                .noContent()
                .build();
    }

    @Override
    public ResponseEntity<?> updatePatch(String id, UserCreateUpdate request) {
        userService.updateUserPatch(id, request);
        return ResponseEntity
                .noContent()
                .build();
    }

    @Override
    public ResponseEntity<?> delete(String id) {
        userService.deleteUser(id);
        return ResponseEntity
                .noContent()
                .build();
    }

    @Override
    public List<UserTO> list() {

        var page = Page.of(0, 1000, "createdAt", "desc");
        var searchQuery = SearchQuery.of(page, QueryType.OR, Map.of());
        var pagination = userService.searchUsers(searchQuery);

        return pagination.map(x-> UserTO.of(x.getId(), x.getName())).items();
    }

    @Override
    public ResponseEntity<UserResponse> getById(String id) {
        var user = userService.getUserById(id);
        return ResponseEntity
                .ok(UserResponse.from(user));
    }

    @Override
    public Pagination<UserResponse> search(int pageNumber, int pageSize, String sort, String direction,
                                           String queryType, Map<String, String> filters) {

        var page = Page.of(pageNumber, pageSize, sort, direction);
        var searchQuery = SearchQuery.of(page, QueryType.of(queryType), filters);

        var pagination = userService.searchUsers(searchQuery);
        return pagination.map(UserResponse::from);
    }
}
