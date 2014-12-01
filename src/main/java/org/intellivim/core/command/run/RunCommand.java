package org.intellivim.core.command.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl;
import com.intellij.openapi.util.text.StringUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.IntelliVimUtil;

/**
 * @author dhleong
 */
@Command("run")
public class RunCommand extends ProjectCommand {

    /* optional */String configuration;

    public RunCommand(Project project) {
        super(project);
    }

    @Override
    public Result execute() {

        final RunnerAndConfigurationSettings setting =
                pickRunSetting(project, configuration);
        if (setting == null) {
            final String error = "Unable to find run configuration";
            return SimpleResult.error(configuration == null
                    ? error
                    : error + ": " + configuration);
        }

        ApplicationConfiguration config =
                (ApplicationConfiguration) setting.getConfiguration();
        JavaRunConfigurationModule configurationModule = config.getConfigurationModule();
        ModuleRootManagerImpl root = (ModuleRootManagerImpl) ModuleRootManager
                .getInstance(configurationModule.getModule());
        if (root.getSdk() != null) {
            System.out.println(root.getSdk().getName() +"|" + root.getSdk().getSdkType().getName());
        }

        System.out.println("Use setting " + setting);
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        final RunProfileState state;
        try {
            state = executeConfiguration(project, setting, executor);
        } catch (ExecutionException e) {
            return SimpleResult.error(e);
        }

        System.out.println("Run!: " + state);

        return SimpleResult.success();
    }

    static RunnerAndConfigurationSettings pickRunSetting(Project project,
             String configuration) {

        final RunManager manager = RunManager.getInstance(project);

        // go with "last run"
        if (configuration == null) {
            RunnerAndConfigurationSettings selected = manager.getSelectedConfiguration();
            if (selected != null)
                return selected;
        }

        for (RunnerAndConfigurationSettings setting : manager.getAllSettings()) {
            if (configuration == null) {
                return setting; // just use the first we see, I guess

            } else if (configuration.equals(setting.getName())) {
                // found it!
                return setting;
            }
        }

        // couldn't find it :(
        return null;
    }

    /** Just a little bit of convenience */
    static RunProfileState executeConfiguration(Project project,
            RunnerAndConfigurationSettings setting, Executor executor)
            throws ExecutionException {
        ProgramRunner runner = ProgramRunnerUtil.getRunner(executor.getId(), setting);
        ExecutionTarget target = ExecutionTargetManager.getActiveTarget(project);
        return executeConfiguration(project, setting, runner, target, executor);
    }

    /**
     * Extracted from ProgramRunnerUtil so we can get access to the RunProfileState
     * @return
     * @throws ExecutionException
     */
    static RunProfileState executeConfiguration(final Project project,
            final RunnerAndConfigurationSettings configuration,
            final ProgramRunner runner, final ExecutionTarget target,
            final Executor executor)
            throws ExecutionException {
        if (ExecutorRegistry.getInstance().isStarting(project, executor.getId(),
                runner.getRunnerId())) {
            return null;
        }

        if (configuration != null &&
                !ExecutionTargetManager.canRun(configuration, target)) {
            throw new ExecutionException(StringUtil.escapeXml("Cannot run '" +
                        configuration.getName() + "' on '" +
                    target.getDisplayName() + "'"));
        }

        if (configuration != null &&
                (!RunManagerImpl.canRunConfiguration(configuration, executor))) {
            throw new IllegalArgumentException("Can't run `" + configuration + "`");
        }

        ExecutionEnvironmentBuilder builder =
                new ExecutionEnvironmentBuilder(project, executor);
        if (configuration != null) {
            builder.setRunnerAndSettings(runner, configuration);
        }
        else {
            builder.setRunnerId(runner.getRunnerId());
        }
        builder.setTarget(target).setContentToReuse(null).setDataContext(null);
        builder.assignNewId();
        final ExecutionEnvironment env = builder.build();

        // run in unit test mode so it doesn't try to do dumb stuff with the UI
        IntelliVimUtil.setUnitTestMode();
        runner.execute(env);
        IntelliVimUtil.unsetUnitTestMode();

        // FINALLY the thing we came here to get (what is this, Costco?)
        return configuration.getConfiguration().getState(executor, env);
    }
}
