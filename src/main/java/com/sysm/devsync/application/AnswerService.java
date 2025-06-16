package com.sysm.devsync.application;

import com.sysm.devsync.controller.dto.CreateResponse;
import com.sysm.devsync.controller.dto.request.AnswerCreateUpdate;
import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pageable;
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
            throw new NotFoundException("Question not found");
        }

        var authorExist = userPersistencePort.existsById(authorId);
        if (!authorExist) {
            throw new NotFoundException("Author not found");
        }

        var answer = Answer.create(
                questionId,
                authorId,
                answerCreateUpdate.content()
        );

        answerPersistence.create(answer);

        return new CreateResponse(answer.getId());
    }

    public void updateAnswer(String answerId, AnswerCreateUpdate answerUpdate) {
        var answer = answerPersistence.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found"));

        answer.update(answerUpdate.content());
        answerPersistence.update(answer);
    }

    public void acceptAnswer(String answerId) {
        var answer = answerPersistence.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found"));

        answer.accept();
        answerPersistence.update(answer);
    }

    public void rejectAnswer(String answerId) {
        var answer = answerPersistence.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found"));

        answer.reject();
        answerPersistence.update(answer);
    }

    public void deleteAnswer(String answerId) {
        var exist = answerPersistence.existsById(answerId);
        if (!exist) {
            throw new NotFoundException("Answer not found");
        }

        answerPersistence.deleteById(answerId);
    }

    public Answer getAnswerById(String answerId) {
        return answerPersistence.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found"));
    }

    public Page<Answer> getAllAnswers(Pageable pageable, String questionId) {
        var questionExist = questionPersistence.existsById(questionId);
        if (!questionExist) {
            throw new NotFoundException("Question not found");
        }

        return answerPersistence.findAllByQuestionId(pageable, questionId);
    }

    public Page<Answer> getAllAnswers(SearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Invalid query parameters");
        }
        return answerPersistence.findAll(query);
    }

}
