package com.krino.backend.entity.enums;

/**
 * The hiring signal an interviewer leaves after a completed interview. Ordered from the
 * strongest endorsement to the strongest objection so it can drive the application decision.
 */
public enum InterviewRecommendation {
    STRONG_YES,
    YES,
    NO,
    STRONG_NO
}
