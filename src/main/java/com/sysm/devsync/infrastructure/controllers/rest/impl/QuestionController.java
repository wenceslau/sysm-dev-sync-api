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
import java.util.Map;

@RestController
public class QuestionController extends AbstractController implements QuestionAPI {

    private final QuestionService questionService;
    // In a real app, this would come from the Spring Security context

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Override
    public ResponseEntity<?> createQuestion(@Valid @RequestBody QuestionCreateUpdate request) {
        var response = questionService.createQuestion(request, authenticatedUserId());
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
    public Pagination<QuestionResponse> searchQuestions(int pageNumber, int pageSize, String sort, String direction,
                                                       String queryType, Map<String, String> filters) {

        var page = Page.of(pageNumber, pageSize, sort, direction);
        var searchQuery = SearchQuery.of(page, filters);

        return questionService.getAllQuestions(searchQuery).map(QuestionResponse::from);
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
