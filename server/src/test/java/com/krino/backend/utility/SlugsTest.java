package com.krino.backend.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SlugsTest {
    @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
    @CsvSource({
            "Java,                  java",
            "Data Science,          data-science",
            "Spring Boot,           spring-boot",

            "Node.js,               node-js",
            "Node.js / React,       node-js-react",
            "--Hello--,             hello",
            "a___b,                 a-b",

            "C++,                   c-plus-plus",
            "C#,                    c-sharp",
            "F#,                    f-sharp",

            "Café,                  cafe",
            "Crème brûlée,          creme-brulee",

            "Bjørn,                 bjorn",
            "Łódź,                  lodz",
            "Straße,                strasse",

            "ﬁle,                   file",
            "Level ①,               level-1"
    })
    void slugify_transformsNameIntoStableSlug(String input, String expected) {
        assertThat(Slugs.slugify(input)).isEqualTo(expected);
    }

    @Test
    void slugify_distinguishesPlusAndSharpVariants() {
        assertThat(Slugs.slugify("C++"))
                .isNotEqualTo(Slugs.slugify("C#"));
    }

    @ParameterizedTest(name = "[{index}] no searchable characters in \"{0}\"")
    @CsvSource({
            "'!!!'",
            "'   '",
            "'---'",
            "'中文'",
            "'❤'"
    })
    void slugify_throwsWhenNothingSearchableRemains(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Slugs.slugify(input))
                .withMessageContaining("searchable");
    }

    @Test
    void slugify_rejectsNullInput() {
        assertThatNullPointerException()
                .isThrownBy(() -> Slugs.slugify(null))
                .withMessageContaining("input");
    }
}
