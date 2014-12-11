package org.intellivim.java.command;

import com.intellij.execution.Executor;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.Command;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.core.command.test.AbstractRunTestCommand;
import org.intellivim.core.util.BuildUtil;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;

/**
 * @author dhleong
 */
@Command("junit")
public class JUnitRunTestCommand extends AbstractRunTestCommand {

    @Required @Inject PsiFile file;
    @Required int offset;

    public JUnitRunTestCommand(final Project project, String file) {
        super(project);

        this.file = ProjectUtil.getPsiFile(project, file);
    }

    @Override
    public Result execute() {

        System.out.println("Config2=" +
                BuildUtil.createConfiguration(project, file, offset));

        System.out.println("Config=" +
                BuildUtil.findConfigurationFor(project, file, offset));

        return super.execute();
    }

    @Override
    protected String getTestFrameworkName() {
        return "JUnit";
    }

    @Override
    protected TestConsoleProperties createProperties(final Project project,
            final Executor executor) {
//        return new JUnitConsoleProperties();
        return null;
    }
}
