
package org.intellivim.core.model;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.impl.CaretModelImpl;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Created by dhleong on 11/4/14.
 */
public class VimEditor implements Editor {

    Document doc;
    private CaretModel caretModel;

    public VimEditor(PsiFile originalFile, int offset) {
        doc = new VimDocument(originalFile);
        caretModel = new VimCaretModel(offset);
        System.out.println("After caret: " + doc.getText(new TextRange(offset, offset + 10)));
    }

    @NotNull
    @Override
    public Document getDocument() {
        return doc;
    }

    @Override
    public boolean isViewer() {
        System.out.println(">> VimEditor.isViewer");
        return false;
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return null;
    }

    @NotNull
    @Override
    public JComponent getContentComponent() {
        return null;
    }

    @Override
    public void setBorder(@Nullable Border border) {

    }

    @Override
    public Insets getInsets() {
        return null;
    }

    @NotNull
    @Override
    public SelectionModel getSelectionModel() {
        return null;
    }

    @NotNull
    @Override
    public MarkupModel getMarkupModel() {
        return null;
    }

    @NotNull
    @Override
    public FoldingModel getFoldingModel() {
        return null;
    }

    @NotNull
    @Override
    public ScrollingModel getScrollingModel() {
        return null;
    }

    @NotNull
    @Override
    public CaretModel getCaretModel() {
        System.out.println(">> VimEditor.getCaretModel");
        return caretModel;
    }

    @NotNull
    @Override
    public SoftWrapModel getSoftWrapModel() {
        return null;
    }

    @NotNull
    @Override
    public EditorSettings getSettings() {
        System.out.println(">> VimEditor.getSettings");
        return null;
    }

    @NotNull
    @Override
    public EditorColorsScheme getColorsScheme() {
        return null;
    }

    @Override
    public int getLineHeight() {
        return 0;
    }

    @NotNull
    @Override
    public Point logicalPositionToXY(@NotNull LogicalPosition pos) {
        return null;
    }

    @Override
    public int logicalPositionToOffset(@NotNull LogicalPosition pos) {
        return 0;
    }

    @NotNull
    @Override
    public VisualPosition logicalToVisualPosition(@NotNull LogicalPosition logicalPos) {
        return null;
    }

    @NotNull
    @Override
    public Point visualPositionToXY(@NotNull VisualPosition visible) {
        return null;
    }

    @NotNull
    @Override
    public LogicalPosition visualToLogicalPosition(@NotNull VisualPosition visiblePos) {
        return null;
    }

    @NotNull
    @Override
    public LogicalPosition offsetToLogicalPosition(int offset) {
        return null;
    }

    @NotNull
    @Override
    public VisualPosition offsetToVisualPosition(int offset) {
        return null;
    }

    @NotNull
    @Override
    public LogicalPosition xyToLogicalPosition(@NotNull Point p) {
        return null;
    }

    @NotNull
    @Override
    public VisualPosition xyToVisualPosition(@NotNull Point p) {
        return null;
    }

    @Override
    public void addEditorMouseListener(@NotNull EditorMouseListener listener) {

    }

    @Override
    public void removeEditorMouseListener(@NotNull EditorMouseListener listener) {

    }

    @Override
    public void addEditorMouseMotionListener(@NotNull EditorMouseMotionListener listener) {

    }

    @Override
    public void removeEditorMouseMotionListener(@NotNull EditorMouseMotionListener listener) {

    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Nullable
    @Override
    public Project getProject() {
        System.out.println(">> VimEditor.getProject");
        return null;
    }

    @Override
    public boolean isInsertMode() {
        System.out.println(">> VimEditor.isInsertMode");
        return false;
    }

    @Override
    public boolean isColumnMode() {
        return false;
    }

    @Override
    public boolean isOneLineMode() {
        return false;
    }

    @NotNull
    @Override
    public EditorGutter getGutter() {
        return null;
    }

    @Nullable
    @Override
    public EditorMouseEventArea getMouseEventArea(@NotNull MouseEvent e) {
        return null;
    }

    @Override
    public void setHeaderComponent(@Nullable JComponent header) {

    }

    @Override
    public boolean hasHeaderComponent() {
        return false;
    }

    @Nullable
    @Override
    public JComponent getHeaderComponent() {
        return null;
    }

    @NotNull
    @Override
    public IndentsModel getIndentsModel() {
        return null;
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        System.out.println(">> VimEditor.getUserData " + key);
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

        System.out.println(">> VimEditor.putUserData");
    }
}
