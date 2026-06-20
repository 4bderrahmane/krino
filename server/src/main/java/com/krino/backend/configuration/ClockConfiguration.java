package com.krino.backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfiguration {

    /**
     * A single application Clock so time-dependent components (e.g. problem-detail
     * timestamps) can be pinned to a fixed instant in tests instead of reading the
     * wall clock directly. UTC keeps emitted timestamps unambiguous across hosts.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
