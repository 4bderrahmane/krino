package com.krino.backend.dto.interview;

// This interface actually represents both of the DTOs that are permitted to implement
// this interface, and the service layer decides when to return each of em (see toPermittedView).
public sealed interface InterviewView permits InterviewResponseDTO, CandidateInterviewResponseDTO {
}
