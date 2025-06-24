package com.sysm.devsync.infrastructure.controllers.rest.impl;

import com.sysm.devsync.application.QuestionService;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.infrastructure.controllers.rest.QuestionAPI;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionStatusUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.QuestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class QuestionController implements QuestionAPI {

    private final QuestionService questionService;
    // In a real app, this would come from the Spring Security context
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Override
    public ResponseEntity<?> createQuestion(@Valid @RequestBody QuestionCreateUpdate request) {
        var response = questionService.createQuestion(request, FAKE_AUTHENTICATED_USER_ID);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    public ResponseEntity<QuestionResponse> getQuestionById(String id) {
        var question = questionService.getQuestionById(id);
        return ResponseEntity.ok(QuestionResponse.from(question));
    }

    @Override
    public Pagination<QuestionResponse> searchQuestions(int pageNumber, int pageSize, String sort, String direction, String terms) {
        var page = Page.of(pageNumber, pageSize, sort, direction);
        var query = new SearchQuery(page, terms);
        return questionService.getAllQuestions(query).map(QuestionResponse::from);
    }

    @Override
    public ResponseEntity<?> updateQuestion(String id, @Valid @RequestBody QuestionCreateUpdate request) {
        questionService.updateQuestion(id, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> updateQuestionStatus(String id, @Valid @RequestBody QuestionStatusUpdate request) {
        questionService.updateQuestionStatus(id, request.status());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> deleteQuestion(String id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> addTag(String id, String tagId) {
        questionService.addTagToQuestion(id, tagId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> removeTag(String id, String tagId) {
        questionService.removeTagFromQuestion(id, tagId);
        return ResponseEntity.noContent().build();
    }
}
