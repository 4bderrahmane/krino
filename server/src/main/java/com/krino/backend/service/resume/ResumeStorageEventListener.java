package com.krino.backend.service.resume;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ResumeStorageEventListener {

    private final ResumeStorageService resumeStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void deleteRolledBackResume(ResumeStoredEvent event) {
        resumeStorageService.deleteResumeBestEffort(event.objectKey());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deleteCommittedResume(ResumeDeletionRequestedEvent event) {
        resumeStorageService.deleteResumeBestEffort(event.objectKey());
    }
}
