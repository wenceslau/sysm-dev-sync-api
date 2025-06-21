package com.sysm.devsync.infrastructure;

import com.sysm.devsync.infrastructure.repositories.*;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static com.sysm.devsync.infrastructure.Utils.sleep;

@PersistenceTest
public class AbstractRepositoryTest {

    @Autowired
    protected TestEntityManager entityManager;

    @Autowired
    protected AnswerJpaRepository answerJpaRepository;

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
        answerJpaRepository.deleteAllInBatch();
        questionJpaRepository.deleteAllInBatch();
        projectJpaRepository.deleteAllInBatch();
        workspaceJpaRepository.deleteAllInBatch();
        userJpaRepository.deleteAllInBatch();
        tagJpaRepository.deleteAllInBatch();
    }

    protected void entityPersist(Object entity) {
        entityManager.persist(entity);
        flushAndClear();
        entityManager.detach(entity);
    }

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }


}
