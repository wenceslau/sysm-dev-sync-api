package com.sysm.devsync.application;

import com.sysm.devsync.controller.dto.CreateResponse;
import com.sysm.devsync.controller.dto.request.CommentCreateUpdate;
import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.models.Comment;
import com.sysm.devsync.domain.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentPersistencePort commentPersistence;
    @Mock
    private NotePersistencePort notePersistence;
    @Mock
    private QuestionPersistencePort questionPersistence;
    @Mock
    private AnswerPersistencePort answerPersistence;
    @Mock
    private UserPersistencePort userPersistence;

    @InjectMocks
    private CommentService commentService;

    private String commentId;
    private String targetId;
    private String authorId;
    private CommentCreateUpdate commentCreateUpdateDto;
    private Comment mockComment;

    @BeforeEach
    void setUp() {
        commentId = UUID.randomUUID().toString();
        targetId = UUID.randomUUID().toString();
        authorId = UUID.randomUUID().toString();

        commentCreateUpdateDto = new CommentCreateUpdate(
                TargetType.NOTE, // Default, can be overridden in tests
                targetId,
                "This is a test comment."
        );
        mockComment = mock(Comment.class); // Used for methods that find and then operate on a comment
    }

    @Nested
    @DisplayName("createComment Tests")
    class CreateCommentTests {

        @Test
        @DisplayName("should create and save comment when author and target exist")
        void createComment_success() {
            // Arrange
            when(userPersistence.existsById(authorId)).thenReturn(true);
            when(notePersistence.existsById(targetId)).thenReturn(true); // Assuming TargetType.NOTE

            ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
            // We'll let Comment.create() be called and capture the result for verification.

            // Act
            CreateResponse response = commentService.createComment(commentCreateUpdateDto, authorId);

            // Assert
            assertNotNull(response);
            assertNotNull(response.id());

            verify(userPersistence).existsById(authorId);
            verify(notePersistence).existsById(targetId); // Verifying the specific target type check
            verify(commentPersistence).create(commentCaptor.capture());

            Comment capturedComment = commentCaptor.getValue();
            assertEquals(commentCreateUpdateDto.targetType(), capturedComment.getTargetType());
            assertEquals(commentCreateUpdateDto.targetId(), capturedComment.getTargetId());
            assertEquals(authorId, capturedComment.getAuthorId());
            assertEquals(commentCreateUpdateDto.content(), capturedComment.getContent());
            assertEquals(response.id(), capturedComment.getId());
        }

        @Test
        @DisplayName("should throw NotFoundException when author does not exist")
        void createComment_authorNotFound() {
            // Arrange
            when(userPersistence.existsById(authorId)).thenReturn(false);

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.createComment(commentCreateUpdateDto, authorId);
            });
            assertEquals("Author not found", exception.getMessage());
            assertEquals(authorId, exception.getId());
            verify(notePersistence, never()).existsById(anyString());
            verify(commentPersistence, never()).create(any(Comment.class));
        }

        @Test
        @DisplayName("should throw NotFoundException when NOTE target does not exist")
        void createComment_noteTargetNotFound() {
            // Arrange
            CommentCreateUpdate dto = new CommentCreateUpdate(TargetType.NOTE, targetId, "content");
            when(userPersistence.existsById(authorId)).thenReturn(true);
            when(notePersistence.existsById(targetId)).thenReturn(false);

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.createComment(dto, authorId);
            });
            assertEquals("Note not found", exception.getMessage());
            assertEquals(targetId, exception.getId());
            verify(commentPersistence, never()).create(any(Comment.class));
        }

        @Test
        @DisplayName("should throw NotFoundException when QUESTION target does not exist")
        void createComment_questionTargetNotFound() {
            // Arrange
            CommentCreateUpdate dto = new CommentCreateUpdate(TargetType.QUESTION, targetId, "content");
            when(userPersistence.existsById(authorId)).thenReturn(true);
            when(questionPersistence.existsById(targetId)).thenReturn(false);

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.createComment(dto, authorId);
            });
            assertEquals("Question not found", exception.getMessage());
            assertEquals(targetId, exception.getId());
        }

        @Test
        @DisplayName("should throw NotFoundException when ANSWER target does not exist")
        void createComment_answerTargetNotFound() {
            // Arrange
            CommentCreateUpdate dto = new CommentCreateUpdate(TargetType.ANSWER, targetId, "content");
            when(userPersistence.existsById(authorId)).thenReturn(true);
            when(answerPersistence.existsById(targetId)).thenReturn(false);

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.createComment(dto, authorId);
            });
            assertEquals("Answer not found", exception.getMessage());
            assertEquals(targetId, exception.getId());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when targetId is null")
        void createComment_targetIdNull() {
            // Arrange
            CommentCreateUpdate dto = new CommentCreateUpdate(TargetType.NOTE, null, "content");
            when(userPersistence.existsById(authorId)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                commentService.createComment(dto, authorId);
            });
            assertEquals("Target ID cannot be null or empty.", exception.getMessage());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when targetType is null")
        void createComment_targetTypeNull() {
            // Arrange
            CommentCreateUpdate dto = new CommentCreateUpdate(null, targetId, "content");
            when(userPersistence.existsById(authorId)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                commentService.createComment(dto, authorId);
            });
            assertEquals("Target type cannot be null.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("updateComment Tests")
    class UpdateCommentTests {
        @Test
        @DisplayName("should update existing comment")
        void updateComment_success() {
            // Arrange
            CommentCreateUpdate updateDto = new CommentCreateUpdate(null, null, "Updated comment content.");
            when(commentPersistence.findById(commentId)).thenReturn(Optional.of(mockComment));

            // Act
            commentService.updateComment(commentId, updateDto);

            // Assert
            verify(commentPersistence).findById(commentId);
            verify(mockComment).update(updateDto.content());
            verify(commentPersistence).update(mockComment);
        }

        @Test
        @DisplayName("should throw NotFoundException when comment not found")
        void updateComment_commentNotFound() {
            // Arrange
            CommentCreateUpdate updateDto = new CommentCreateUpdate(null, null, "Updated content.");
            when(commentPersistence.findById(commentId)).thenReturn(Optional.empty());

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.updateComment(commentId, updateDto);
            });
            assertEquals("Comment not found", exception.getMessage());
            assertEquals(commentId, exception.getId());
            verify(commentPersistence, never()).update(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("deleteComment Tests")
    class DeleteCommentTests {
        @Test
        @DisplayName("should call persistence deleteById when comment exists")
        void deleteComment_success() {
            // Arrange
            when(commentPersistence.existsById(commentId)).thenReturn(true);
            doNothing().when(commentPersistence).deleteById(commentId);

            // Act
            commentService.deleteComment(commentId);

            // Assert
            verify(commentPersistence).existsById(commentId);
            verify(commentPersistence).deleteById(commentId);
        }

        @Test
        @DisplayName("should throw NotFoundException when comment not found")
        void deleteComment_commentNotFound() {
            // Arrange
            when(commentPersistence.existsById(commentId)).thenReturn(false);

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.deleteComment(commentId);
            });
            assertEquals("Comment not found", exception.getMessage());
            assertEquals(commentId, exception.getId());
            verify(commentPersistence, never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("getCommentById Tests")
    class GetCommentByIdTests {
        @Test
        @DisplayName("should return comment when found")
        void getCommentById_success() {
            // Arrange
            when(commentPersistence.findById(commentId)).thenReturn(Optional.of(mockComment));

            // Act
            Comment actualComment = commentService.getCommentById(commentId);

            // Assert
            assertNotNull(actualComment);
            assertSame(mockComment, actualComment);
            verify(commentPersistence).findById(commentId);
        }

        @Test
        @DisplayName("should throw NotFoundException when comment not found")
        void getCommentById_commentNotFound() {
            // Arrange
            when(commentPersistence.findById(commentId)).thenReturn(Optional.empty());

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.getCommentById(commentId);
            });
            assertEquals("Comment not found", exception.getMessage());
            assertEquals(commentId, exception.getId());
        }
    }

    @Nested
    @DisplayName("getAllComments (by target) Tests")
    class GetAllCommentsByTargetTests {
        private Pageable pageable;

        @BeforeEach
        void PagedSetup() {
            pageable = new Pageable(1, 10, "id", "desc");
        }

        @Test
        @DisplayName("should return page when NOTE target exists")
        void getAllComments_noteTargetExists() {
            // Arrange
            Page<Comment> expectedPage = new Page<>(0, 10, 0L, Collections.emptyList());
            when(notePersistence.existsById(targetId)).thenReturn(true);
            when(commentPersistence.findAllByTargetId(pageable, TargetType.NOTE, targetId)).thenReturn(expectedPage);

            // Act
            Page<Comment> actualPage = commentService.getAllComments(pageable, targetId, TargetType.NOTE);

            // Assert
            assertNotNull(actualPage);
            assertSame(expectedPage, actualPage);
            verify(notePersistence).existsById(targetId);
            verify(commentPersistence).findAllByTargetId(pageable, TargetType.NOTE, targetId);
        }

        @Test
        @DisplayName("should throw NotFoundException when NOTE target not found")
        void getAllComments_noteTargetNotFound() {
            // Arrange
            when(notePersistence.existsById(targetId)).thenReturn(false);

            // Act & Assert
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.getAllComments(pageable, targetId, TargetType.NOTE);
            });
            assertEquals("Note not found", exception.getMessage());
            assertEquals(targetId, exception.getId());
            verify(commentPersistence, never()).findAllByTargetId(any(), any(), anyString());
        }

        // Similar tests for QUESTION and ANSWER targets
        @Test
        @DisplayName("should throw NotFoundException when QUESTION target not found")
        void getAllComments_questionTargetNotFound() {
            when(questionPersistence.existsById(targetId)).thenReturn(false);
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.getAllComments(pageable, targetId, TargetType.QUESTION);
            });
            assertEquals("Question not found", exception.getMessage());
            assertEquals(targetId, exception.getId());
        }

        @Test
        @DisplayName("should throw NotFoundException when ANSWER target not found")
        void getAllComments_answerTargetNotFound() {
            when(answerPersistence.existsById(targetId)).thenReturn(false);
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                commentService.getAllComments(pageable, targetId, TargetType.ANSWER);
            });
            assertEquals("Answer not found", exception.getMessage());
            assertEquals(targetId, exception.getId());
        }


        @Test
        @DisplayName("should throw IllegalArgumentException when targetId is null")
        void getAllComments_targetIdNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                commentService.getAllComments(pageable, null, TargetType.NOTE);
            });
            assertEquals("Target ID cannot be null or empty.", exception.getMessage());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when targetType is null")
        void getAllComments_targetTypeNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                commentService.getAllComments(pageable, targetId, null);
            });
            assertEquals("Target type cannot be null.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getAllComments (by SearchQuery) Tests")
    class GetAllCommentsBySearchQueryTests {
        @Test
        @DisplayName("should return page from persistence")
        void getAllComments_withSearchQuery_success() {
            // Arrange
            SearchQuery query = new SearchQuery(new Pageable(1, 10, "id", "desc"), "content");
            Page<Comment> expectedPage = new Page<>(0, 10, 0L, Collections.emptyList());
            when(commentPersistence.findAll(query)).thenReturn(expectedPage);

            // Act
            Page<Comment> actualPage = commentService.getAllComments(query);

            // Assert
            assertNotNull(actualPage);
            assertSame(expectedPage, actualPage);
            verify(commentPersistence).findAll(query);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when query is null")
        void getAllComments_withSearchQuery_queryNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                commentService.getAllComments((SearchQuery) null);
            });
            assertEquals("Invalid query parameters", exception.getMessage());
            verify(commentPersistence, never()).findAll(any(SearchQuery.class));
        }
    }
}
