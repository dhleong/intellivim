
package org.intellivim.core.model;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

/**
 * @author dhleong
 */
public class VimEditor {

    public static RangeMarker createRangeMarker(EditorEx editor) {
        final int offset = editor.getCaretModel().getOffset();
        return editor.getDocument().createRangeMarker(offset, offset);
    }

    /**
     * Convenience. Sometimes <code>PsiFile.findElementAt()</code>
     *  isn't enough. This will never return null.
     * @throws IllegalArgumentException if it couldn't find an element
     */
    public static @NotNull PsiElement findTargetElement(final Editor editor) {
        final PsiElement found = TargetElementUtil
                .findTargetElement(editor,
                        TargetElementUtil.getInstance().getAllAccepted());

        if (found == null) {
            throw new IllegalArgumentException("No element under the cursor");
        }

        return found;
    }

    /**
     * Create a new EditorEx. It will be disposed whenever
     *  context is disposed
     *
     * @param context The context during which the EditorEx
     *                will be valid.
     * @param file The file
     * @param offset The offset at which the cursor in the
     *               Editor should be placed
     */
    public static @NotNull EditorEx from(final Disposable context,
            final PsiFile file, final int offset) {

////        return new VimEditor(project, file, offset);
//        final Editor editor = Service.getInstance().findEditorByPsiElement(file);
//        if (editor instanceof EditorEx) {
//            System.out.println("EditorByElement");
//            editor.getCaretModel().moveToOffset(offset);
//            return (EditorEx) editor;
//        }
//
//        final DocumentEx doc = VimDocument.getInstance(file);
//        final EditorFactory editorFactory = EditorFactory.getInstance();
//        final Editor[] editors = editorFactory.getEditors(doc);
//        for (final Editor e : editors) {
//            if (e instanceof EditorEx) {
//                System.out.println("EditorByFactory");
//                e.getCaretModel().moveToOffset(offset);
//                return (EditorEx) e;
//            }
//        }

        final DocumentEx doc = VimDocument.getInstance(file);
        final EditorFactory editorFactory = EditorFactory.getInstance();
        final EditorEx created = (EditorEx) editorFactory.createEditor(doc, file.getProject());
        created.getCaretModel().moveToOffset(offset);

        Disposer.register(context, new Disposable() {
            @Override
            public void dispose() {
                editorFactory.releaseEditor(created);
            }
        });

        return created;
    }
}
