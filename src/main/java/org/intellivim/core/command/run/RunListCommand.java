package org.intellivim.core.command.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.project.Project;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

/**
 * List known run configurations for a project
 *
 * @author dhleong
 */
@Command("run_list")
public class RunListCommand extends ProjectCommand {

    public static class RunConfigInfo {
        public final String name, type;

        RunConfigInfo(RunnerAndConfigurationSettings config) {
            name = config.getName();
            type = config.getType().getDisplayName();
        }

        @Override
        public String toString() {
            return name + ":" + type;
        }
    }

    public RunListCommand(final Project project) {
        super(project);
    }

    @Override
    public Result execute() {
        final RunManager manager = RunManager.getInstance(project);
        return SimpleResult.success(ContainerUtil.map(manager.getAllSettings(),
                new Function<RunnerAndConfigurationSettings, RunConfigInfo>() {

            @Override
            public RunConfigInfo fun(
                    final RunnerAndConfigurationSettings config) {
                return new RunConfigInfo(config);
            }
        }));
    }
}
