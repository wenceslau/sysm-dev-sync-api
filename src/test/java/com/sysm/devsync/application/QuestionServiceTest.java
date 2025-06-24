package com.sysm.devsync.application;

import com.sysm.devsync.infrastructure.controllers.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionCreateUpdate;
import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.persistence.ProjectPersistencePort;
import com.sysm.devsync.domain.persistence.QuestionPersistencePort;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
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
class QuestionServiceTest {

    @Mock
    private QuestionPersistencePort questionPersistence;
    @Mock
    private ProjectPersistencePort projectPersistence;
    @Mock
    private TagPersistencePort tagPersistence;
    @Mock
    private UserPersistencePort userPersistence;

    @InjectMocks
    private QuestionService questionService;

    private String questionId;
    private String projectId;
    private String authorId;
    private String tagId;
    private QuestionCreateUpdate questionCreateUpdateDto;
    private Question mockQuestion;

    @BeforeEach
    void setUp() {
        questionId = UUID.randomUUID().toString();
        projectId = UUID.randomUUID().toString();
        authorId = UUID.randomUUID().toString();
        tagId = UUID.randomUUID().toString();

        questionCreateUpdateDto = new QuestionCreateUpdate(
                "Test Question Title",
                "Detailed description of the test question.",
                projectId
        );
        mockQuestion = mock(Question.class);
    }

    @Test
    @DisplayName("createQuestion should create and save question when project and user exist")
    void createQuestion_shouldCreateAndSaveQuestion_whenProjectAndUserExist() {
        // Arrange
        when(projectPersistence.existsById(projectId)).thenReturn(true);
        when(userPersistence.existsById(authorId)).thenReturn(true);
        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        // Assuming Question.create() returns a question with a generated ID
        // For simplicity, we'll let the real Question.create happen.

        // Act
        CreateResponse response = questionService.createQuestion(questionCreateUpdateDto, authorId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());

        verify(projectPersistence).existsById(projectId);
        verify(userPersistence).existsById(authorId);
        verify(questionPersistence).create(questionCaptor.capture());

        Question capturedQuestion = questionCaptor.getValue();
        assertEquals(questionCreateUpdateDto.title(), capturedQuestion.getTitle());
        assertEquals(questionCreateUpdateDto.description(), capturedQuestion.getDescription());
        assertEquals(projectId, capturedQuestion.getProjectId());
        assertEquals(authorId, capturedQuestion.getAuthorId());
        assertEquals(response.id(), capturedQuestion.getId());
    }

    @Test
    @DisplayName("createQuestion should throw NotFoundException when project does not exist")
    void createQuestion_shouldThrowNotFoundException_whenProjectDoesNotExist() {
        // Arrange
        when(projectPersistence.existsById(projectId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.createQuestion(questionCreateUpdateDto, authorId);
        });
        assertEquals("Project not found", exception.getMessage());
        verify(userPersistence, never()).existsById(anyString());
        verify(questionPersistence, never()).create(any(Question.class));
    }

    @Test
    @DisplayName("createQuestion should throw NotFoundException when user does not exist")
    void createQuestion_shouldThrowNotFoundException_whenUserDoesNotExist() {
        // Arrange
        when(projectPersistence.existsById(projectId)).thenReturn(true);
        when(userPersistence.existsById(authorId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.createQuestion(questionCreateUpdateDto, authorId);
        });
        assertEquals("User not found", exception.getMessage());
        verify(questionPersistence, never()).create(any(Question.class));
    }

    @Test
    @DisplayName("updateQuestion should update existing question")
    void updateQuestion_shouldUpdateExistingQuestion() {
        // Arrange
        QuestionCreateUpdate updateDto = new QuestionCreateUpdate("Updated Title", "Updated Desc", null);
        when(questionPersistence.findById(questionId)).thenReturn(Optional.of(mockQuestion));

        // Act
        questionService.updateQuestion(questionId, updateDto);

        // Assert
        verify(questionPersistence).findById(questionId);
        verify(mockQuestion).update(updateDto.title(), updateDto.description());
        verify(questionPersistence).update(mockQuestion);
    }

    @Test
    @DisplayName("updateQuestion should throw NotFoundException when question not found")
    void updateQuestion_shouldThrowNotFoundException_whenQuestionNotFound() {
        // Arrange
        QuestionCreateUpdate updateDto = new QuestionCreateUpdate("Updated Title", "Updated Desc", null);
        when(questionPersistence.findById(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.updateQuestion(questionId, updateDto);
        });
        assertEquals("Question not found", exception.getMessage());
        verify(questionPersistence, never()).update(any(Question.class));
    }

    @Test
    @DisplayName("updateQuestionStatus should update existing question's status")
    void updateQuestionStatus_shouldUpdateExistingQuestionStatus() {
        // Arrange
        QuestionStatus newStatus = QuestionStatus.RESOLVED;
        when(questionPersistence.findById(questionId)).thenReturn(Optional.of(mockQuestion));

        // Act
        questionService.updateQuestionStatus(questionId, newStatus);

        // Assert
        verify(questionPersistence).findById(questionId);
        verify(mockQuestion).changeStatus(newStatus);
        verify(questionPersistence).update(mockQuestion);
    }

    @Test
    @DisplayName("updateQuestionStatus should throw NotFoundException when question not found")
    void updateQuestionStatus_shouldThrowNotFoundException_whenQuestionNotFound() {
        // Arrange
        QuestionStatus newStatus = QuestionStatus.CLOSED;
        when(questionPersistence.findById(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.updateQuestionStatus(questionId, newStatus);
        });
        assertEquals("Question not found", exception.getMessage());
        verify(questionPersistence, never()).update(any(Question.class));
    }


    @Test
    @DisplayName("addTagToQuestion should add tag and update question when question and tag exist")
    void addTagToQuestion_shouldAddTagAndUpdateQuestion_whenAllExist() {
        // Arrange
        when(questionPersistence.findById(questionId)).thenReturn(Optional.of(mockQuestion));
        when(tagPersistence.existsById(tagId)).thenReturn(true);

        // Act
        questionService.addTagToQuestion(questionId, tagId);

        // Assert
        verify(questionPersistence).findById(questionId);
        verify(tagPersistence).existsById(tagId);
        verify(mockQuestion).addTag(tagId);
        verify(questionPersistence).update(mockQuestion);
    }

    @Test
    @DisplayName("addTagToQuestion should throw NotFoundException when question not found")
    void addTagToQuestion_shouldThrowNotFoundException_whenQuestionNotFound() {
        // Arrange
        when(questionPersistence.findById(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.addTagToQuestion(questionId, tagId);
        });
        assertEquals("Question not found", exception.getMessage());
        verify(tagPersistence, never()).existsById(anyString());
        verify(questionPersistence, never()).update(any(Question.class));
    }

    @Test
    @DisplayName("addTagToQuestion should throw NotFoundException when tag not found")
    void addTagToQuestion_shouldThrowNotFoundException_whenTagNotFound() {
        // Arrange
        when(questionPersistence.findById(questionId)).thenReturn(Optional.of(mockQuestion));
        when(tagPersistence.existsById(tagId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.addTagToQuestion(questionId, tagId);
        });
        assertEquals("Tag not found", exception.getMessage());
        verify(mockQuestion, never()).addTag(anyString());
        verify(questionPersistence, never()).update(any(Question.class));
    }

    @Test
    @DisplayName("removeTagFromQuestion should remove tag and update question when question and tag exist")
    void removeTagFromQuestion_shouldRemoveTagAndUpdateQuestion_whenAllExist() {
        // Arrange
        when(questionPersistence.findById(questionId)).thenReturn(Optional.of(mockQuestion));
        when(tagPersistence.existsById(tagId)).thenReturn(true);

        // Act
        questionService.removeTagFromQuestion(questionId, tagId);

        // Assert
        verify(questionPersistence).findById(questionId);
        verify(tagPersistence).existsById(tagId);
        verify(mockQuestion).removeTag(tagId);
        verify(questionPersistence).update(mockQuestion);
    }

    @Test
    @DisplayName("removeTagFromQuestion should throw NotFoundException when question not found")
    void removeTagFromQuestion_shouldThrowNotFoundException_whenQuestionNotFound() {
        // Arrange
        when(questionPersistence.findById(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.removeTagFromQuestion(questionId, tagId);
        });
        assertEquals("Question not found", exception.getMessage());
        verify(tagPersistence, never()).existsById(anyString());
        verify(questionPersistence, never()).update(any(Question.class));
    }

    @Test
    @DisplayName("removeTagFromQuestion should throw NotFoundException when tag not found")
    void removeTagFromQuestion_shouldThrowNotFoundException_whenTagNotFound() {
        // Arrange
        when(questionPersistence.findById(questionId)).thenReturn(Optional.of(mockQuestion));
        when(tagPersistence.existsById(tagId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.removeTagFromQuestion(questionId, tagId);
        });
        assertEquals("Tag not found", exception.getMessage());
        verify(mockQuestion, never()).removeTag(anyString());
        verify(questionPersistence, never()).update(any(Question.class));
    }

    @Test
    @DisplayName("deleteQuestion should call persistence deleteById when question exists")
    void deleteQuestion_shouldCallPersistenceDeleteById_whenQuestionExists() {
        // Arrange
        when(questionPersistence.existsById(questionId)).thenReturn(true);
        doNothing().when(questionPersistence).deleteById(questionId);

        // Act
        questionService.deleteQuestion(questionId);

        // Assert
        verify(questionPersistence).existsById(questionId);
        verify(questionPersistence).deleteById(questionId);
    }

    @Test
    @DisplayName("deleteQuestion should throw NotFoundException when question not found")
    void deleteQuestion_shouldThrowNotFoundException_whenQuestionNotFound() {
        // Arrange
        when(questionPersistence.existsById(questionId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.deleteQuestion(questionId);
        });
        assertEquals("Question not found", exception.getMessage());
        verify(questionPersistence, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("getQuestionById should return question when found")
    void getQuestionById_shouldReturnQuestion_whenFound() {
        // Arrange
        when(questionPersistence.findById(questionId)).thenReturn(Optional.of(mockQuestion));

        // Act
        Question actualQuestion = questionService.getQuestionById(questionId);

        // Assert
        assertNotNull(actualQuestion);
        assertSame(mockQuestion, actualQuestion);
        verify(questionPersistence).findById(questionId);
    }

    @Test
    @DisplayName("getQuestionById should throw NotFoundException when question not found")
    void getQuestionById_shouldThrowNotFoundException_whenQuestionNotFound() {
        // Arrange
        when(questionPersistence.findById(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.getQuestionById(questionId);
        });
        assertEquals("Question not found", exception.getMessage());
    }

    @Test
    @DisplayName("getAllQuestions with Pageable and projectId should return page when project exists")
    void getAllQuestions_withPageableAndProjectId_shouldReturnPage_whenProjectExists() {
        // Arrange
        Page page = new Page(0, 10, "createdAt", "desc"); // Assuming a static factory method for Pageable
        Pagination<Question> expectedPagination = new Pagination<>(0, 10, 0L, Collections.emptyList());
        when(projectPersistence.existsById(projectId)).thenReturn(true);
        when(questionPersistence.findAllByProjectId(page, projectId)).thenReturn(expectedPagination);

        // Act
        Pagination<Question> actualPagination = questionService.getAllQuestions(page, projectId);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(projectPersistence).existsById(projectId);
        verify(questionPersistence).findAllByProjectId(page, projectId);
    }

    @Test
    @DisplayName("getAllQuestions with Pageable and projectId should throw NotFoundException when project not found")
    void getAllQuestions_withPageableAndProjectId_shouldThrowNotFoundException_whenProjectNotFound() {
        // Arrange
        Page page = new Page(0, 10, "createdAt", "desc"); // Assuming a static factory method for Pageable
        when(projectPersistence.existsById(projectId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            questionService.getAllQuestions(page, projectId);
        });
        assertEquals("Project not found", exception.getMessage());
        verify(questionPersistence, never()).findAllByProjectId(any(Page.class), anyString());
    }

    @Test
    @DisplayName("getAllQuestions with SearchQuery should return page from persistence")
    void getAllQuestions_withSearchQuery_shouldReturnPageFromPersistence() {
        // Arrange
        SearchQuery query = new SearchQuery(new Page(0, 10, "createdAt", "desc"), "title");
        Pagination<Question> expectedPagination = new Pagination<>(0, 10, 0L, Collections.emptyList());
        when(questionPersistence.findAll(query)).thenReturn(expectedPagination);

        // Act
        Pagination<Question> actualPagination = questionService.getAllQuestions(query);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(questionPersistence).findAll(query);
    }

    @Test
    @DisplayName("getAllQuestions with SearchQuery should throw IllegalArgumentException when query is null")
    void getAllQuestions_withSearchQuery_shouldThrowIllegalArgumentException_whenQueryIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            questionService.getAllQuestions( null);
        });
        assertEquals("Invalid query parameters", exception.getMessage());
        verify(questionPersistence, never()).findAll(any(SearchQuery.class));
    }
}
