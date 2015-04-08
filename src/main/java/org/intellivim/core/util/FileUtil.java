package org.intellivim.core.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.reference.SoftReference;
import org.intellivim.core.model.VimDocument;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Various file-related utilities
 *
 * @author dhleong
 */
public class FileUtil {

    private static final Key<SoftReference<Trinity<PsiFile, Document, Long>>>
            FILE_COPY_KEY = Key.create("CompletionFileCopy");

    /**
     * Commits any changes made to the Document or its underlying
     *  PsiFile to disk.
     * @param doc
     */
    @SuppressWarnings("unchecked")
    private static void commitChanges(final Document doc) {

        final FileDocumentManager manager = FileDocumentManager.getInstance();
        if (!manager.isDocumentUnsaved(doc)
                && (doc instanceof VimDocument)) {
            // NB: If we're using regular Documents, we can probably trust it

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

            try {
                Set<Document> set = (Set<Document>) myUnsavedDocuments.get(manager);
                set.add(doc);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }
        }

        manager.saveDocument(doc);
    }

    /** @see #commitChanges(com.intellij.openapi.editor.Document) */
    public static void commitChanges(Editor editor) {
        final Document doc = editor.getDocument();

        // make sure the document matches the PsiFile
        final PsiDocumentManager psiMan = PsiDocumentManager.getInstance(editor.getProject());
        psiMan.doPostponedOperationsAndUnblockDocument(doc);

        commitChanges(doc);
    }

    /**
     * Duplicate a PsiFile; from CodeCompletionHandlerBase
     * @param file
     * @param caret
     * @param selEnd
     * @return
     */
    public static PsiFile createFileCopy(PsiFile file, long caret, long selEnd) {
        final VirtualFile virtualFile = file.getVirtualFile();
        boolean mayCacheCopy = file.isPhysical() &&
                // we don't want to cache code fragment copies even if they appear to be physical
                virtualFile != null && virtualFile.isInLocalFileSystem();
        long combinedOffsets = caret + (selEnd << 32);
        if (mayCacheCopy) {
            final Trinity<PsiFile, Document, Long> cached = SoftReference.dereference(file.getUserData(FILE_COPY_KEY));
            if (cached != null && cached.first.getClass().equals(file.getClass()) && isCopyUpToDate(cached.second, cached.first)) {
                final PsiFile copy = cached.first;
                if (copy.getViewProvider().getModificationStamp() > file.getViewProvider().getModificationStamp() &&
                        cached.third != combinedOffsets) {
                    // the copy PSI might have some caches that are not cleared on its modification because there are no events in the copy
                    //   so, clear all the caches
                    // hopefully it's a rare situation that the user invokes completion in different parts of the file
                    //   without modifying anything physical in between
                    ((PsiModificationTrackerImpl) file.getManager().getModificationTracker()).incCounter();
                }
                final Document document = cached.second;
                assert document != null;
                file.putUserData(FILE_COPY_KEY, new SoftReference<Trinity<PsiFile,Document, Long>>(Trinity.create(copy, document, combinedOffsets)));

                Document originalDocument = file.getViewProvider().getDocument();
                assert originalDocument != null;
                assert originalDocument.getTextLength() == file.getTextLength() : originalDocument;
                document.setText(originalDocument.getImmutableCharSequence());
                return copy;
            }
        }

        final PsiFile copy = (PsiFile)file.copy();
        if (mayCacheCopy) {
            final Document document = copy.getViewProvider().getDocument();
            assert document != null;
            file.putUserData(FILE_COPY_KEY, new SoftReference<Trinity<PsiFile,Document, Long>>(Trinity.create(copy, document, combinedOffsets)));
        }
        return copy;
    }

    /** Also from CodeCompletionHandlerBase */
    private static boolean isCopyUpToDate(Document document, @NotNull PsiFile file) {
        if (!file.isValid()) {
            return false;
        }
        // the psi file cache might have been cleared by some external activity,
        // in which case PSI-document sync may stop working
        PsiFile current = PsiDocumentManager.getInstance(file.getProject()).getPsiFile(document);
        return current != null && current.getViewProvider().getPsi(file.getLanguage()) == file;
    }

    public static void writeToDisk(final PsiFile file) {
        final Project project = file.getProject();
        final Document doc = VimDocument.getInstance(file);
        final PsiDocumentManager man = PsiDocumentManager.getInstance(project);

        man.doPostponedOperationsAndUnblockDocument(doc);

        commitChanges(doc);
    }
}
