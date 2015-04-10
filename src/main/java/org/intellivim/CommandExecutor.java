package org.intellivim;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.intellivim.IVGson.RawCommand;
import org.intellivim.core.util.IntelliVimUtil;
import org.intellivim.core.util.Profiler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dhleong
 */
public class CommandExecutor {
    private static Logger logger = Logger.getLogger("IntelliVim:CommandExecutor");

    static class CommandResult implements Future<Result> {

        Result result;
        Semaphore semaphore = new Semaphore(0);

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return result != null;
        }

        @Override
        public Result get() throws InterruptedException, ExecutionException {
            try {
                return get(1, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                // hopefully won't happen
                e.printStackTrace();
                throw new RuntimeException("Command took a MINUTE to execute!", e);
            }
        }

        @Override
        public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (result != null)
                return result;

            semaphore.tryAcquire(timeout, unit);
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
            semaphore.release();
        }
    }

    final Gson gson;

    public CommandExecutor(Gson gson) {
        this.gson = gson;
    }

    public Future<Result> execute(InputStream json) {
        return execute(new InputStreamReader(json));
    }

    public Future<Result> execute(final Reader json) {
        final CommandResult result = new CommandResult();

        final Profiler profiler = Profiler.start(IVGson.RawCommand.class);
        final IVGson.RawCommand rawCommand = gson.fromJson(json, IVGson.RawCommand.class);
        profiler.mark("Gson Parsed");

        final Runnable execution = new Runnable() {

            @Override
            public void run() {

                try {
                    profiler.mark("Init'ing");
                    final ICommand command = rawCommand.init();
                    if (command == null) {
                        result.setResult(SimpleResult.error("Invalid command"));
                        return;
                    }
                    profiler.mark("Init'd");
                    profiler.switchContext(command);

                    execute(result, command);

                } catch (JsonSyntaxException e) {
                    Throwable cause = e.getCause();
                    final Throwable actual = cause == null ? cause : e;
                    result.setResult(handleError(rawCommand, actual));
                } catch (Throwable e) {
                    result.setResult(handleError(rawCommand, e));
                }
            }
        };

        // NB: we could make this a bit more granular if we need to....
        if (rawCommand.needsInitOnDispatch() || rawCommand.needsExecuteOnDispatch()) {
            // we must execute on the event dispatcher thread
            ApplicationManager.getApplication().invokeAndWait(
                    execution, ModalityState.any());
        } else {
            execution.run();
        }

        return result;
    }

    protected void execute(final CommandResult result,
            final ICommand command) {
        final Profiler profiler = Profiler.with(command);
        if (command instanceof ProjectCommand) {
            final Project project = ((ProjectCommand) command).getProject();

            final Runnable executeRunnable = IntelliVimUtil.whenSmart(project,
                    new Runnable() {
                @Override
                public void run() {
                    profiler.mark("runWhenSmart");
                    executeProjectCommand(project, command, result);
                }
            });

            if (IntelliVimUtil.isUnitTestMode()) {
                // in unit test mode, just run immediately; there will
                //  be no external edits, and the IntelliJ plugin junit
                //  stuff has a weird relationship with the dispatch thread
                executeRunnable.run();
            } else {
                // Invoke later so any delayed updates have a chance
                //  to run. This is important if we're editing a file
                //  that's open in IntelliJ. See issue #10
                ApplicationManager.getApplication().invokeLater(executeRunnable);
            }

        } else {
            // just invoke via the application
            result.setResult(command.execute());
            profiler.finish("normalExecute");
            dispose(command);
        }
    }

    private void executeProjectCommand(final Project project,
            final ICommand command, final CommandResult result) {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {

            @Override
            public void run() {
                final Profiler profiler = Profiler.with(command);
                try {
                    profiler.mark("preProjectExecute");
                    result.setResult(command.execute());
                    profiler.finish("projectExecute");
                } catch (Throwable e) {
                    logger.log(Level.WARNING,
                            "Unexpected error executing "
                                    + command.getClass().getSimpleName(),
                            e.fillInStackTrace());

                    if (!result.isDone()) {
                        result.setResult(SimpleResult.error(e));
                    }
                } finally {
                    dispose(command);
                }
            }
        }, "intellivim-command", "org.intellivim");
    }

    static void dispose(final ICommand command) {
        if (command instanceof Disposable) {
            Disposer.dispose((Disposable) command);
        }
    }

    private static Result handleError(final RawCommand rawCommand, Throwable e) {
        logger.log(Level.WARNING, "Error parsing: " + rawCommand.obj, e);
        return SimpleResult.error(e);
    }

    /** Mostly for testing */
    public Result execute(String json) {
        try {
            return execute(new StringReader(json)).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to execute `" + json + "`", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Unable to execute `" + json + "`", e);
        }
    }
}
