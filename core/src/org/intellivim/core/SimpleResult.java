package org.intellivim.core;

/**
 * Created by dhleong on 11/7/14.
 */
public class SimpleResult {
    public final String error;
    public final Object result;

    SimpleResult(String error, Object result) {
        this.error = error;
        this.result = result;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public static SimpleResult error(String message) {
        return new SimpleResult(message, null);
    }

    public static SimpleResult error(Throwable error) {
        return new SimpleResult(error.getMessage(), null);
    }

    public static SimpleResult success(Object result) {
        return new SimpleResult(null, result);
    }
}
