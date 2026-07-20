package com.signal.global.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션 커밋 이전에 비동기 후속 작업(@Async 호출 등)이 시작되면, 그 작업이 아직 커밋되지 않은
 * 데이터를 조회하지 못해 결과가 조용히 유실될 수 있다. 이 유틸은 현재 트랜잭션이 커밋된 이후에
 * task가 실행되도록 보장한다. 트랜잭션이 없는 컨텍스트(단위 테스트 등)에서는 즉시 실행한다.
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    public static void runAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
