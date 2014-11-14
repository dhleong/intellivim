package org.intellivim.core.model;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.ex.ScrollingModelEx;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Created by dhleong on 11/14/14.
 */
public class NullScrollingModel implements ScrollingModelEx {

    private final Document doc;

    public NullScrollingModel(Document doc) {
        this.doc = doc;
    }

    @Override
    public void accumulateViewportChanges() {

    }

    @Override
    public void flushViewportChanges() {

    }

    @NotNull
    @Override
    public Rectangle getVisibleArea() {
        // whatever; 1px per line/col
        return new Rectangle(80, doc.getLineCount());
    }

    @NotNull
    @Override
    public Rectangle getVisibleAreaOnScrollingFinished() {
        return getVisibleArea();
    }

    @Override
    public void scrollToCaret(@NotNull ScrollType scrollType) {

    }

    @Override
    public void scrollTo(@NotNull LogicalPosition pos, @NotNull ScrollType scrollType) {

    }

    @Override
    public void runActionOnScrollingFinished(@NotNull Runnable action) {

    }

    @Override
    public void disableAnimation() {

    }

    @Override
    public void enableAnimation() {

    }

    @Override
    public int getVerticalScrollOffset() {
        return 0;
    }

    @Override
    public int getHorizontalScrollOffset() {
        return 0;
    }

    @Override
    public void scrollVertically(int scrollOffset) {

    }

    @Override
    public void scrollHorizontally(int scrollOffset) {

    }

    @Override
    public void addVisibleAreaListener(@NotNull VisibleAreaListener listener) {

    }

    @Override
    public void removeVisibleAreaListener(@NotNull VisibleAreaListener listener) {

    }
}
