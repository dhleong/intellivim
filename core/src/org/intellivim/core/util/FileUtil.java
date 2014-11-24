package org.intellivim.core.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Various file-related utilities
 */
public class FileUtil {

    /**
     * Commits any changes made to the Document or its underlying
     *  PsiFile to disk.
     * @param doc
     */
    public static void commitChanges(final Document doc) {

        // we have to assume the Impl because it will refuse to write anything
        //  if it doesn't think it's "unsaved." Since we made our own Document,
        //  we need to let it know that it's "unsaved"
        // TODO: If we just register the appropriate listeners when we create
        //  the VimDocument, this whole method may not be necessary
        final Field myUnsavedDocuments;
        try {
            myUnsavedDocuments = FileDocumentManagerImpl.class.getDeclaredField("myUnsavedDocuments");
            myUnsavedDocuments.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return;
        }

        FileDocumentManagerImpl manager = (FileDocumentManagerImpl) FileDocumentManager.getInstance();
        try {
            Set<Document> set = (Set<Document>) myUnsavedDocuments.get(manager);
            set.add(doc);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        manager.saveDocument(doc);
    }

    /** @see #commitChanges(com.intellij.openapi.editor.Document) */
    public static void commitChanges(Editor editor) {
        commitChanges(editor.getDocument());
    }
}
