package org.intellivim.core.command.test;

import com.intellij.execution.testframework.sm.runner.events.TestFailedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestIgnoredEvent;
import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent;
import com.intellij.execution.testframework.sm.runner.events.TestStartedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteStartedEvent;
import com.intellij.openapi.util.Key;
import org.intellivim.core.command.run.AsyncRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dhleong
 */
public interface AsyncTestRunner extends AsyncRunner {

    public void onStartTesting();

    public void onTestsCountInSuite(final int count);

    public void onTestStarted(@NotNull final TestStartedEvent testStartedEvent);

    public void onTestFinished(@NotNull final TestFinishedEvent testFinishedEvent);

    public void onTestFailure(@NotNull final TestFailedEvent testFailedEvent);

    public void onTestIgnored(@NotNull final TestIgnoredEvent testIgnoredEvent);

    public void onTestOutput(@NotNull final TestOutputEvent testOutputEvent);

    public void onSuiteStarted(@NotNull final TestSuiteStartedEvent suiteStartedEvent);

    public void onSuiteFinished(@NotNull final TestSuiteFinishedEvent suiteFinishedEvent);

    public void onUncapturedOutput(@NotNull final String text, final Key outputType);

    public void onError(@NotNull final String localizedMessage,
            @Nullable final String stackTrace,
            final boolean isCritical);

    public void onFinishTesting();

}
