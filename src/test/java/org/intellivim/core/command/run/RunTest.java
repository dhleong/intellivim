package org.intellivim.core.command.run;

import com.intellij.openapi.project.Project;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.intellivim.SimpleResult;
import org.intellivim.inject.UnsupportedClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class RunTest extends UsableSdkTestCase {

    @Override
    protected void invokeTestRunnable(final Runnable runnable) throws Exception {
        // DON'T run on Swing dispatch thread; some of the compile
        //  stuff wants to run there, and we'll never get the results
        //  if we do, too
        System.out.println("Invoke: " + runnable);
        runnable.run();
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    public void testRun() throws Exception {
        Project project = prepareProject(RUNNABLE_PROJECT);

        LoggingRunner runner = new LoggingRunner();
        SimpleResult result = (SimpleResult) new RunCommand(project, runner).execute();
        assertSuccess(result);

        if (!runner.awaitTermination(5000))
            fail("RunnableProject did not finish execution within 5s");

        // launch manager should clear it
        assertThat(LaunchManager.get(runner.launchId))
                .isNull();

        // make sure we got our output
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(runner.cancelled).as("Run Cancelled").isFalse();
        softly.assertThat(runner.stderr).as("stderr").contains("Standard Error");
        softly.assertThat(runner.stdout).as("stdout").contains("Standard Output");
        softly.assertThat(runner.system).as("system")
                .hasSize(2) // first one is the command line
                .contains("Process finished with exit code 0");
        softly.assertAll();
    }

    public void testCompileError() throws Exception {
        Project project = prepareProject(JAVA_PROJECT);

        LoggingRunner runner = new LoggingRunner();
        SimpleResult result = (SimpleResult) new RunCommand(project, runner).execute();
        assertSuccess(result);

        if (!runner.awaitTermination(5000))
            fail("RunnableProject did not finish execution within 5s");

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(runner.cancelled).as("Run Cancelled").isTrue();
        softly.assertThat(runner.stderr).as("stderr")
            .haveAtLeast(3, containing("cannot find symbol"))
            .haveAtLeastOne(containing("is not abstract and does not override"));
        softly.assertThat(runner.stdout).as("stdout").isEmpty();
        softly.assertThat(runner.system).as("system").isEmpty();
        softly.assertAll();
    }

    public void testTerminate() throws Exception {
        Project project = prepareProject(LOOPING_PROJECT);

        LoggingRunner runner = new LoggingRunner();
        SimpleResult result = (SimpleResult) new RunCommand(project, runner).execute();
        assertSuccess(result);

        if (!runner.awaitOutput(5000))
            fail("LoopingProject did not have stdout within 5s");

        // should still be there
        assertThat(LaunchManager.get(runner.launchId))
                .isNotNull();

        // make sure we got our output
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(runner.cancelled).as("Run Cancelled").isFalse();
        softly.assertThat(runner.stderr).as("stderr").isEmpty();
        softly.assertThat(runner.stdout).as("stdout").contains("Loop #1");
        softly.assertThat(runner.system).as("system")
                .doNotHave(containing("Process finished with exit code"));
        softly.assertAll();

        // terminate
        SimpleResult termination =
                (SimpleResult) new TerminateCommand(project, runner.launchId)
                        .execute();
        assertSuccess(termination);

        // gone, now
        assertThat(LaunchManager.get(runner.launchId))
                .isNull();

        if (!runner.awaitTermination(5000))
            fail("LoopingProject did not finish execution within 5s");

        assertThat(runner.system).as("system")
                .hasSize(2) // first one is the command line
                .haveExactly(1, containing("Process finished with exit code"));
    }

    private static Condition<String> containing(final String substring) {
        return new Condition<String>("contains substring `" + substring + "`") {
            @Override
            public boolean matches(final String s) {
                return s.contains(substring);
            }
        };
    }

    static class LoggingRunner implements AsyncRunner {

        List<String> stdout = new ArrayList<String>();
        List<String> stderr = new ArrayList<String>();
        List<String> system = new ArrayList<String>();

        String launchId;

        boolean cancelled = false;

        final Map<OutputType, List<String>> sink =
                new HashMap<OutputType, List<String>>();

        private Semaphore outputLock = new Semaphore(0);
        private Semaphore terminationLock = new Semaphore(0);

        LoggingRunner() {
            sink.put(OutputType.STDOUT, stdout);
            sink.put(OutputType.STDERR, stderr);
            sink.put(OutputType.SYSTEM, system);
        }

        @Override
        public void prepare(String launchId) throws UnsupportedClientException {
            this.launchId = launchId;
        }

        @Override
        public void sendLine(OutputType type, String line) {
            sink.get(type).add(line);

            if (type == OutputType.STDOUT)
                outputLock.release();
        }

        @Override
        public void cancel() {
            cancelled = true;
            terminationLock.release(999);
        }

        @Override
        public void terminate() {
            terminationLock.release(999);
        }

        boolean awaitTermination(long timeout) throws InterruptedException {
            return terminationLock.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        }

        boolean awaitOutput(long timeout) throws InterruptedException {
            return outputLock.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        }
    }
}
