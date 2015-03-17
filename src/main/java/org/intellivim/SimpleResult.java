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

    /**
     * Some commands may edit the text, and we would like to
     *  preserve the cursor position across such edits.
     *  You can use {@see VimEditor#createRangeMarker()} before
     *  performing the edit and pass it here after, then
     *  utilize the newOffset field of the result
     *
     * @return This object again, Builder-style
     */
    public SimpleResult withOffsetFrom(final RangeMarker marker) {
        if (marker != null) {
            final int editorOffset = marker.getStartOffset();
            System.out.println("marker=" + editorOffset);
            if (editorOffset > 0) {
                newOffset = editorOffset;
            }
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
