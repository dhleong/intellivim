package org.intellivim.core.command.run;

import com.intellij.openapi.project.Project;
import org.intellivim.SimpleResult;
import org.intellivim.inject.UnsupportedClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        try {
            runner.awaitTermination(5000);
        } catch (InterruptedException e) {
            fail("RunnableProject did not finish execution within 5s");
        }

        // launch manager should clear it
        assertThat(LaunchManager.get("runnable-project:RunnableMain"))
                .isNull();

        // make sure we got our output
        assertThat(runner.stderr).contains("Standard Error");
        assertThat(runner.stdout).contains("Standard Output");
        assertThat(runner.system)
                .hasSize(2) // first one is the command line
                .contains("Process finished with exit code 0");
    }

    public void testCompileError() throws Exception {
        // NB not a proper test because compileAndRun refuses to
        //  work inside a unit test (for whatever reason)
        //  so we don't get the "cancelled" callback

        Project project = prepareProject(JAVA_PROJECT);

        LoggingRunner runner = new LoggingRunner();
        SimpleResult result = (SimpleResult) new RunCommand(project, runner).execute();
        assertSuccess(result);

        try {
            runner.awaitTermination(5000);
        } catch (InterruptedException e) {
            fail("RunnableProject did not finish execution within 5s");
        }

        assertThat(runner.stderr)
                .isNotEmpty();
//        assertThat(runner.system)
//                .contains("Process finished with exit code 1");
        assertThat(runner.cancelled)
                .describedAs("Build Cancelled")
                .isTrue();

    }

    // FIXME test termination of spin-looping projects

    static class LoggingRunner implements AsyncRunner {

        List<String> stdout = new ArrayList<String>();
        List<String> stderr = new ArrayList<String>();
        List<String> system = new ArrayList<String>();

        boolean cancelled = false;

        final Map<OutputType, List<String>> sink =
                new HashMap<OutputType, List<String>>();

        LoggingRunner() {
            sink.put(OutputType.STDOUT, stdout);
            sink.put(OutputType.STDERR, stderr);
            sink.put(OutputType.SYSTEM, system);
        }

        @Override
        public void prepare(String launchId) throws UnsupportedClientException {
        }

        @Override
        public void sendLine(OutputType type, String line) {
            sink.get(type).add(line);
        }

        @Override
        public synchronized void cancel() {
            cancelled = true;
            notify();
        }

        @Override
        public synchronized void terminate() {
            notify();
        }

        synchronized void awaitTermination(long timeout) throws InterruptedException {
            wait(timeout);
        }
    }
}
