package org.intellivim.core.model;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.editor.ex.FoldingListener;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Created by dhleong on 11/14/14.
 */
public class NullFoldingModel implements FoldingModelEx {
    @Override
    public void setFoldingEnabled(boolean isEnabled) {

    }

    @Override
    public boolean isFoldingEnabled() {
        return false;
    }

    @Override
    public FoldRegion getFoldingPlaceholderAt(Point p) {
        return null;
    }

    @Override
    public boolean intersectsRegion(int startOffset, int endOffset) {
        return false;
    }

    @Override
    public FoldRegion fetchOutermost(int offset) {
        return null;
    }

    @Override
    public int getLastCollapsedRegionBefore(int offset) {
        return 0;
    }

    @Override
    public TextAttributes getPlaceholderAttributes() {
        return null;
    }

    @Override
    public FoldRegion[] fetchTopLevel() {
        return new FoldRegion[0];
    }

    @Nullable
    @Override
    public FoldRegion createFoldRegion(int startOffset, int endOffset, @NotNull String placeholder, @Nullable FoldingGroup group, boolean neverExpands) {
        return null;
    }

    @Override
    public void addListener(@NotNull FoldingListener foldingListener, @NotNull Disposable disposable) {

    }

    @Override
    public void clearFoldRegions() {

    }

    @Override
    public void rebuild() {

    }

    @Nullable
    @Override
    public FoldRegion addFoldRegion(int startOffset, int endOffset, @NotNull String placeholderText) {
        return null;
    }

    @Override
    public boolean addFoldRegion(@NotNull FoldRegion region) {
        return false;
    }

    @Override
    public void removeFoldRegion(@NotNull FoldRegion region) {

    }

    @NotNull
    @Override
    public FoldRegion[] getAllFoldRegions() {
        return new FoldRegion[0];
    }

    @Override
    public boolean isOffsetCollapsed(int offset) {
        return false;
    }

    @Nullable
    @Override
    public FoldRegion getCollapsedRegionAtOffset(int offset) {
        return null;
    }

    @Override
    public void runBatchFoldingOperation(@NotNull Runnable operation) {

    }

    @Override
    public void runBatchFoldingOperation(@NotNull Runnable operation, boolean moveCaretFromCollapsedRegion) {

    }

    @Override
    public void runBatchFoldingOperationDoNotCollapseCaret(@NotNull Runnable operation) {

    }
}
