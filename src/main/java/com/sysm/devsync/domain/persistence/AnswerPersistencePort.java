package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.Answer;

public interface AnswerPersistencePort extends PersistencePort<Answer> {

    Page<Answer> findAllByQuestionId(Pageable pageable, String questionId);

}
