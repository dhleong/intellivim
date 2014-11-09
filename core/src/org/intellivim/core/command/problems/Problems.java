package org.intellivim.core.command.problems;

import com.intellij.codeInsight.daemon.impl.GeneralHighlightingPass;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoProcessor;
import com.intellij.codeInsight.daemon.impl.HighlightingSession;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;

/**
 * Created by dhleong on 11/8/14.
 */
public class Problems extends ArrayList<Problem> {

    public static Problems collectFrom(Project project, String filePath) {
        final VirtualFile virtualFile = ProjectUtil.getVirtualFile(project, filePath);
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        final VimEditor editor = new VimEditor(project, psiFile, 0);
        final DocumentEx doc = editor.getDocument();

        final Problems problems = new Problems();
        HighlightInfoProcessor highlightInfoProcessor = new HighlightInfoProcessor() {
            @Override
            public void infoIsAvailable(HighlightingSession highlightingSession, HighlightInfo info) {
                final Problem problem = Problem.from(doc, info);
                if (problem == null)
                    return;

                problems.add(problem);
            }
        };

        ProperTextRange range = new ProperTextRange(0, editor.getDocument().getTextLength());
        GeneralHighlightingPass highlightingPass = new GeneralHighlightingPass(project, psiFile, doc,
                range.getStartOffset(), range.getEndOffset(),
                false, range, editor, highlightInfoProcessor);
        highlightingPass.collectInformation(new ProgressIndicatorBase());

        return problems;
    }
}
