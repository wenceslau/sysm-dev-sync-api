package com.sysm.devsync.application.security;

import com.sysm.devsync.domain.persistence.*;

public class SecurityService {

    private final NotePersistencePort notePersistence;
    private final AnswerPersistencePort answerPersistence;
    private final QuestionPersistencePort questionPersistence;
    private final WorkspacePersistencePort workspacePersistence;
    // Add other persistence ports as needed

    public SecurityService(NotePersistencePort notePersistence,
                           AnswerPersistencePort answerPersistence,
                           QuestionPersistencePort questionPersistence,
                           WorkspacePersistencePort workspacePersistence) {
        this.notePersistence = notePersistence;
        this.answerPersistence = answerPersistence;
        this.questionPersistence = questionPersistence;
        this.workspacePersistence = workspacePersistence;
    }

    public boolean isAnswerOwner(String currentUserId, String answerId) {
        return answerPersistence.findById(answerId)
                .map(answer -> answer.getAuthorId().equals(currentUserId))
                .orElse(false); // If answer not found, deny access for safety
    }

    public boolean isNoteOwner(String currentUserId, String noteId) {
        return notePersistence.findById(noteId)
                .map(note -> note.getAuthorId().equals(currentUserId))
                .orElse(false);
    }

    public boolean isQuestionOwner(String currentUserId, String questionId) {
        return questionPersistence.findById(questionId)
                .map(question -> currentUserId.equals(question.getAuthorId()))
                .orElse(false);
    }

    public boolean canUserAcceptAnswer(String currentUserId, String answerId) {
        // Find the answer, then find its question, then check the question's author.
        // The transaction is handled by the individual persistence calls.
        return answerPersistence.findById(answerId)
                .flatMap(answer -> questionPersistence.findById(answer.getQuestionId()))
                .map(question -> currentUserId.equals(question.getAuthorId()))
                .orElse(false);
    }

    // You can add more complex checks here, for example:
    // - isWorkspaceMember(auth, workspaceId)
    // - canUserViewProject(auth, projectId)
    // etc.
}
