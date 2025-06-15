package com.sysm.devsync.domain.repositories;

import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.Note;
import com.sysm.devsync.domain.models.Project;

public interface NotePersistencePort extends PersistencePort<Note> {

    Page<Note> findAllByProjectId(Pageable pageable, String projectId);

}
