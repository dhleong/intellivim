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

    public QuickFixDescriptor locateQuickFix(String fixId) {
        String problemId = fixId.substring(0, fixId.indexOf(Problem.FIX_ID_SEPARATOR));
        try {
            int index = Integer.parseInt(problemId);
            Problem problem = get(index);
            return problem.locateQuickFix(fixId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid quickfix id");
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid problem id");
        }
    }

    public static Problems collectFrom(Project project, String filePath) {
        return collectFrom(project, ProjectUtil.getVirtualFile(project, filePath));
    }
    public static Problems collectFrom(Project project, VirtualFile virtualFile) {
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        final VimEditor editor = new VimEditor(project, psiFile, 0);
        final DocumentEx doc = editor.getDocument();

        final Problems problems = new Problems();
        HighlightInfoProcessor highlightInfoProcessor = new HighlightInfoProcessor() {
            @Override
            public void infoIsAvailable(HighlightingSession highlightingSession, HighlightInfo info) {
                final Problem problem = Problem.from(problems.size(), doc, info);
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
