package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.Question;

public interface QuestionPersistencePort extends PersistencePort<Question> {

    Pagination<Question> findAllByProjectId(Page page, String projectId);


}
