package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Pageable;
import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.models.Comment;

public interface CommentPersistencePort extends PersistencePort<Comment> {
    Pagination<Comment> findAllByTargetId(Pageable pageable, TargetType targetType, String targetId);

}
