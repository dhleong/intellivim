package org.intellivim.core.model;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by dhleong on 11/6/14.
 */
public class NullSelectionModel implements SelectionModel {

    @Override
    public int getSelectionStart() {
        return 0;
    }

    @Nullable
    @Override
    public VisualPosition getSelectionStartPosition() {
        return null;
    }

    @Override
    public int getSelectionEnd() {
        return 0;
    }

    @Nullable
    @Override
    public VisualPosition getSelectionEndPosition() {
        return null;
    }

    @Nullable
    @Override
    public String getSelectedText() {
        return null;
    }

    @Nullable
    @Override
    public String getSelectedText(boolean b) {
        return null;
    }

    @Override
    public int getLeadSelectionOffset() {
        return 0;
    }

    @Nullable
    @Override
    public VisualPosition getLeadSelectionPosition() {
        return null;
    }

    @Override
    public boolean hasSelection() {
        return false;
    }

    @Override
    public boolean hasSelection(boolean b) {
        return false;
    }

    @Override
    public void setSelection(int startOffset, int endOffset) {

    }

    @Override
    public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {

    }

    @Override
    public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {

    }

    @Override
    public void removeSelection() {

    }

    @Override
    public void removeSelection(boolean b) {

    }

    @Override
    public void addSelectionListener(SelectionListener listener) {

    }

    @Override
    public void removeSelectionListener(SelectionListener listener) {

    }

    @Override
    public void selectLineAtCaret() {

    }

    @Override
    public void selectWordAtCaret(boolean honorCamelWordsSettings) {

    }

    @Override
    public void copySelectionToClipboard() {

    }

    @Override
    public void setBlockSelection(@NotNull LogicalPosition blockStart, @NotNull LogicalPosition blockEnd) {

    }

    @Override
    public void removeBlockSelection() {

    }

    @Override
    public boolean hasBlockSelection() {
        return false;
    }

    @NotNull
    @Override
    public int[] getBlockSelectionStarts() {
        return new int[0];
    }

    @NotNull
    @Override
    public int[] getBlockSelectionEnds() {
        return new int[0];
    }

    @Nullable
    @Override
    public LogicalPosition getBlockStart() {
        return null;
    }

    @Nullable
    @Override
    public LogicalPosition getBlockEnd() {
        return null;
    }

    @Override
    public boolean isBlockSelectionGuarded() {
        return false;
    }

    @Nullable
    @Override
    public RangeMarker getBlockSelectionGuard() {
        return null;
    }

    @Override
    public TextAttributes getTextAttributes() {
        return null;
    }
}
