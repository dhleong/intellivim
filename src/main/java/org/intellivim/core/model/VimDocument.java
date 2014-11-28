package org.intellivim.core.model;

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
