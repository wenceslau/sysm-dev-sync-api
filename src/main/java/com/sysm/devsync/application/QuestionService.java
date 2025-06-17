package com.sysm.devsync.application;

import com.sysm.devsync.infrastructure.controller.dto.CreateResponse;
import com.sysm.devsync.infrastructure.controller.dto.request.QuestionCreateUpdate;
import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.persistence.ProjectPersistencePort;
import com.sysm.devsync.domain.persistence.QuestionPersistencePort;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import com.sysm.devsync.domain.persistence.UserPersistencePort;

public class QuestionService {

    private final QuestionPersistencePort questionPersistence;
    private final ProjectPersistencePort projectPersistence;
    private final TagPersistencePort tagPersistence;
    private final UserPersistencePort userPersistence;

    public QuestionService(QuestionPersistencePort questionPersistence, ProjectPersistencePort projectPersistence,
                           TagPersistencePort tagPersistence, UserPersistencePort userPersistence) {
        this.questionPersistence = questionPersistence;
        this.projectPersistence = projectPersistence;
        this.tagPersistence = tagPersistence;
        this.userPersistence = userPersistence;
    }

    public CreateResponse createQuestion(QuestionCreateUpdate questionCreateUpdate, String authorId) {
        var projectExist = projectPersistence.existsById(questionCreateUpdate.projectId());
        if (!projectExist) {
            throw new NotFoundException("Project not found", questionCreateUpdate.projectId());
        }

        var userExists = userPersistence.existsById(authorId);
        if (!userExists) {
            throw new NotFoundException("User not found", authorId);
        }

        var question = Question.create(
                questionCreateUpdate.title(),
                questionCreateUpdate.description(),
                questionCreateUpdate.projectId(),
                authorId
        );

        questionPersistence.create(question);
        return new CreateResponse(question.getId());
    }

    public void updateQuestion(String questionId, QuestionCreateUpdate questionUpdate) {
        var question = questionPersistence.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found", questionId));

        question.update(
                questionUpdate.title(),
                questionUpdate.description()
        );

        questionPersistence.update(question);
    }

    public void updateQuestionStatus(String questionId, QuestionStatus questionUpdate) {
        var question = questionPersistence.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found", questionId));

        question.changeStatus(questionUpdate);

        questionPersistence.update(question);
    }

    public void addTagToQuestion(String questionId, String tagId) {
        var question = questionPersistence.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found", questionId));

        var tagExist = tagPersistence.existsById(tagId);
        if (!tagExist) {
            throw new NotFoundException("Tag not found", tagId);
        }

        question.addTag(tagId);
        questionPersistence.update(question);
    }

    public void removeTagFromQuestion(String questionId, String tagId) {
        var question = questionPersistence.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found", questionId));

        var tagExist = tagPersistence.existsById(tagId);
        if (!tagExist) {
            throw new NotFoundException("Tag not found", tagId);
        }

        question.removeTag(tagId);
        questionPersistence.update(question);
    }

    public void deleteQuestion(String questionId) {
        var exist = questionPersistence.existsById(questionId);
        if (!exist) {
            throw new NotFoundException("Question not found", questionId);
        }

        questionPersistence.deleteById(questionId);
    }

    public Question getQuestionById(String questionId) {
        return questionPersistence.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found", questionId));
    }

    public Pagination<Question> getAllQuestions(Pageable pageable, String projectId) {
        var projectExist = projectPersistence.existsById(projectId);
        if (!projectExist) {
            throw new NotFoundException("Project not found", projectId);
        }

        return questionPersistence.findAllByProjectId(pageable, projectId);
    }

    public Pagination<Question> getAllQuestions(SearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Invalid query parameters");
        }
        return questionPersistence.findAll(query);
    }

}
