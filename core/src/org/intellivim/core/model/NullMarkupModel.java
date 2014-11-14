package org.intellivim.core.model;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.DisposableIterator;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.SweepProcessor;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by dhleong on 11/14/14.
 */
public class NullMarkupModel extends UserDataHolderBase implements MarkupModelEx {

    private final Document doc;

    NullMarkupModel(Document doc) {
        this.doc = doc;
    }

    @Override
    public void dispose() {

    }

    @Nullable
    @Override
    public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        return null;
    }

    @Override
    public void fireAttributesChanged(@NotNull RangeHighlighterEx rangeHighlighterEx, boolean b) {

    }

    @Override
    public void fireAfterAdded(@NotNull RangeHighlighterEx segmentHighlighter) {

    }

    @Override
    public void fireBeforeRemoved(@NotNull RangeHighlighterEx segmentHighlighter) {

    }

    @Override
    public boolean containsHighlighter(@NotNull RangeHighlighter highlighter) {
        return false;
    }

    @Override
    public void addRangeHighlighter(RangeHighlighterEx marker, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {

    }

    @Override
    public void addMarkupModelListener(@NotNull Disposable parentDisposable, @NotNull MarkupModelListener listener) {

    }

    @Override
    public void setRangeHighlighterAttributes(@NotNull RangeHighlighter highlighter, @NotNull TextAttributes textAttributes) {

    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end, @NotNull Processor<? super RangeHighlighterEx> processor) {
        return false;
    }

    @Override
    public boolean processRangeHighlightersOutside(int start, int end, @NotNull Processor<? super RangeHighlighterEx> processor) {
        return false;
    }

    @NotNull
    @Override
    public DisposableIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
        return null;
    }

    @Override
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset, int endOffset, int layer, TextAttributes textAttributes, @NotNull HighlighterTargetArea targetArea, boolean isPersistent, Consumer<RangeHighlighterEx> changeAttributesAction) {
        return null;
    }

    @Override
    public void changeAttributesInBatch(@NotNull RangeHighlighterEx highlighter, @NotNull Consumer<RangeHighlighterEx> changeAttributesAction) {

    }

    @Override
    public boolean sweep(int start, int end, @NotNull SweepProcessor<RangeHighlighterEx> sweepProcessor) {
        return false;
    }

    @NotNull
    @Override
    public Document getDocument() {
        return doc;
    }

    @NotNull
    @Override
    public RangeHighlighter addRangeHighlighter(int startOffset, int endOffset, int layer, @Nullable TextAttributes textAttributes, @NotNull HighlighterTargetArea targetArea) {
        return null;
    }

    @NotNull
    @Override
    public RangeHighlighter addLineHighlighter(int line, int layer, @Nullable TextAttributes textAttributes) {
        return null;
    }

    @Override
    public void removeHighlighter(@NotNull RangeHighlighter rangeHighlighter) {

    }

    @Override
    public void removeAllHighlighters() {

    }

    @NotNull
    @Override
    public RangeHighlighter[] getAllHighlighters() {
        return new RangeHighlighter[0];
    }
}
