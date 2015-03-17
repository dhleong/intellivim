package org.intellivim.core.model;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.UserDataHolderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author dhleong
 */
public class VimCaretModel implements CaretModel {

    private VimEditor editor;
    private Document doc;
    private LogicalPosition logicalPosition;

    public VimCaretModel(VimEditor editor, Document doc, int offset) {
        this.editor = editor;
        this.doc = doc;

        moveToOffset(offset);
    }

    @Override
    public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean blockSelection, boolean scrollToCaret) {
        System.out.println(" *\n * MOVE RELATIVELY\n *");
    }

    @Override
    public void moveToLogicalPosition(@NotNull LogicalPosition pos) {
        logicalPosition = pos;
    }

    @Override
    public void moveToVisualPosition(@NotNull VisualPosition pos) {
        System.out.println(" *\n * MOVE VISUAL\n *");
    }

    @Override
    public void moveToOffset(int offset) {
        final int line = doc.getLineNumber(offset);
        final int col = offset - doc.getLineStartOffset(line);
        moveToLogicalPosition(new LogicalPosition(line, col));
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
//        System.out.println("  >> " + this + ".getOffset ->" + logicalPosition);
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
        return new VimCaret(this, editor, getOffset());
    }

    @NotNull
    @Override
    public Caret getPrimaryCaret() {
        return getCurrentCaret();
    }

    @Override
    public int getCaretCount() {
        return 1;
    }

    @NotNull
    @Override
    public List<Caret> getAllCarets() {
        return Arrays.asList(getCurrentCaret());
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

    @Override
    public void setCaretsAndSelections(@NotNull final List<CaretState> list,
            final boolean b) {

    }

    @NotNull
    @Override
    public List<CaretState> getCaretsAndSelections() {
        return null;
    }

    @Override
    public void runForEachCaret(@NotNull CaretAction caretAction) {
        caretAction.perform(getCurrentCaret());
    }

    @Override
    public void runForEachCaret(@NotNull final CaretAction caretAction, final boolean b) {

    }

    @Override
    public void runBatchCaretOperation(@NotNull Runnable runnable) {

    }

    private class VimCaret extends UserDataHolderBase implements Caret {

        private VimCaretModel model;
        private final Editor editor;
        private final int offset;

        private VimCaret(VimCaretModel model, final Editor editor, final int offset) {
            this.model = model;
            this.editor = editor;
            this.offset = offset;
        }

        @NotNull
        @Override
        public Editor getEditor() {
            return editor;
        }

        @NotNull
        @Override
        public CaretModel getCaretModel() {
            return model;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean scrollToCaret) {

        }

        @Override
        public void moveToLogicalPosition(@NotNull LogicalPosition pos) {

        }

        @Override
        public void moveToVisualPosition(@NotNull VisualPosition pos) {

        }

        @Override
        public void moveToOffset(int offset) {

        }

        @Override
        public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {

        }

        @Override
        public boolean isUpToDate() {
            return false;
        }

        @NotNull
        @Override
        public LogicalPosition getLogicalPosition() {
            return null;
        }

        @NotNull
        @Override
        public VisualPosition getVisualPosition() {
            return null;
        }

        @Override
        public int getOffset() {
            return offset;
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
        public int getSelectionStart() {
            return 0;
        }

        @NotNull
        @Override
        public VisualPosition getSelectionStartPosition() {
            return null;
        }

        @Override
        public int getSelectionEnd() {
            return 0;
        }

        @NotNull
        @Override
        public VisualPosition getSelectionEndPosition() {
            return null;
        }

        @Nullable
        @Override
        public String getSelectedText() {
            return null;
        }

        @Override
        public int getLeadSelectionOffset() {
            return 0;
        }

        @NotNull
        @Override
        public VisualPosition getLeadSelectionPosition() {
            return null;
        }

        @Override
        public boolean hasSelection() {
            return false;
        }

        @Override
        public void setSelection(int startOffset, int endOffset) {

        }

        @Override
        public void setSelection(int startOffset, int endOffset, boolean updateSystemSelection) {

        }

        @Override
        public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {

        }

        @Override
        public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {

        }

        @Override
        public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset, boolean updateSystemSelection) {

        }

        @Override
        public void removeSelection() {

        }

        @Override
        public void selectLineAtCaret() {

        }

        @Override
        public void selectWordAtCaret(boolean honorCamelWordsSettings) {

        }

        @Nullable
        @Override
        public Caret clone(boolean above) {
            return null;
        }

        @Override
        public void dispose() {

        }
    }
}
