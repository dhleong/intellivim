package org.intellivim.core.command.complete;

import com.intellij.codeInsight.completion.CompletionContext;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.OffsetMap;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.DocumentUtil;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.model.VimLookup;
import org.intellivim.core.util.FileUtil;

import java.lang.reflect.Constructor;

/**
 * @author dhleong
 */
public class CompletionParametersUtil {

    static Constructor<CompletionParameters> sConstructor;

    static CompletionParameters newInstance(PsiElement position, PsiFile originalFile,
            CompletionType completionType, int offset, int invocationCount, Lookup lookup) {

        try {

            final Constructor<CompletionParameters> cached = sConstructor;

            final Constructor<CompletionParameters> ctor;
            if (cached == null) {
                ctor = CompletionParameters.class.getDeclaredConstructor(
                        PsiElement.class /* position */, PsiFile.class /* originalFile */,
                        CompletionType.class, int.class /* offset */, int.class /* invocationCount */,
                        Lookup.class
                );
                ctor.setAccessible(true);
                sConstructor = ctor;
            } else {
                ctor = cached;
            }

            return ctor.newInstance(position, originalFile, completionType, offset, invocationCount, lookup);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    static CompletionParameters from(Project project, VirtualFile file, int offset) {

        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        final PsiElement position = psiFile.findElementAt(offset);
        final CompletionType completionType = CompletionType.BASIC;

        final Editor editor = new VimEditor(project, psiFile, offset);
        final Lookup lookup = new VimLookup(project, editor);

        final int invocationCount = 0;

        final OffsetMap offsetMap = new OffsetMap(editor.getDocument());
        final CompletionContext context = new CompletionContext(psiFile, offsetMap);
        position.putUserData(CompletionContext.COMPLETION_CONTEXT_KEY, context);

        // we need to insert a dummy identifier so there's something there.
        //  this is what intellij does typically
        final PsiElement completionPosition = insertDummyIdentifier(
                psiFile, position, editor);

        return newInstance(completionPosition, psiFile,
                completionType, offset, invocationCount, lookup);
    }

    /** based on CodeCompletionHandlerBase */
    private static PsiElement insertDummyIdentifier(final PsiFile originalFile,
            final PsiElement position, final Editor editor) {
        final InjectedLanguageManager manager = InjectedLanguageManager
                .getInstance(originalFile.getProject());
        final PsiFile hostFile = manager.getTopLevelFile(originalFile);

        final PsiFile[] hostCopy = {null};
        DocumentUtil.writeInRunUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                final int start = position.getTextOffset();
                final int end = start + position.getTextLength();
                hostCopy[0] = FileUtil.createFileCopy(hostFile, start, end);
            }
        });

        final Document copyDocument = hostCopy[0].getViewProvider().getDocument();
        if (copyDocument == null) {
            throw new IllegalStateException("No document found for copy");
        }

        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        final String dummyIdentifier =
                                CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED;
                        if (StringUtil.isEmpty(dummyIdentifier)) return;

                        final int startOffset = position.getTextOffset();
                        final int endOffset = startOffset + position.getTextLength();
//                        int startOffset = hostMap.getOffset(CompletionInitializationContext.START_OFFSET);
//                        int endOffset = hostMap.getOffset(CompletionInitializationContext.SELECTION_END_OFFSET);
                        copyDocument.replaceString(startOffset, endOffset, dummyIdentifier);
                    }
                });
            }
        });

        PsiDocumentManager.getInstance(originalFile.getProject())
                .commitDocument(copyDocument);
        return hostCopy[0];
    }

}
