package com.sysm.devsync.infrastructure;

import com.sysm.devsync.infrastructure.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@PersistenceTest
public class AbstractRepositoryTest {

    @Autowired
    protected TestEntityManager entityManager;

    @Autowired
    protected QuestionJpaRepository questionJpaRepository;

    @Autowired
    protected ProjectJpaRepository projectJpaRepository;

    @Autowired
    protected WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    protected UserJpaRepository userJpaRepository;

    @Autowired
    protected TagJpaRepository tagJpaRepository;

    protected void clearRepositories() {
        questionJpaRepository.deleteAllInBatch();
        projectJpaRepository.deleteAllInBatch();
        workspaceJpaRepository.deleteAllInBatch();
        userJpaRepository.deleteAllInBatch();
        tagJpaRepository.deleteAllInBatch();
    }

}
