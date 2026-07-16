package com.krino.backend.service.resume;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RegistrationResumeRollbackListener {

    private final ResumeStorageService resumeStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRegistrationResumeStored(RegistrationResumeStoredEvent event) {
        resumeStorageService.deleteResumeBestEffort(event.objectKey());
    }
}
