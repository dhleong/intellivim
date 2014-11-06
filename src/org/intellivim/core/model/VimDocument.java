package org.intellivim.core.model;

import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Created by dhleong on 11/6/14.
 */
public class VimDocument extends DocumentImpl {
    private PsiFile psiFile;

    public VimDocument(PsiFile psiFile) {
        this(psiFile, psiFile.getViewProvider().getContents());
    }

    public VimDocument(PsiFile psiFile, @NotNull CharSequence chars) {
        super(chars);
        this.psiFile = psiFile;
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        if ("HARD_REFERENCE_TO_PSI".equals(key.toString())) {

            return (T) psiFile;
        }
        return super.getUserData(key);
    }
}
