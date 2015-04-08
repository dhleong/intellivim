
package org.intellivim.core.model;

import com.intellij.ide.CopyProvider;
import com.intellij.ide.CutProvider;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.IndentsModel;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.ScrollingModelEx;
import com.intellij.openapi.editor.ex.SoftWrapModelEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.ButtonlessScrollBarUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

/**
 * Created by dhleong on 11/4/14.
 */
public class VimEditor extends UserDataHolderBase implements EditorEx {

    private Project project;
    private PsiFile originalFile;

    private DocumentEx doc;
    private CaretModel caretModel;
    private SoftWrapModelEx softWrapModel;
    private SelectionModel selectionModel;
    private EditorHighlighter editorHighlighter;
    private ScrollingModelEx scrollingModel;
    private EditorSettings editorSettings;
    private MarkupModelEx markupModel;
    private IndentsModel indentsModel;
    private FoldingModelEx foldingModel;

    private JComponent component;

    public VimEditor(Project project, PsiFile originalFile, int offset) {
        this(project, originalFile, VimDocument.getInstance(originalFile), offset);
    }

    /** For TESTING only */
    public VimEditor(DocumentEx doc, int offset) {
        this(null, null, doc, offset);

        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            throw new IllegalStateException("This constructor is for TESTING only");
        }
    }

    private VimEditor(Project project, PsiFile originalFile, DocumentEx document, int offset) {
        this.project = project;
        this.originalFile = originalFile;
        doc = document;
        caretModel = new VimCaretModel(this, doc, offset);
        softWrapModel = new NullSoftWrapModel();
        selectionModel = new NullSelectionModel();
        editorHighlighter = new NullEditorHighlighter();
        scrollingModel = new NullScrollingModel(doc);
        editorSettings = new NullEditorSettings();
//        markupModel = new NullMarkupModel(doc);
        // normally I hate blind casting, but intellij does it ALL THE FREAKING TIME
        markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(doc, project, true);
        indentsModel = new NullIndentsModel();
        foldingModel = new NullFoldingModel();

        component = new JComponent() {
            @Override
            public FontMetrics getFontMetrics(Font font) {
                return new FontMetrics(font) {
                    @Override
                    public int charsWidth(char[] data, int off, int len) {
                        return 0;
                    }

                    @Override
                    public int stringWidth(String str) {
                        return 0;
                    }
                };
            }
        };
    }

    /** Convenience to create a RangeMarker based on the current cursor offset */
    public RangeMarker createRangeMarker() {
        return createRangeMarker(this);
    }

    @NotNull
    @Override
    public DocumentEx getDocument() {
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
        return component;
//        return null;
    }

    @NotNull
    @Override
    public JComponent getContentComponent() {
        return getComponent();
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
        return selectionModel;
    }

    @NotNull
    @Override
    public MarkupModelEx getMarkupModel() {
        return markupModel;
    }

    @NotNull
    @Override
    public EditorGutterComponentEx getGutterComponentEx() {
        return null;
    }

    @NotNull
    @Override
    public EditorHighlighter getHighlighter() {
        return editorHighlighter;
    }

    @Override
    public JComponent getPermanentHeaderComponent() {
        return null;
    }

    @Override
    public void setPermanentHeaderComponent(JComponent component) {

    }

    @Override
    public void setHighlighter(@NotNull EditorHighlighter highlighter) {

    }

    @Override
    public void setColorsScheme(@NotNull EditorColorsScheme scheme) {

    }

    @Override
    public void setInsertMode(boolean val) {

    }

    @Override
    public void setColumnMode(boolean val) {

    }

    @Override
    public void setLastColumnNumber(int val) {

    }

    @Override
    public int getLastColumnNumber() {
        return 0;
    }

    @Override
    public void setVerticalScrollbarOrientation(int type) {

    }

    @Override
    public int getVerticalScrollbarOrientation() {
        return 0;
    }

    @Override
    public void setVerticalScrollbarVisible(boolean b) {

    }

    @Override
    public void setHorizontalScrollbarVisible(boolean b) {

    }

    @Override
    public CutProvider getCutProvider() {
        return null;
    }

    @Override
    public CopyProvider getCopyProvider() {
        return null;
    }

    @Override
    public PasteProvider getPasteProvider() {
        return null;
    }

    @Override
    public DeleteProvider getDeleteProvider() {
        return null;
    }

    @Override
    public void repaint(int startOffset, int endOffset) {

    }

    @Override
    public void reinitSettings() {

    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public int getMaxWidthInRange(int startOffset, int endOffset) {
        return 0;
    }

    @Override
    public void stopOptimizedScrolling() {

    }

    @Override
    public boolean setCaretVisible(boolean b) {
        return false;
    }

    @Override
    public boolean setCaretEnabled(boolean enabled) {
        return false;
    }

    @Override
    public void addFocusListener(@NotNull FocusChangeListener listener) {

    }

    @Override
    public void addFocusListener(@NotNull FocusChangeListener listener, @NotNull Disposable parentDisposable) {

    }

    @Override
    public void setOneLineMode(boolean b) {

    }

    @NotNull
    @Override
    public JScrollPane getScrollPane() {
        return null;
    }

    @Override
    public boolean isRendererMode() {
        return false;
    }

    @Override
    public void setRendererMode(boolean isRendererMode) {

    }

    @Override
    public void setFile(VirtualFile vFile) {

    }

    @NotNull
    @Override
    public DataContext getDataContext() {
        return null;
    }

    @Override
    public boolean processKeyTyped(@NotNull KeyEvent e) {
        return false;
    }

    @Override
    public void setFontSize(int fontSize) {

    }

    @Override
    public Color getBackgroundColor() {
        return null;
    }

    @Override
    public void setBackgroundColor(Color color) {

    }

    @Override
    public Dimension getContentSize() {
        return null;
    }

    @Override
    public boolean isEmbeddedIntoDialogWrapper() {
        return false;
    }

    @Override
    public void setEmbeddedIntoDialogWrapper(boolean b) {

    }

    @Override
    public VirtualFile getVirtualFile() {
        return originalFile.getVirtualFile();
    }

    @Override
    public int calcColumnNumber(@NotNull CharSequence text, int start, int offset, int tabSize) {
        return 0;
    }

    @Override
    public int calcColumnNumber(int offset, int lineIndex) {
        return 0;
    }

    @Override
    public TextDrawingCallback getTextDrawingCallback() {
        return null;
    }

    @NotNull
    @Override
    public FoldingModelEx getFoldingModel() {
        return foldingModel;
    }

    @NotNull
    @Override
    public ScrollingModelEx getScrollingModel() {
        return scrollingModel;
    }

    @NotNull
    @Override
    public LogicalPosition visualToLogicalPosition(@NotNull VisualPosition visiblePos, boolean softWrapAware) {
        return null;
    }

    @NotNull
    @Override
    public LogicalPosition offsetToLogicalPosition(int offset, boolean softWrapAware) {
        return offsetToLogicalPosition(offset);
    }

    @NotNull
    @Override
    public VisualPosition logicalToVisualPosition(@NotNull LogicalPosition logicalPos, boolean softWrapAware) {
        return null;
    }

    @Override
    public int logicalPositionToOffset(@NotNull final LogicalPosition logicalPosition,
            final boolean b) {
        return logicalPositionToOffset(logicalPosition);
    }

    @NotNull
    @Override
    public EditorColorsScheme createBoundColorSchemeDelegate(@Nullable EditorColorsScheme customGlobalScheme) {
        return null;
    }

    @Override
    public void setSoftWrapAppliancePlace(@NotNull SoftWrapAppliancePlaces place) {

    }

    @Override
    public void setPlaceholder(@Nullable CharSequence text) {

    }

    @Override
    public void setShowPlaceholderWhenFocused(final boolean b) {

    }

    @Override
    public boolean isStickySelection() {
        return false;
    }

    @Override
    public void setStickySelection(boolean enable) {

    }

    @Override
    public int getPrefixTextWidthInPixels() {
        return 0;
    }

    @Override
    public void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes) {

    }

    @Override
    public boolean isPurePaintingMode() {
        return false;
    }

    @Override
    public void setPurePaintingMode(boolean enabled) {

    }

    @Override
    public void registerScrollBarRepaintCallback(@Nullable ButtonlessScrollBarUI.ScrollbarRepaintCallback scrollbarRepaintCallback) {

    }

    @NotNull
    @Override
    public CaretModel getCaretModel() {
        return caretModel;
    }

    @NotNull
    @Override
    public SoftWrapModelEx getSoftWrapModel() {
        return softWrapModel;
    }

    @NotNull
    @Override
    public EditorSettings getSettings() {
        return editorSettings;
    }

    @NotNull
    @Override
    public EditorColorsScheme getColorsScheme() {
        return new EditorColorsSchemeImpl(null);
    }

    @Override
    public int getLineHeight() {
        return 0;
    }

    @NotNull
    @Override
    public Point logicalPositionToXY(@NotNull LogicalPosition pos) {
        return new Point(pos.line, pos.column);
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
        int line = doc.getLineNumber(offset);
        int col = offset - doc.getLineStartOffset(line);
        return new LogicalPosition(line, col);
    }

    @NotNull
    @Override
    public VisualPosition offsetToVisualPosition(int offset) {
        return null;
    }

    @NotNull
    @Override
    public LogicalPosition xyToLogicalPosition(@NotNull Point p) {
        return new LogicalPosition(p.x, p.y); // ?!? (needed by VisibleHighlightingPassFactory)
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
        return project;
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
        return indentsModel;
    }

    public static RangeMarker createRangeMarker(EditorEx editor) {
        final int offset = editor.getCaretModel().getOffset();
        return editor.getDocument().createRangeMarker(offset, offset);
    }
}
