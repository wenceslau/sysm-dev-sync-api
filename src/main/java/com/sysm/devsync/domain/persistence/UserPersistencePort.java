package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.infrastructure.repositories.objects.KeyValue;

import java.util.List;

public interface UserPersistencePort extends PersistencePort<User> {

    List<KeyValue> userIdXUseName(List<String> userIds);

}
