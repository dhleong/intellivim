package org.intellivim;

import com.intellij.openapi.editor.RangeMarker;

/**
 * Convenient implementation of Result
 * @author dhleong
 */
public class SimpleResult implements Result {
    public final String error;
    public final Object result;

    private int newOffset;

    SimpleResult(String error, Object result) {
        this.error = error;
        this.result = result;
    }

    public int getNewOffset() {
        return newOffset;
    }

    /** Convenience method to avoid casting when you're sure of what it is */
    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
    }

    public SimpleResult withOffsetFrom(RangeMarker marker) {
        final int editorOffset = marker.getStartOffset();
        if (editorOffset > 0) {
            newOffset = editorOffset;
        }
        return this;
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
