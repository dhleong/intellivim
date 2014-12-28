package org.intellivim.core.command.test;

import org.intellivim.core.command.run.AsyncRunner;

/**
 * @author dhleong
 */
public interface AsyncTestRunner extends AsyncRunner {

    public void onStartTesting(TestNode testsRoot);

    public void onTestOutput(TestNode owner, String output, OutputType type);

    public void onTestStateChanged(TestNode node);

    public void onFinishTesting();

}
