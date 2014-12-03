package org.intellivim.core.util;

import junit.framework.TestCase;

import java.util.concurrent.TimeoutException;

import static org.intellivim.IVAssertions.assertThat;


/**
 * @author dhleong
 */
public class ExternalRunnerTest extends TestCase {

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
