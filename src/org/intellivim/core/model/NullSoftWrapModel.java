package org.intellivim.core.model;

import com.intellij.openapi.editor.SoftWrap;
import com.intellij.openapi.editor.SoftWrapModel;
import com.intellij.openapi.editor.VisualPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by dhleong on 11/6/14.
 */
public class NullSoftWrapModel implements SoftWrapModel {
    @Override
    public boolean isSoftWrappingEnabled() {
        return false;
    }

    @Nullable
    @Override
    public SoftWrap getSoftWrap(int offset) {
        return null;
    }

    @NotNull
    @Override
    public List<? extends SoftWrap> getSoftWrapsForRange(int start, int end) {
        return new LinkedList<SoftWrap>();
    }

    @NotNull
    @Override
    public List<? extends SoftWrap> getSoftWrapsForLine(int documentLine) {
        return new LinkedList<SoftWrap>();
    }

    @Override
    public boolean isVisible(SoftWrap softWrap) {
        return false;
    }

    @Override
    public void beforeDocumentChangeAtCaret() {

    }

    @Override
    public boolean isInsideSoftWrap(@NotNull VisualPosition position) {
        return false;
    }

    @Override
    public boolean isInsideOrBeforeSoftWrap(@NotNull VisualPosition visual) {
        return false;
    }

    @Override
    public void release() {

    }
}
