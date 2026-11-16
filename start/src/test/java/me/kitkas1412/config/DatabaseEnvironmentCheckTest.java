package me.kitkas1412.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DatabaseEnvironmentCheckTest {

    private final DatabaseEnvironmentCheck check = new DatabaseEnvironmentCheck();

    private void run(MockEnvironment env) {
        check.postProcessEnvironment(env, null);
    }

    @Test
    void prodWithAllMissingListsAllThreeNames() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> run(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("profile 'prod'")
                .hasMessageContaining("TIXORA_DB_URL, TIXORA_DB_USER, TIXORA_DB_PASSWORD");
    }

    @Test
    void devWithOnlyPasswordMissingListsOnlyPassword() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("TIXORA_DB_URL", "jdbc:sentinel")
                .withProperty("TIXORA_DB_USER", "u");
        env.setActiveProfiles("dev");

        assertThatThrownBy(() -> run(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("profile 'dev'")
                .hasMessageContaining("TIXORA_DB_PASSWORD")
                .hasMessageNotContaining("TIXORA_DB_URL")
                .hasMessageNotContaining("TIXORA_DB_USER");
    }

    @Test
    void blankValueCountsAsMissing() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("TIXORA_DB_URL", "jdbc:sentinel")
                .withProperty("TIXORA_DB_USER", "   ")
                .withProperty("TIXORA_DB_PASSWORD", "pw");
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> run(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TIXORA_DB_USER")
                .hasMessageNotContaining("TIXORA_DB_URL")
                .hasMessageNotContaining("TIXORA_DB_PASSWORD");
    }

    @Test
    void allPresentDoesNotThrow() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("TIXORA_DB_URL", "jdbc:x")
                .withProperty("TIXORA_DB_USER", "u")
                .withProperty("TIXORA_DB_PASSWORD", "p");
        env.setActiveProfiles("dev", "prod");

        assertThatCode(() -> run(env)).doesNotThrowAnyException();
    }

    @Test
    void uncheckedProfilesDoNotThrowEvenWhenAllMissing() {
        for (String[] profiles : new String[][] {{"local"}, {"test"}, {}}) {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles(profiles);

            assertThatCode(() -> run(env)).doesNotThrowAnyException();
        }
    }

    @Test
    void messageNeverContainsProvidedValues() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("TIXORA_DB_URL", "jdbc:postgresql://SENTINEL-HOST/secretdb")
                .withProperty("TIXORA_DB_USER", "SENTINEL-USER");
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> run(env))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .doesNotContain("SENTINEL")
                        .doesNotContain("secretdb")
                        .contains("TIXORA_DB_PASSWORD"));
    }
}
