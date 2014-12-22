package org.intellivim.java.command;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit2.ui.properties.JUnitConsoleProperties;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.Command;
import org.intellivim.core.command.test.AbstractRunTestCommand;
import org.intellivim.core.command.test.AsyncTestRunner;
import org.intellivim.core.util.BuildUtil;

/**
 * @author dhleong
 */
@Command("junit")
public class JUnitRunTestCommand extends AbstractRunTestCommand {

    public JUnitRunTestCommand(final Project project,
           AsyncTestRunner runner, PsiFile file, int offset) {
        super(project, runner, file, offset);
    }

    @Override
    protected String getTestFrameworkName() {
        return "JUnit";
    }

    @Override
    protected TestConsoleProperties createProperties(final Project project,
            final Executor executor) {
        RunConfiguration configuration =
                BuildUtil.createConfiguration(project, file, offset);
        if (!(configuration instanceof JUnitConfiguration)) {
            System.err.println("Got " + configuration);
            return null;
        }

        JUnitConfiguration config = (JUnitConfiguration) configuration;
        return new JUnitConsoleProperties(config, executor);
    }
}
