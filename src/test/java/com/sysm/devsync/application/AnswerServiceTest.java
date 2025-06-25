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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class AnswerServiceTest {

    @Mock
    private AnswerPersistencePort answerPersistence;
    @Mock
    private QuestionPersistencePort questionPersistence;
    @Mock
    private UserPersistencePort userPersistencePort;

    @InjectMocks
    private AnswerService answerService;

    private String answerId;
    private String questionId;
    private String authorId;
    private AnswerCreateUpdate answerCreateUpdateDto;
    private Answer mockAnswer;

    @BeforeEach
    void setUp() {
        answerId = UUID.randomUUID().toString();
        questionId = UUID.randomUUID().toString();
        authorId = UUID.randomUUID().toString();

        answerCreateUpdateDto = new AnswerCreateUpdate(
                "This is the content of the test answer."
        );
        mockAnswer = mock(Answer.class);
    }

    @Test
    @DisplayName("createAnswer should create and save answer when question and author exist")
    void createAnswer_shouldCreateAndSaveAnswer_whenQuestionAndAuthorExist() {
        // Arrange
        when(questionPersistence.existsById(questionId)).thenReturn(true);
        when(userPersistencePort.existsById(authorId)).thenReturn(true);
        ArgumentCaptor<Answer> answerCaptor = ArgumentCaptor.forClass(Answer.class);
        // We'll let Answer.create() be called, and capture the result for verification.

        // Act
        CreateResponse response = answerService.createAnswer(answerCreateUpdateDto, questionId, authorId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());

        verify(questionPersistence).existsById(questionId);
        verify(userPersistencePort).existsById(authorId);
        verify(answerPersistence).create(answerCaptor.capture());

        Answer capturedAnswer = answerCaptor.getValue();
        assertEquals(questionId, capturedAnswer.getQuestionId());
        assertEquals(authorId, capturedAnswer.getAuthorId());
        assertEquals(answerCreateUpdateDto.content(), capturedAnswer.getContent());
        assertEquals(response.id(), capturedAnswer.getId());
    }

    @Test
    @DisplayName("createAnswer should throw NotFoundException when question does not exist")
    void createAnswer_shouldThrowNotFoundException_whenQuestionDoesNotExist() {
        // Arrange
        when(questionPersistence.existsById(questionId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            answerService.createAnswer(answerCreateUpdateDto, questionId, authorId);
        });
        assertEquals("Question not found", exception.getMessage());
        verify(userPersistencePort, never()).existsById(anyString());
        verify(answerPersistence, never()).create(any(Answer.class));
    }

    @Test
    @DisplayName("createAnswer should throw NotFoundException when author does not exist")
    void createAnswer_shouldThrowNotFoundException_whenAuthorDoesNotExist() {
        // Arrange
        when(questionPersistence.existsById(questionId)).thenReturn(true);
        when(userPersistencePort.existsById(authorId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            answerService.createAnswer(answerCreateUpdateDto, questionId, authorId);
        });
        assertEquals("Author not found", exception.getMessage());
        verify(answerPersistence, never()).create(any(Answer.class));
    }

    @Test
    @DisplayName("updateAnswer should update existing answer's content")
    void updateAnswer_shouldUpdateExistingAnswerContent() {
        // Arrange
        AnswerCreateUpdate updateDto = new AnswerCreateUpdate("Updated content.");
        when(answerPersistence.findById(answerId)).thenReturn(Optional.of(mockAnswer));

        // Act
        answerService.updateAnswer(answerId, updateDto);

        // Assert
        verify(answerPersistence).findById(answerId);
        verify(mockAnswer).update(updateDto.content());
        verify(answerPersistence).update(mockAnswer);
    }

    @Test
    @DisplayName("updateAnswer should throw NotFoundException when answer not found")
    void updateAnswer_shouldThrowNotFoundException_whenAnswerNotFound() {
        // Arrange
        AnswerCreateUpdate updateDto = new AnswerCreateUpdate("Updated content.");
        when(answerPersistence.findById(answerId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            answerService.updateAnswer(answerId, updateDto);
        });
        assertEquals("Answer not found", exception.getMessage());
        verify(answerPersistence, never()).update(any(Answer.class));
    }

    @Test
    @DisplayName("acceptAnswer should accept existing answer")
    void acceptAnswer_shouldAcceptExistingAnswer() {
        // Arrange
        when(answerPersistence.findById(answerId)).thenReturn(Optional.of(mockAnswer));

        // Act
        answerService.acceptAnswer(answerId);

        // Assert
        verify(answerPersistence).findById(answerId);
        verify(mockAnswer).accept();
        verify(answerPersistence).update(mockAnswer);
    }

    @Test
    @DisplayName("acceptAnswer should throw NotFoundException when answer not found")
    void acceptAnswer_shouldThrowNotFoundException_whenAnswerNotFound() {
        // Arrange
        when(answerPersistence.findById(answerId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            answerService.acceptAnswer(answerId);
        });
        assertEquals("Answer not found", exception.getMessage());
        verify(answerPersistence, never()).update(any(Answer.class));
    }

    @Test
    @DisplayName("rejectAnswer should reject existing answer")
    void rejectAnswer_shouldRejectExistingAnswer() {
        // Arrange
        when(answerPersistence.findById(answerId)).thenReturn(Optional.of(mockAnswer));

        // Act
        answerService.rejectAnswer(answerId);

        // Assert
        verify(answerPersistence).findById(answerId);
        verify(mockAnswer).reject();
        verify(answerPersistence).update(mockAnswer);
    }

    @Test
    @DisplayName("rejectAnswer should throw NotFoundException when answer not found")
    void rejectAnswer_shouldThrowNotFoundException_whenAnswerNotFound() {
        // Arrange
        when(answerPersistence.findById(answerId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            answerService.rejectAnswer(answerId);
        });
        assertEquals("Answer not found", exception.getMessage());
        verify(answerPersistence, never()).update(any(Answer.class));
    }

    @Test
    @DisplayName("deleteAnswer should call persistence deleteById when answer exists")
    void deleteAnswer_shouldCallPersistenceDeleteById_whenAnswerExists() {
        // Arrange
        when(answerPersistence.existsById(answerId)).thenReturn(true);
        doNothing().when(answerPersistence).deleteById(answerId);

        // Act
        answerService.deleteAnswer(answerId);

        // Assert
        verify(answerPersistence).existsById(answerId);
        verify(answerPersistence).deleteById(answerId);
    }

    @Test
    @DisplayName("deleteAnswer should throw NotFoundException when answer not found")
    void deleteAnswer_shouldThrowNotFoundException_whenAnswerNotFound() {
        // Arrange
        when(answerPersistence.existsById(answerId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            answerService.deleteAnswer(answerId);
        });
        assertEquals("Answer not found", exception.getMessage());
        verify(answerPersistence, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("getAnswerById should return answer when found")
    void getAnswerById_shouldReturnAnswer_whenFound() {
        // Arrange
        when(answerPersistence.findById(answerId)).thenReturn(Optional.of(mockAnswer));

        // Act
        Answer actualAnswer = answerService.getAnswerById(answerId);

        // Assert
        assertNotNull(actualAnswer);
        assertSame(mockAnswer, actualAnswer);
        verify(answerPersistence).findById(answerId);
    }

    @Test
    @DisplayName("getAnswerById should throw NotFoundException when answer not found")
    void getAnswerById_shouldThrowNotFoundException_whenAnswerNotFound() {
        // Arrange
        when(answerPersistence.findById(answerId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            answerService.getAnswerById(answerId);
        });
        assertEquals("Answer not found", exception.getMessage());
    }

    @Test
    @DisplayName("getAllAnswers with Pageable and questionId should return page when question exists")
    void getAllAnswers_withPageableAndQuestionId_shouldReturnPage_whenQuestionExists() {
        // Arrange
        Page page = new Page(0, 0, "createdAt", "desc");
        Pagination<Answer> expectedPagination = new Pagination<>(1, 10, 0L, Collections.emptyList());
        when(questionPersistence.existsById(questionId)).thenReturn(true);
        when(answerPersistence.findAllByQuestionId(page, questionId)).thenReturn(expectedPagination);

        // Act
        Pagination<Answer> actualPagination = answerService.getAllAnswers(page, questionId);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(questionPersistence).existsById(questionId);
        verify(answerPersistence).findAllByQuestionId(page, questionId);
    }

    @Test
    @DisplayName("getAllAnswers with Pageable and questionId should throw NotFoundException when question not found")
    void getAllAnswers_withPageableAndQuestionId_shouldThrowNotFoundException_whenQuestionNotFound() {
        // Arrange
        Page page = new Page(0, 0, "createdAt", "desc");
        when(questionPersistence.existsById(questionId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            answerService.getAllAnswers(page, questionId);
        });
        assertEquals("Question not found", exception.getMessage());
        verify(answerPersistence, never()).findAllByQuestionId(any(Page.class), anyString());
    }

    @Test
    @DisplayName("getAllAnswers with SearchQuery should return page from persistence")
    void getAllAnswers_withSearchQuery_shouldReturnPageFromPersistence() {
        // Arrange
        SearchQuery query = new SearchQuery(new Page(0, 0, "createdAt", "desc"), "content");
        Pagination<Answer> expectedPagination = new Pagination<>(1, 10, 0L, Collections.emptyList());
        when(answerPersistence.findAll(query)).thenReturn(expectedPagination);

        // Act
        Pagination<Answer> actualPagination = answerService.getAllAnswers(query);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(answerPersistence).findAll(query);
    }

    @Test
    @DisplayName("getAllAnswers with SearchQuery should throw IllegalArgumentException when query is null")
    void getAllAnswers_withSearchQuery_shouldThrowIllegalArgumentException_whenQueryIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            answerService.getAllAnswers((SearchQuery) null);
        });
        assertEquals("Invalid query parameters", exception.getMessage());
        verify(answerPersistence, never()).findAll(any(SearchQuery.class));
    }
}
