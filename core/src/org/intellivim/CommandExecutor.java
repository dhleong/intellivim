package org.intellivim;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by dhleong on 11/9/14.
 */
public class CommandExecutor {

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
            return false;
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
            System.out.println("Got result!");
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

        // we must execute on the event dispatcher thread
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {

            @Override
            public void run() {
                System.out.println("Running");

                try {
                    final ICommand command = gson.fromJson(json, ICommand.class);
                    if (command == null) {
                        result.setResult(SimpleResult.error("Invalid command"));
                        return;
                    }

                    System.err.println("GOT COMMAND " + command);
                    execute(result, command);

                } catch (JsonSyntaxException e) {
                    Throwable cause = e.getCause();
                    final Throwable actual = cause == null ? cause : e;
                    result.setResult(handleError(actual));
                } catch (Throwable e) {
                    result.setResult(handleError(e));
                }
            }
        }, ModalityState.any());

        System.out.println("Returning");
        return result;
    }

    protected void execute(final CommandResult result, final ICommand command) {
        if (command instanceof ProjectCommand) {
            Project project = ((ProjectCommand) command).getProject();

            System.out.println("Wait until smart");
            final DumbService dumbService = DumbService.getInstance(project);
            dumbService.runWhenSmart(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Smart!");
                    result.setResult(command.execute());
                }
            });
        } else {
            // just invoke via the application
            System.out.println("Invoke");
            result.setResult(command.execute());
        }
    }

    private static Result handleError(Throwable e) {
        e.printStackTrace();
        return SimpleResult.error(e);
    }

    /** Mostly for testing */
    public Result execute(String json) {
        try {
            System.out.println("Executing: " + json);
            return execute(new StringReader(json)).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to execute `" + json + "`", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Unable to execute `" + json + "`", e);
        }
    }
}
