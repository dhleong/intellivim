package org.intellivim.core.command.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.CompileAndRunner;
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

        final CompileAndRunner runner = CompileAndRunner.forProject(project)
                .usingConfigurationName(configuration)
                .build();
        final String launchId = runner.allocateLaunchId();

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

        // attach our listener
        runner.addListener(new CompileAndRunner.Listener() {

            @Override
            public void onCompileComplete(final boolean aborted, final int errors,
                    final int warnings,
                    final CompileContext compileContext) {
                if (aborted || errors > 0) {
                    for (CompilerMessage msg : compileContext
                            .getMessages(CompilerMessageCategory.ERROR)) {

                        System.out.println(msg.getMessage());
                        asyncRunner.sendLine(AsyncRunner.OutputType.STDERR,
                                msg.getMessage());
                    }

                    asyncRunner.cancel();
                    return;
                }

            }

            @Override public void onCompileFailed() { }

            @Override public void onCompileSucceeded() { }

            @Override public void onCompileSkipped() { /* TODO ? */ }

            @Override
            public void onProcessStarted(final RunContentDescriptor descriptor,
                    ProcessHandler handler) {

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
//                        System.out.println(type + "> " + event.getText().trim());
                        if (type != null)
                            asyncRunner.sendLine(type, event.getText().trim());

                    }
                });
            }

            @Override
            public void onRunException(final ExecutionException e) {
                e.printStackTrace(); // TODO ?
            }
        });

        try {
            runner.execute();
            return SimpleResult.success();
        } catch (ExecutionException e) {
            return SimpleResult.error(e);
        }
    }

}
