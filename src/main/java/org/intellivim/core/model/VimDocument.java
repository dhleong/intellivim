package org.intellivim.core.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author dhleong
 */
public class VimDocument extends DocumentImpl implements DocumentEx {
    private PsiFile psiFile;
    private boolean forceEventsHandling;

    /** Testing only */
    public VimDocument(@NotNull CharSequence chars) {
        this(null, chars);

        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            throw new IllegalStateException("This constructor is ONLY for testing");
        }
    }

    VimDocument(PsiFile psiFile) {
        this(psiFile, psiFile.getViewProvider().getContents());
    }

    VimDocument(PsiFile psiFile, @NotNull CharSequence chars) {
        super(chars);
        this.psiFile = psiFile;
    }

    @Override
    public boolean isInEventsHandling() {
        if (forceEventsHandling)
            return true;

        return super.isInEventsHandling();
    }

    public void setUncommited(boolean isUncommited) {
        // while this flag is true, PsiDocumentManager will
        //  think we are uncommitted. In this state, the
        //  FormattingDocumentModelImpl will create a new,
        //  temporary DocumentImpl. When using that new one,
        //  it successfully formats the text without generating
        //  garbage. I still don't know why it makes that garbage,
        //  or what the practical difference between this DocumentImpl
        //  and the new one would be, but this seems an acceptable hack
        forceEventsHandling = isUncommited;
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        String stringValue = key.toString();
        if ("HARD_REFERENCE_TO_PSI".equals(stringValue)) {
            return (T) psiFile;
        }
        if ("FILE_KEY".equals(stringValue)) {
            return (T) psiFile.getVirtualFile();
        }
        return super.getUserData(key);
    }

    @Override
    public String toString() {
        return "VimDocument: " + super.toString();
    }

    public static VimDocument getInstance(@NotNull PsiFile originalFile) {
        VirtualFile file = originalFile.getVirtualFile();
        final Document doc = FileDocumentManager.getInstance()
                .getCachedDocument(file);
        if (doc instanceof VimDocument) {
            return (VimDocument) doc;
        }

        // create and cache
        final VimDocument newDoc = new VimDocument(originalFile);
//        file.putUserData(FileDocumentManagerImpl.DOCUMENT_KEY,
//                new SoftReference<Document>(newDoc));
        FileDocumentManagerImpl.registerDocument(newDoc, file);

        return newDoc;
    }
}
