package com.krino.backend.service.resume;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResumeStorageEventListenerTest {

    @Test
    void removesNewResumeAfterRollback() {
        ResumeStorageService resumeStorageService = mock(ResumeStorageService.class);
        ResumeStorageEventListener listener = new ResumeStorageEventListener(resumeStorageService);

        listener.deleteRolledBackResume(new ResumeStoredEvent("users/user-id/resume/cv.pdf"));

        verify(resumeStorageService).deleteResumeBestEffort("users/user-id/resume/cv.pdf");
    }

    @Test
    void removesObsoleteResumeAfterCommit() {
        ResumeStorageService resumeStorageService = mock(ResumeStorageService.class);
        ResumeStorageEventListener listener = new ResumeStorageEventListener(resumeStorageService);

        listener.deleteCommittedResume(
                new ResumeDeletionRequestedEvent("applications/application-id/resume/old.pdf"));

        verify(resumeStorageService)
                .deleteResumeBestEffort("applications/application-id/resume/old.pdf");
    }
}
