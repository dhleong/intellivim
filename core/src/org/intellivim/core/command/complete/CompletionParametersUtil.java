package org.intellivim.core.command.complete;

import com.intellij.codeInsight.completion.CompletionContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.OffsetMap;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.model.VimLookup;

import java.lang.reflect.Constructor;

/**
 * Created by dhleong on 11/8/14.
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
        PsiFile originalFile = PsiManager.getInstance(project).findFile(file);

        PsiFile fileCopy = originalFile;
        PsiElement position = fileCopy.findElementAt(offset);
        CompletionType completionType = CompletionType.BASIC;

        Editor editor = new VimEditor(project, fileCopy, offset);
        Lookup lookup = new VimLookup(project, editor);

        int invocationCount = 0;

        OffsetMap offsetMap = new OffsetMap(editor.getDocument());
        CompletionContext context = new CompletionContext(originalFile, offsetMap);
        position.putUserData(CompletionContext.COMPLETION_CONTEXT_KEY, context);

//        System.out.println("In " + originalFile + ": found: " + position + " with " + lookup);
//        System.out.println("After caret:["
//                + editor.getDocument().getText(new TextRange(offset, offset + 10))
//                + "]");

        return newInstance(position, originalFile,
                completionType, offset, invocationCount, lookup);
    }
}
