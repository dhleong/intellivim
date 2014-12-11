package org.intellivim.core.util;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

/**
 * @author dhleong
 */
public class ExternalRunnerAssert
        extends AbstractAssert<ExternalRunnerAssert, ExternalRunner> {

    public ExternalRunnerAssert(ExternalRunner actual) {
        super(actual, ExternalRunnerAssert.class);
    }

    // 3 - A fluent entry point to your specific assertion class, use it with static import.
    public static ExternalRunnerAssert assertThat(ExternalRunner actual) {
        return new ExternalRunnerAssert(actual);
    }

    public ExternalRunnerAssert isSuccess() {
        if (!actual.isSuccess()) {
            failWithMessage("Expected success, but:\n" +
                    " - interrupted: <%s>\n" +
                    " - stdout: <%s>\n" +
                    " - stderr: <%s>\n" +
                    " - error: <%s>\n" +
                    " - exit: <%d>",
                    actual.isInterrupted(),
                    actual.getStdOut(),
                    actual.getStdErr(),
                    actual.getError(),
                    actual.getExitValue());
        }
        return this;
    }

    public ExternalRunnerAssert isNotSuccess() {
        if (actual.isSuccess())
            failWithMessage("Expected not successful, but was");
        return this;
    }

    public ExternalRunnerAssert hasStdOut(String expected) {
        Assertions.assertThat(actual.getStdOut())
                .contains(expected);
        return this;
    }

    public ExternalRunnerAssert hasStdErr(String expected) {
        Assertions.assertThat(actual.getStdErr())
                .contains(expected);
        return this;
    }

    public ExternalRunnerAssert hasErrorOfType(Class<?> exceptionType) {
        Assertions.assertThat(actual.getError())
                .describedAs("Runner Error")
                .isNotNull()
                .isInstanceOf(exceptionType);
        return this;
    }

    public ExternalRunnerAssert hasNoError() {
        Assertions.assertThat(actual.getError())
                .describedAs("Runner Error")
                .isNull();
        return this;
    }

    public ExternalRunnerAssert hasExitCode(int exitCode) {
        Assertions.assertThat(actual.getExitValue())
                .describedAs("Runner ExitCode")
                .isEqualTo(exitCode);
        return this;
    }
}
