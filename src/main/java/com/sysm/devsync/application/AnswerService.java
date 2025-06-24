package com.sysm.devsync.application;

import com.sysm.devsync.infrastructure.controllers.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controllers.dto.request.AnswerCreateUpdate;
import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Answer;
import com.sysm.devsync.domain.persistence.AnswerPersistencePort;
import com.sysm.devsync.domain.persistence.QuestionPersistencePort;
import com.sysm.devsync.domain.persistence.UserPersistencePort;

public class AnswerService {

    private final AnswerPersistencePort answerPersistence;
    private final QuestionPersistencePort questionPersistence;
    private final UserPersistencePort userPersistencePort;

    public AnswerService(AnswerPersistencePort answerPersistence,
                         QuestionPersistencePort questionPersistence,
                         UserPersistencePort userPersistencePort) {
        this.answerPersistence = answerPersistence;
        this.questionPersistence = questionPersistence;
        this.userPersistencePort = userPersistencePort;
    }

    public CreateResponse createAnswer(AnswerCreateUpdate answerCreateUpdate, String questionId, String authorId) {
        var questionExist = questionPersistence.existsById(questionId);
        if (!questionExist) {
            throw new NotFoundException("Question not found", questionId);
        }

        var authorExist = userPersistencePort.existsById(authorId);
        if (!authorExist) {
            throw new NotFoundException("Author not found", authorId);
        }

        var answer = Answer.create(
                answerCreateUpdate.content(),
                questionId,
                authorId
        );

        answerPersistence.create(answer);

        return new CreateResponse(answer.getId());
    }

    public void updateAnswer(String answerId, AnswerCreateUpdate answerUpdate) {
        var answer = answerPersistence.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found", answerId));

        answer.update(answerUpdate.content());
        answerPersistence.update(answer);
    }

    public void acceptAnswer(String answerId) {
        var answer = answerPersistence.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found", answerId));

        answer.accept();
        answerPersistence.update(answer);
    }

    public void rejectAnswer(String answerId) {
        var answer = answerPersistence.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found", answerId));

        answer.reject();
        answerPersistence.update(answer);
    }

    public void deleteAnswer(String answerId) {
        var exist = answerPersistence.existsById(answerId);
        if (!exist) {
            throw new NotFoundException("Answer not found", answerId);
        }

        answerPersistence.deleteById(answerId);
    }

    public Answer getAnswerById(String answerId) {
        return answerPersistence.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found", answerId));
    }

    public Pagination<Answer> getAllAnswers(Page page, String questionId) {
        var questionExist = questionPersistence.existsById(questionId);
        if (!questionExist) {
            throw new NotFoundException("Question not found", questionId);
        }

        return answerPersistence.findAllByQuestionId(page, questionId);
    }

    public Pagination<Answer> getAllAnswers(SearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Invalid query parameters");
        }
        return answerPersistence.findAll(query);
    }

}
