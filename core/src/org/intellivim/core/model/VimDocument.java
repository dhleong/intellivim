package org.intellivim.core.model;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Created by dhleong on 11/6/14.
 */
public class VimDocument extends DocumentImpl implements DocumentEx {
    private PsiFile psiFile;

    VimDocument(PsiFile psiFile) {
        this(psiFile, psiFile.getViewProvider().getContents());
    }

    public VimDocument(PsiFile psiFile, @NotNull CharSequence chars) {
        super(chars);
        this.psiFile = psiFile;
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

    public static VimDocument getInstance(PsiFile originalFile) {
        final Document doc = FileDocumentManager.getInstance()
                .getCachedDocument(originalFile.getVirtualFile());
        if (doc instanceof VimDocument) {
            return (VimDocument) doc;
        }

        return new VimDocument(originalFile);
    }
}
