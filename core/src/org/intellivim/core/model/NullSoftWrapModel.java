package org.intellivim.core.model;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SoftWrap;
import com.intellij.openapi.editor.SoftWrapModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.SoftWrapChangeListener;
import com.intellij.openapi.editor.ex.SoftWrapModelEx;
import com.intellij.openapi.editor.impl.EditorTextRepresentationHelper;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapDrawingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dhleong on 11/6/14.
 */
public class NullSoftWrapModel implements SoftWrapModelEx {
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

    @NotNull
    @Override
    public LogicalPosition visualToLogicalPosition(@NotNull VisualPosition visual) {
        return null;
    }

    @NotNull
    @Override
    public LogicalPosition offsetToLogicalPosition(int offset) {
        return null;
    }

    @NotNull
    @Override
    public VisualPosition adjustVisualPosition(@NotNull LogicalPosition logical, @NotNull VisualPosition defaultVisual) {
        return null;
    }

    @Override
    public List<? extends SoftWrap> getRegisteredSoftWraps() {
        return null;
    }

    @Override
    public int getSoftWrapIndex(int offset) {
        return 0;
    }

    @Override
    public int paint(@NotNull Graphics g, @NotNull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
        return 0;
    }

    @Override
    public int getMinDrawingWidthInPixels(@NotNull SoftWrapDrawingType drawingType) {
        return 0;
    }

    @Override
    public boolean addSoftWrapChangeListener(@NotNull SoftWrapChangeListener listener) {
        return false;
    }

    @Override
    public boolean isRespectAdditionalColumns() {
        return false;
    }

    @Override
    public void forceAdditionalColumnsUsage() {

    }

}
