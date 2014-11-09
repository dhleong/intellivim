package org.intellivim;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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

    public Result execute(Reader json) {
        try {
            ICommand command = gson.fromJson(json, ICommand.class);
            if (command == null)
                return SimpleResult.error("Invalid command");

            return command.execute();
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
