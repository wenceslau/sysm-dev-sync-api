package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.Answer;

public interface AnswerPersistencePort extends PersistencePort<Answer> {

    Pagination<Answer> findAllByQuestionId(Pageable pageable, String questionId);

}
