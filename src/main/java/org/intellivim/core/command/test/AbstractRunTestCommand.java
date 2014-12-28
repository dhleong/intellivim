package org.intellivim.core.command.test;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.runner.GeneralTestEventsProcessor;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.TestProxyPrinterProvider;
import com.intellij.execution.testframework.sm.runner.events.TestFailedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestIgnoredEvent;
import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent;
import com.intellij.execution.testframework.sm.runner.events.TestStartedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteStartedEvent;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.testIntegration.TestLocationProvider;
import org.apache.log4j.Level;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.run.AsyncRunner;
import org.intellivim.core.util.BuildUtil;
import org.intellivim.core.util.CompileAndRunner;
import org.intellivim.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common ancestor for commands that execute some sort of test case.
 * TODO Have a delegating command that picks the best one for the file type
 *
 * @author dhleong
 * @see com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
 */
public abstract class AbstractRunTestCommand extends ProjectCommand {

    @Required @Inject AsyncTestRunner asyncRunner;

    @Required @Inject protected PsiFile file;
    @Required protected int offset;


    public AbstractRunTestCommand(final Project project, AsyncTestRunner asyncRunner,
            PsiFile file, int offset) {
        super(project);

        this.asyncRunner = asyncRunner;
        this.file = file;
        this.offset = offset;
    }

    @Override
    public Result execute() {

        Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        TestConsoleProperties properties = createProperties(project, executor);
        if (properties == null) {
            return SimpleResult.error("Couldn't create properties for "
                    + getTestFrameworkName());
        }

        final ProcessorDelegate processor = new ProcessorDelegate(asyncRunner);
        final OutputToGeneralTestEventsConverter outputConsumer =
                new OutputToGeneralTestEventsConverter(
                    getTestFrameworkName(),
                    properties);
        outputConsumer.setProcessor(processor);

        RunConfiguration configuration =
                BuildUtil.createConfiguration(project, file, offset);
        if (configuration == null) {
            return SimpleResult.error("Could not create run configuration");
        }

        CompileAndRunner runner = CompileAndRunner.forProject(project)
                .usingConfiguration(configuration)
                .withExecutor(executor)
                .build();

        // FIXME asyncRunner.prepare()

        Logger.getInstance("#com.intellij.openapi.vfs.impl.local.FileWatcher").setLevel(Level.ALL);
        runner.addListener(new CompileAndRunner.Listener() {
            @Override
            public void onCompileComplete(final boolean aborted, final int errors,
                    final int warnings,
                    final CompileContext compileContext) {

                if (aborted || errors > 0) {
                    for (CompilerMessage msg : compileContext
                            .getMessages(CompilerMessageCategory.ERROR)) {

                        System.out.println(msg.getMessage());
                        asyncRunner.sendLine(AsyncRunner.OutputType.STDERR,
                                msg.getMessage());
                    }

                    asyncRunner.cancel();
                    return;
                }
            }

            @Override public void onCompileFailed() { }

            @Override public void onCompileSkipped() {
                System.out.println("Compile skipped!");
            }

            @Override
            public void onCompileSucceeded() { }

            @Override
            public void onProcessStarted(final RunContentDescriptor descriptor,
                    final ProcessHandler handler) {
                handleProcessStarted(descriptor, handler, outputConsumer, processor);
            }

            @Override
            public void onRunException(final ExecutionException e) {
                e.printStackTrace();
            }
        });

        try {
            runner.execute();
            System.out.println("Executed...");
            return SimpleResult.success();
        } catch (ExecutionException e) {
            return SimpleResult.error(e);
        }
    }

    protected abstract String getTestFrameworkName();

    /**
     * Ex:
     *  new PythonTRunnerConsoleProperties(myConfiguration, executor, false);
     *  new JUnitConsoleProperties(JUnitConfiguration configuration, executor);
     */
    protected abstract TestConsoleProperties createProperties(Project project,
            final Executor executor);

    protected void handleProcessStarted(final RunContentDescriptor descriptor,
            final ProcessHandler handler,
            final OutputToGeneralTestEventsConverter outputConsumer,
            final GeneralTestEventsProcessor processor) {

        handler.addProcessListener(new ProcessAdapter() {
            @Override
            public void startNotified(final ProcessEvent event) {
                processor.onStartTesting();
            }

            @Override
            public void processTerminated(final ProcessEvent event) {
                System.out.println("Terminated! ``" + event.getText() + "''");
                outputConsumer.flushBufferBeforeTerminating();
                processor.onFinishTesting();
                asyncRunner.terminate();

                Disposer.dispose(outputConsumer);
            }

            @Override
            public void onTextAvailable(final ProcessEvent event,
                    final Key outputType) {

                System.out.println("onTextAvailable! (" + outputType
                    + "): ``" + event.getText() + "''");
                outputConsumer.process(event.getText(), outputType);
            }
        });
    }

    /**
     * Delegate from the GeneralTestEventsProcessor directly to
     *  the AsyncTestRunner
     */
    private static class ProcessorDelegate extends GeneralTestEventsProcessor {

        final AsyncTestRunner runner;

        public ProcessorDelegate(final AsyncTestRunner runner) {
            this.runner = runner;
        }

        @Override
        public void onStartTesting() {
            runner.onStartTesting();
        }

        @Override
        public void onTestsCountInSuite(final int count) {
            runner.onTestsCountInSuite(count);
        }

        @Override
        public void onTestStarted(@NotNull final TestStartedEvent testStartedEvent) {
            runner.onTestStarted(testStartedEvent);
        }

        @Override
        public void onTestFinished(@NotNull final TestFinishedEvent testFinishedEvent) {
            runner.onTestFinished(testFinishedEvent);
        }

        @Override
        public void onTestFailure(@NotNull final TestFailedEvent testFailedEvent) {
            runner.onTestFailure(testFailedEvent);
        }

        @Override
        public void onTestIgnored(@NotNull final TestIgnoredEvent testIgnoredEvent) {
            runner.onTestIgnored(testIgnoredEvent);
        }

        @Override
        public void onTestOutput(@NotNull final TestOutputEvent testOutputEvent) {
            runner.onTestOutput(testOutputEvent);
        }

        @Override
        public void onSuiteStarted(
                @NotNull final TestSuiteStartedEvent suiteStartedEvent) {
            runner.onSuiteStarted(suiteStartedEvent);
        }

        @Override
        public void onSuiteFinished(
                @NotNull final TestSuiteFinishedEvent suiteFinishedEvent) {
            runner.onSuiteFinished(suiteFinishedEvent);
        }

        @Override
        public void onUncapturedOutput(@NotNull final String text, final Key outputType) {
            runner.onUncapturedOutput(text, outputType);
        }

        @Override
        public void onError(@NotNull final String localizedMessage,
                @Nullable final String stackTrace,
                final boolean isCritical) {
            runner.onError(localizedMessage, stackTrace, isCritical);
        }

        @Override
        public void onFinishTesting() {
            runner.onFinishTesting();
        }

        @Override
        public void onCustomProgressTestsCategory(@Nullable final String categoryName,
                final int testCount) {
            // ???
        }

        @Override
        public void onCustomProgressTestStarted() {
            // ???
        }

        @Override
        public void onCustomProgressTestFailed() {
            // ???
        }

        @Override
        public void onTestsReporterAttached() {
        }

        @Override
        public void setLocator(@NotNull final TestLocationProvider locator) {
        }

        @Override
        public void addEventsListener(@NotNull final SMTRunnerEventsListener viewer) {
        }

        @Override
        public void setPrinterProvider(
                @NotNull final TestProxyPrinterProvider printerProvider) {
        }
    }
}
