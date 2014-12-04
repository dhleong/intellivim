package org.intellivim.core.util;

import junit.framework.TestCase;
import org.intellivim.BaseTestCase;

import java.util.concurrent.TimeoutException;

import static org.intellivim.IVAssertions.assertThat;


/**
 * @author dhleong
 */
public class ExternalRunnerTest extends BaseTestCase {

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testEcho() {
        ExternalRunner runner = ExternalRunner.run("echo", "echo chamber");
        assertThat(runner)
                .isSuccess()
                .hasStdOut("echo chamber");
    }

    public void testTimeout() {
        ExternalRunner runner = ExternalRunner.run(10, "sleep", "500");
        assertThat(runner)
                .isNotSuccess()
                .hasErrorOfType(TimeoutException.class);
    }

    public void testError() {
        ExternalRunner runner = ExternalRunner.run("sleep", "jigglypuff", "sleep");
        assertThat(runner)
                .isNotSuccess()
                .hasNoError()
                .hasExitCode(1);
    }
}
