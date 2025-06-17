package com.sysm.devsync.application;

import com.sysm.devsync.infrastructure.controller.dto.CreateResponse;
import com.sysm.devsync.infrastructure.controller.dto.request.CommentCreateUpdate;
import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.models.Comment;
import com.sysm.devsync.domain.persistence.*;

public class CommentService {

    private final CommentPersistencePort commentPersistence;
    private final NotePersistencePort notePersistence;
    private final QuestionPersistencePort questionPersistence;
    private final AnswerPersistencePort answerPersistence;
    private final UserPersistencePort userPersistence;

    public CommentService(CommentPersistencePort commentPersistence, NotePersistencePort notePersistence,
                          QuestionPersistencePort questionPersistence, AnswerPersistencePort answerPersistence, UserPersistencePort userPersistence) {
        this.commentPersistence = commentPersistence;
        this.notePersistence = notePersistence;
        this.questionPersistence = questionPersistence;
        this.answerPersistence = answerPersistence;
        this.userPersistence = userPersistence;
    }

    public CreateResponse createComment(CommentCreateUpdate commentCreate, String authorId){

        var authorExist = userPersistence.existsById(authorId);
        if (!authorExist) {
            throw new NotFoundException("Author not found", authorId);
        }

        validateTargetExistence(commentCreate.targetId(), commentCreate.targetType());

        var comment = Comment.create(
                commentCreate.targetType(),
                commentCreate.targetId(),
                authorId,
                commentCreate.content()
        );

        commentPersistence.create(comment);
        return new CreateResponse(comment.getId());

    }

    public void updateComment(String commentId, CommentCreateUpdate commentUpdate) {
        var comment = commentPersistence.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found", commentId));

        comment.update(commentUpdate.content());
        commentPersistence.update(comment);
    }

    public void deleteComment(String commentId) {
        var exist = commentPersistence.existsById(commentId);
        if (!exist) {
            throw new NotFoundException("Comment not found", commentId);
        }
        commentPersistence.deleteById(commentId);
    }

    public Comment getCommentById(String commentId) {
        return commentPersistence.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found", commentId));
    }

    public Pagination<Comment> getAllComments(Pageable pageable, String targetId, TargetType targetType) {
        validateTargetExistence(targetId, targetType);
        return commentPersistence.findAllByTargetId(pageable, targetType, targetId);

    }

    public Pagination<Comment> getAllComments(SearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Invalid query parameters");
        }
        return commentPersistence.findAll(query);
    }

    private void validateTargetExistence(String targetId, TargetType targetType) {
        if (targetId == null || targetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Target ID cannot be null or empty.");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Target type cannot be null.");
        }

        boolean exists;
        String targetName = ""; // For more descriptive error messages

        exists = switch (targetType) {
            case NOTE -> {
                targetName = "Note";
                yield notePersistence.existsById(targetId);
            }
            case QUESTION -> {
                targetName = "Question";
                yield questionPersistence.existsById(targetId);
            }
            case ANSWER -> {
                targetName = "Answer";
                yield answerPersistence.existsById(targetId);
            }
        };

        if (!exists) {
            throw new NotFoundException(targetName + " not found", targetId);
        }
    }

}
