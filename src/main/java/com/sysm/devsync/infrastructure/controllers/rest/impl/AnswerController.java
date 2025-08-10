package com.sysm.devsync.infrastructure.controllers.rest.impl;

import com.sysm.devsync.application.AnswerService;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.infrastructure.controllers.rest.AnswerAPI;
import com.sysm.devsync.infrastructure.controllers.dto.request.AnswerCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.AnswerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
public class AnswerController extends AbstractController implements AnswerAPI {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }

    @Override
    public ResponseEntity<?> createAnswer(String questionId, @Valid @RequestBody AnswerCreateUpdate request) {
        // Note: We need to adapt the call to the service method
        var response = answerService.createAnswer(request, questionId, authenticatedUserId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/answers/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    public ResponseEntity<AnswerResponse> getAnswerById(String answerId) {
        var answer = answerService.getAnswerById(answerId);
        return ResponseEntity.ok(AnswerResponse.from(answer));
    }

    @Override
    public Pagination<AnswerResponse> getAnswersByQuestionId(String questionId, int pageNumber, int pageSize,
                                                             String sort, String direction) {

        var page = Page.of(pageNumber, pageSize, sort, direction);

        return answerService.getAllAnswers(page, questionId).map(AnswerResponse::from);
    }

    @Override
    public Pagination<AnswerResponse> searchAnswers(int pageNumber, int pageSize, String sort, String direction,
                                                    String queryType, Map<String, String> filters) {
        var page = Page.of(pageNumber, pageSize, sort, direction);
        var searchQuery = SearchQuery.of(page, filters);

        return answerService.getAllAnswers(searchQuery).map(AnswerResponse::from);
    }

    @Override
    public ResponseEntity<?> updateAnswer(String answerId, @Valid @RequestBody AnswerCreateUpdate request) {
        // Note: We need to adapt the call to the service method
        answerService.updateAnswer(answerId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> acceptAnswer(String answerId) {
        answerService.acceptAnswer(answerId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> rejectAnswer(String answerId) {
        answerService.rejectAnswer(answerId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> deleteAnswer(String answerId) {
        answerService.deleteAnswer(answerId);
        return ResponseEntity.noContent().build();
    }
}
