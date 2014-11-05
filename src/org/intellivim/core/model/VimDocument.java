package org.intellivim.core.model;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;

/**
 * Created by dhleong on 11/4/14.
 */
public class VimDocument implements Document {

    private final PsiFile originalFile;

    public VimDocument(PsiFile originalFile) {
        this.originalFile = originalFile;
    }

    @NotNull
    @Override
    public String getText() {
        System.out.println(">> VimDocument.getText");
        return getCharsSequence().toString();
    }

    @NotNull
    @Override
    public String getText(@NotNull TextRange range) {
        System.out.println(">> VimDocument.getText(Range)");
        return getCharsSequence()
                .subSequence(range.getStartOffset(), range.getEndOffset())
                .toString();
    }

    @NotNull
    @Override
    public CharSequence getCharsSequence() {
        return originalFile.getViewProvider().getContents();
    }

    @NotNull
    @Override
    public CharSequence getImmutableCharSequence() {
        System.out.println(">> VimDocument.getImmutableCharsSequence");
        return getCharsSequence(); // ???
    }

    @NotNull
    @Override
    public char[] getChars() {
        System.out.println(">> VimDocument.getChars");
        return getText().toCharArray();
    }

    @Override
    public int getTextLength() {
        System.out.println(">> VimDocument.getTextLength");
        return originalFile.getTextLength();
    }

    @Override
    public int getLineCount() {
        System.out.println(">> VimDocument.getLineCount");
        return 0;
    }

    @Override
    public int getLineNumber(int offset) {
        return 0;
    }

    @Override
    public int getLineStartOffset(int line) {
        return 0;
    }

    @Override
    public int getLineEndOffset(int line) {
        return 0;
    }

    @Override
    public void insertString(int offset, @NotNull CharSequence s) {

    }

    @Override
    public void deleteString(int startOffset, int endOffset) {

    }

    @Override
    public void replaceString(int startOffset, int endOffset, @NotNull CharSequence s) {

    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public long getModificationStamp() {
        return 0;
    }

    @Override
    public void fireReadOnlyModificationAttempt() {

    }

    @Override
    public void addDocumentListener(@NotNull DocumentListener listener) {

    }

    @Override
    public void addDocumentListener(@NotNull DocumentListener listener, @NotNull Disposable parentDisposable) {

    }

    @Override
    public void removeDocumentListener(@NotNull DocumentListener listener) {

    }

    @NotNull
    @Override
    public RangeMarker createRangeMarker(int startOffset, int endOffset) {
        System.out.println(">> VimDocument.createRangeMarker");
        return null;
    }

    @NotNull
    @Override
    public RangeMarker createRangeMarker(int startOffset, int endOffset, boolean surviveOnExternalChange) {
        System.out.println(">> VimDocument.createRangeMarker2");
        return null;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void setReadOnly(boolean isReadOnly) {

    }

    @NotNull
    @Override
    public RangeMarker createGuardedBlock(int startOffset, int endOffset) {
        return null;
    }

    @Override
    public void removeGuardedBlock(@NotNull RangeMarker block) {

    }

    @Nullable
    @Override
    public RangeMarker getOffsetGuard(int offset) {
        return null;
    }

    @Nullable
    @Override
    public RangeMarker getRangeGuard(int start, int end) {
        return null;
    }

    @Override
    public void startGuardedBlockChecking() {

    }

    @Override
    public void stopGuardedBlockChecking() {

    }

    @Override
    public void setCyclicBufferSize(int bufferSize) {

    }

    @Override
    public void setText(@NotNull CharSequence text) {

    }

    @NotNull
    @Override
    public RangeMarker createRangeMarker(@NotNull TextRange textRange) {
        return null;
    }

    @Override
    public int getLineSeparatorLength(int line) {
        return 0;
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        System.out.println(">> VimDocument.getUserData");
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        System.out.println(">> VimDocument.putUserData");

    }
}
