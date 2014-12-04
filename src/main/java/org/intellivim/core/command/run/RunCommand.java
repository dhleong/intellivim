package org.intellivim.core.command.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.IntelliVimUtil;
import org.intellivim.inject.Inject;
import org.intellivim.inject.UnsupportedClientException;

/**
 * @author dhleong
 */
@Command("run")
public class RunCommand extends ProjectCommand {

    @Required @Inject AsyncRunner asyncRunner;

    /* optional */String configuration;

    public RunCommand(Project project, AsyncRunner runner) {
        super(project);

        asyncRunner = runner;
    }

    /** for testing */
    public AsyncRunner getRunner() {
        return asyncRunner;
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

        System.out.println("Use setting " + setting);
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        final ProgramRunner runner = pickRunner(setting, executor);
        final ExecutionEnvironment env;
        final RunProfileState state;
        try {
            env = prepareEnvironment(project, setting, runner, executor);
            state = getRunProfileState(setting, executor, env);
        } catch (ExecutionException e) {
            return SimpleResult.error(e);
        }

        final String launchId = pickLaunchId(setting);
        final Runnable startRunnable = new Runnable() {
            @Override
            public void run() {
                // run in unit test mode so it doesn't try to do dumb stuff with the UI
                try {
                    IntelliVimUtil.setUnitTestMode();
                    execute(launchId, runner, env);
                } catch (ExecutionException e) {
                    // TODO do something with this
                    e.printStackTrace();
                } finally {
                    IntelliVimUtil.unsetUnitTestMode();
                }
            }
        };

        try {
            // make sure we can do it
            asyncRunner.prepare(launchId);
        } catch (UnsupportedClientException e) {
            System.err.println(e.getMessage());
            LaunchManager.terminate(launchId);
            return SimpleResult.error(e);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            LaunchManager.terminate(launchId);
            return SimpleResult.error(e);
        }

        if (IntelliVimUtil.isUnitTestMode()) {
            startRunnable.run();
        } else {
            final ExecutionManager mgr = ExecutionManager.getInstance(project);
            mgr.compileAndRun(startRunnable,
                    env, state,
                    new Runnable() {
                @Override
                public void run() {
                    System.out.println("CANCEL");
                    asyncRunner.cancel();
                }
            });
        }

        return SimpleResult.success();
    }

    private String pickLaunchId(RunnerAndConfigurationSettings setting) {
        return LaunchManager.allocateId(project, setting);
    }

    void execute(final String launchId, ProgramRunner runner, ExecutionEnvironment env)
            throws ExecutionException {
        // borrowed from ExecutionManagerImpl so we can get to the RunContentDescriptor
        runner.execute(env, new ProgramRunner.Callback() {
            @Override
            public void processStarted(final RunContentDescriptor descriptor) {
                System.out.println("Started!" + descriptor);
                final ProcessHandler handler = descriptor.getProcessHandler();
                if (handler == null) {
                    System.out.println("NO HANDLER!"); // what would this even mean?
                    return;
                }

                System.out.println("Process: " + handler);
                if (handler.isProcessTerminated() || handler.isProcessTerminating()) {
                    descriptor.dispose();
                    System.err.println("Process was already terminated");
                    return;
                }

                LaunchManager.register(launchId, handler);

                handler.addProcessListener(new ProcessAdapter() {

                    @Override
                    public void processTerminated(ProcessEvent event) {
                        // everybody do your share
                        descriptor.dispose();

                        asyncRunner.terminate();
                    }

                    @Override
                    public void onTextAvailable(ProcessEvent event, Key outputType) {
                        final AsyncRunner.OutputType type =
                                AsyncRunner.OutputType.from(outputType);
                        System.out.println(type + "> " + event.getText().trim());
                        if (type != null)
                            asyncRunner.sendLine(type, event.getText().trim());

                    }
                });
            }
        });
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

    static ProgramRunner pickRunner(RunnerAndConfigurationSettings setting, Executor executor) {
        return ProgramRunnerUtil.getRunner(executor.getId(), setting);
    }

    /** Just a little bit of convenience */
    static ExecutionEnvironment prepareEnvironment(Project project,
            RunnerAndConfigurationSettings setting,
            ProgramRunner runner, Executor executor)
            throws ExecutionException {
        ExecutionTarget target = ExecutionTargetManager.getActiveTarget(project);
        return prepareEnvironment(project, setting, runner, target, executor);
    }

    /**
     * Extracted from ProgramRunnerUtil
     * @throws ExecutionException
     */
    static ExecutionEnvironment prepareEnvironment(final Project project,
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

        // FINALLY the thing we came here to get (what is this, Costco?)
//        return configuration.getConfiguration().getState(executor, env);
        return getExecutionEnvironment(project, configuration, runner, target, executor);
    }

    static RunProfileState getRunProfileState(RunnerAndConfigurationSettings settings,
            Executor executor, ExecutionEnvironment env) throws ExecutionException {
        return settings.getConfiguration().getState(executor, env);
    }

    static ExecutionEnvironment getExecutionEnvironment(Project project,
            RunnerAndConfigurationSettings configuration, ProgramRunner runner,
            ExecutionTarget target, Executor executor) {
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
        return builder.build();
    }

}
