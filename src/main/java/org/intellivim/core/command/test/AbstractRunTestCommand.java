package org.intellivim.core.command.test;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.openapi.project.Project;
import org.intellivim.ProjectCommand;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

/**
 * Common ancestor for commands that execute some sort of test case
 *
 * @author dhleong
 * @see com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
 */
public abstract class AbstractRunTestCommand extends ProjectCommand {

    public AbstractRunTestCommand(final Project project) {
        super(project);
    }

    @Override
    public Result execute() {

        Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        TestConsoleProperties properties = createProperties(project, executor);
        if (properties == null) {
            return SimpleResult.error("Couldn't create properties for "
                    + getTestFrameworkName());
        }

        OutputToGeneralTestEventsConverter outputConsumer =
                new OutputToGeneralTestEventsConverter(
                    getTestFrameworkName(),
                    properties);

        return null;
    }

    protected abstract String getTestFrameworkName();

    /**
     * Ex:
     *  new PythonTRunnerConsoleProperties(myConfiguration, executor, false);
     *  new JUnitConsoleProperties(JUnitConfiguration configuration, executor);
     */
    protected abstract TestConsoleProperties createProperties(Project project,
            final Executor executor);
}
