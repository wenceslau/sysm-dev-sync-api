package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.Note;

public interface NotePersistencePort extends PersistencePort<Note> {

    Pagination<Note> findAllByProjectId(Page page, String projectId);

}
