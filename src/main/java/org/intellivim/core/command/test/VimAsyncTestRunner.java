package org.intellivim.core.command.test;

import com.intellij.execution.testframework.sm.runner.events.TestFailedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestIgnoredEvent;
import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent;
import com.intellij.execution.testframework.sm.runner.events.TestStartedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteStartedEvent;
import com.intellij.openapi.util.Key;
import org.intellivim.core.command.run.VimAsyncRunner;
import org.intellivim.inject.Client;
import org.intellivim.inject.ClientSpecific;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dhleong
 */
@ClientSpecific(Client.VIM)
public class VimAsyncTestRunner extends VimAsyncRunner implements AsyncTestRunner {

    private static final String PREPARE_COMMAND = "intellivim#core#test#onPrepareOutput";
    private static final String OUTPUT_COMMAND = "intellivim#core#test#onOutput";
    private static final String CANCEL_COMMAND =  "intellivim#core#test#onCancelled";
    private static final String TERMINATE_COMMAND =  "intellivim#core#test#onTerminated";

    public VimAsyncTestRunner() {
        super(PREPARE_COMMAND, OUTPUT_COMMAND, CANCEL_COMMAND, TERMINATE_COMMAND);
    }

    @Override
    public void onStartTesting() {
        // FIXME implement these
        System.out.println("Start testing!");
    }

    @Override
    public void onTestsCountInSuite(final int count) {

    }

    @Override
    public void onTestStarted(@NotNull final TestStartedEvent testStartedEvent) {
        System.out.println("STarted " + testStartedEvent);
    }

    @Override
    public void onTestFinished(@NotNull final TestFinishedEvent testFinishedEvent) {

    }

    @Override
    public void onTestFailure(@NotNull final TestFailedEvent testFailedEvent) {

    }

    @Override
    public void onTestIgnored(@NotNull final TestIgnoredEvent testIgnoredEvent) {

    }

    @Override
    public void onTestOutput(@NotNull final TestOutputEvent testOutputEvent) {

    }

    @Override
    public void onSuiteStarted(@NotNull final TestSuiteStartedEvent suiteStartedEvent) {

    }

    @Override
    public void onSuiteFinished(
            @NotNull final TestSuiteFinishedEvent suiteFinishedEvent) {

    }

    @Override
    public void onUncapturedOutput(@NotNull final String text, final Key outputType) {

    }

    @Override
    public void onError(@NotNull final String localizedMessage,
            @Nullable final String stackTrace, final boolean isCritical) {

    }

    @Override
    public void onFinishTesting() {
        System.out.println("Stop testing!");
    }
}
