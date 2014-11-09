package org.intellivim;

/**
 * Created by dhleong on 11/7/14.
 */
public class SimpleResult implements Result {
    public final String error;
    public final Object result;

    SimpleResult(String error, Object result) {
        this.error = error;
        this.result = result;
    }

    @Override
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

    public static SimpleResult success() {
        return new SimpleResult(null, null);
    }
}
