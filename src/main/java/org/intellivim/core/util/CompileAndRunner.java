package org.intellivim.core.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for compiling the project
 *  and executing a run configuration. Prepare
 *  with the Builder using #forProject()
 *
 *
 * @author dhleong
 */
public class CompileAndRunner {

    public static class Builder {
        private final Project project;
        private RunnerAndConfigurationSettings settings;
        private Executor executor = DefaultRunExecutor.getRunExecutorInstance();

        private Builder(Project project) {
            this.project = project;
        }

        public Builder usingConfigurationName(String configurationName) {
            this.settings = pickRunSetting(project, configurationName);
            return this;
        }

        public Builder usingConfiguration(RunConfiguration configuration) {
            this.settings = new RunnerAndConfigurationSettingsImpl(
                    RunManagerImpl.getInstanceImpl(project),
                    configuration,
                    false);
            return this;
        }

        /**
         * We use the DefaultRunExecutor by default.
         *  Provide something else here
         */
        public Builder withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public CompileAndRunner build() {
            return new CompileAndRunner(project, executor, settings);
        }
    }

    public interface Listener {
        public void onCompileComplete(boolean aborted, int errors, int warnings,
                final CompileContext compileContext);

        /**
         * Implied by having errors or aborted=true in onCompileComplete,
         *  but if you want a more discrete signal then here it is
         */
        void onCompileFailed();

        /**
         * Implied by having no errors and aborted=false in onCompileComplete,
         *  but if you want a more discrete signal then here it is
         */
        void onCompileSucceeded();

        /**
         * In some situations, we may not be able to compile before running.
         */
        void onCompileSkipped();

        /**
         * Called when we've successfully compiled and started running.
         *  The ProcessHandler is guaranteed to be non-null here
         */
        void onProcessStarted(RunContentDescriptor descriptor,
                final ProcessHandler handler);

        /**
         * Called if something went wrong trying to run the
         *  compiled program
         */
        void onRunException(ExecutionException e);
    }

    private final Project project;
    private final Executor executor;
    private final RunnerAndConfigurationSettings settings;

    private final List<Listener> listeners = new ArrayList<Listener>();

    private CompileAndRunner(final Project project, final Executor executor,
            final RunnerAndConfigurationSettings settings) {

        this.project = project;
        this.executor = executor;
        this.settings = settings;
    }

    public RunnerAndConfigurationSettings getSettings() {
        return settings;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Begin the compile and run process
     */
    public void execute() throws ExecutionException {
        final ProgramRunner runner = pickRunner(settings, executor);
        final ExecutionEnvironment env =
                prepareEnvironment(project, settings, runner, executor);

        final Runnable startRunnable = IntelliVimUtil.onSwingThread(
                new Runnable() {
                    @Override
                    public void run() {
                        // run in unit test mode so it doesn't try to do dumb stuff with the UI
                        try {
                            IntelliVimUtil.setUnitTestMode();
                            execute(runner, env);
                        } catch (ExecutionException e) {
                            for (Listener listener : listeners) {
                                listener.onRunException(e);
                            }
                        } finally {
                            IntelliVimUtil.unsetUnitTestMode();
                        }
                    }
                });

        final boolean startedCompile =
            BuildUtil.compileProject(project, settings.getConfiguration(),
                new CompileStatusNotification() {
                    @Override
                    public void finished(final boolean aborted, final int errors,
                            final int warnings,
                            final CompileContext compileContext) {
                        System.out.println("aborted=" + aborted +
                                        "errors=" + errors +
                                        "warnings=" + warnings
                        );

                        for (Listener listener : listeners) {
                            listener.onCompileComplete(
                                    aborted, errors, warnings, compileContext);
                        }

                        if (aborted || errors > 0) {
                            for (Listener listener : listeners) {
                                listener.onCompileFailed();
                            }
                            return;
                        }

                        for (Listener listener : listeners) {
                            listener.onCompileSucceeded();
                        }

                        startRunnable.run();
                    }
                });

        if (!startedCompile) {
            for (Listener listener : listeners) {
                listener.onCompileSkipped(); // ?!
            }

            startRunnable.run();
        }
    }

    void execute(final ProgramRunner runner, final ExecutionEnvironment env)
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

                for (Listener listener : listeners) {
                    listener.onProcessStarted(descriptor, handler);
                }

                handler.addProcessListener(new ProcessAdapter() {
                    @Override
                    public void processTerminated(ProcessEvent event) {
                        // this dispose whines if not on dispatch thread
                        UIUtil.invokeAndWaitIfNeeded(new Runnable() {

                            @Override
                            public void run() {
                                // everybody do your share
                                descriptor.dispose();
                            }
                        });
                    }
                });
            }
        });
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


    /**
     * Factory for the Builder interface
     */
    public static Builder forProject(@NotNull Project project) {
        return new Builder(project);
    }

    /** Public for testing */
    public static RunnerAndConfigurationSettings pickRunSetting(Project project,
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
}
