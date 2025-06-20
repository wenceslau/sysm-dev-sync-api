package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.Answer;

public interface AnswerPersistencePort extends PersistencePort<Answer> {

    Pagination<Answer> findAllByQuestionId(Page page, String questionId);

}
