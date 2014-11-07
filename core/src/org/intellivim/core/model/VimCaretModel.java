package org.intellivim.core.model;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by dhleong on 11/5/14.
 */
public class VimCaretModel implements CaretModel {

    private Document doc;
    private int offset;
    private LogicalPosition logicalPosition;

    public VimCaretModel(Document doc, int offset) {
        this.doc = doc;
        this.offset = offset;

        int row = doc.getLineNumber(offset);
        int col = offset - doc.getLineStartOffset(row);
        moveToLogicalPosition(new LogicalPosition(row, col));
    }

    @Override
    public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean blockSelection, boolean scrollToCaret) {

    }

    @Override
    public void moveToLogicalPosition(@NotNull LogicalPosition pos) {
        logicalPosition = pos;
    }

    @Override
    public void moveToVisualPosition(@NotNull VisualPosition pos) {

    }

    @Override
    public void moveToOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
        moveToOffset(offset);
    }

    @Override
    public boolean isUpToDate() {
        return true;
    }

    @NotNull
    @Override
    public LogicalPosition getLogicalPosition() {
//        System.out.println("  >> VimCaretModel.getLogicalPosition: " + logicalPosition);
        return logicalPosition;
    }

    @NotNull
    @Override
    public VisualPosition getVisualPosition() {
        return null;
    }

    @Override
    public int getOffset() {
//        System.out.println("  >> VimCaretModel.getOffset");
        return doc.getLineStartOffset(logicalPosition.line) + logicalPosition.column;
    }

    @Override
    public void addCaretListener(@NotNull CaretListener listener) {

    }

    @Override
    public void removeCaretListener(@NotNull CaretListener listener) {

    }

    @Override
    public int getVisualLineStart() {
        return 0;
    }

    @Override
    public int getVisualLineEnd() {
        return 0;
    }

    @Override
    public TextAttributes getTextAttributes() {
        return null;
    }

    @Override
    public boolean supportsMultipleCarets() {
        return false;
    }

    @NotNull
    @Override
    public Caret getCurrentCaret() {
        return null;
    }

    @NotNull
    @Override
    public Caret getPrimaryCaret() {
        return null;
    }

    @Override
    public int getCaretCount() {
        return 0;
    }

    @NotNull
    @Override
    public List<Caret> getAllCarets() {
        return null;
    }

    @Nullable
    @Override
    public Caret getCaretAt(@NotNull VisualPosition visualPosition) {
        return null;
    }

    @Nullable
    @Override
    public Caret addCaret(@NotNull VisualPosition visualPosition) {
        return null;
    }

    @Override
    public boolean removeCaret(@NotNull Caret caret) {
        return false;
    }

    @Override
    public void removeSecondaryCarets() {

    }

    @Override
    public void setCaretsAndSelections(@NotNull List<CaretState> caretStates) {

    }

    @NotNull
    @Override
    public List<CaretState> getCaretsAndSelections() {
        return null;
    }

    @Override
    public void runForEachCaret(@NotNull CaretAction caretAction) {

    }

    @Override
    public void runBatchCaretOperation(@NotNull Runnable runnable) {

    }
}
