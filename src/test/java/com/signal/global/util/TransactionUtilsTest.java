package com.signal.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionUtilsTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 트랜잭션이_없으면_즉시_실행한다() {
        List<String> calls = new ArrayList<>();

        TransactionUtils.runAfterCommit(() -> calls.add("run"));

        assertThat(calls).containsExactly("run");
    }

    @Test
    void 트랜잭션_안에서는_커밋_전에_실행하지_않고_커밋_후에_실행한다() {
        TransactionSynchronizationManager.initSynchronization();
        List<String> calls = new ArrayList<>();

        TransactionUtils.runAfterCommit(() -> calls.add("run"));

        assertThat(calls).isEmpty();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(calls).containsExactly("run");
    }

    @Test
    void 롤백되면_실행하지_않는다() {
        TransactionSynchronizationManager.initSynchronization();
        List<String> calls = new ArrayList<>();

        TransactionUtils.runAfterCommit(() -> calls.add("run"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(calls).isEmpty();
    }
}
