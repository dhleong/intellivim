package org.intellivim;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * Created by dhleong on 11/9/14.
 */
public class CommandExecutor {

    final Gson gson;

    public CommandExecutor(Gson gson) {
        this.gson = gson;
    }

    public Result execute(InputStream json) {
        return execute(new InputStreamReader(json));
    }

    public Result execute(final Reader json) {
        try {
            final Result[] result = new Result[1];

            // we must execute on the event dispatcher thread
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {

                @Override
                public void run() {

                    final ICommand command = gson.fromJson(json, ICommand.class);
                    if (command == null) {
                        result[0] = SimpleResult.error("Invalid command");
                        return;
                    }

                    result[0] = command.execute();
                }
            }, ModalityState.any());

            return result[0];
        } catch (JsonSyntaxException e) {
            Throwable cause = e.getCause();
            final Throwable actual = cause == null ? cause : e;
            return handleError(actual);
        } catch (Throwable e) {
            return handleError(e);
        }
    }

    private static Result handleError(Throwable e) {
        e.printStackTrace();
        return SimpleResult.error(e);
    }

    public Result execute(String json) {
        return execute(new StringReader(json));
    }
}
