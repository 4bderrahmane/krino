package com.krino.backend.service.resume;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RegistrationResumeRollbackListenerTest {

    @Test
    void removesStoredResumeAfterRegistrationRollback() {
        ResumeStorageService resumeStorageService = mock(ResumeStorageService.class);
        RegistrationResumeRollbackListener listener =
                new RegistrationResumeRollbackListener(resumeStorageService);

        listener.onRegistrationResumeStored(
                new RegistrationResumeStoredEvent("users/user-id/resume/cv.pdf"));

        verify(resumeStorageService).deleteResumeBestEffort("users/user-id/resume/cv.pdf");
    }
}
