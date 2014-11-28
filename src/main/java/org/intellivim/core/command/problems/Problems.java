package org.intellivim.core.command.problems;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.cache.impl.todo.TodoIndex;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.indexing.FileBasedIndex;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dhleong on 11/8/14.
 */
public class Problems extends ArrayList<Problem> {

    private static final int ATTEMPTS = 5;

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
    public static Problems collectFrom(final Project project, VirtualFile virtualFile) {

        final Problems problems = new Problems();

        final PsiManager manager = PsiManager.getInstance(project);
        final PsiFile psiFile = manager.findFile(virtualFile);

        final VimEditor editor = new VimEditor(project, psiFile, 0);
        final DocumentEx doc = editor.getDocument();

        final ProgressIndicator progress = new ProgressIndicatorBase();
        ProgressManager.getInstance().executeProcessUnderProgress(new Runnable() {

            @Override
            public void run() {
                final List<HighlightInfo> results =
                        DaemonCodeAnalyzerEx.getInstanceEx(project)
                                .runMainPasses(psiFile, doc, progress);
                for (HighlightInfo info : results) {
                    Problem problem = Problem.from(problems.size(), doc, info);
                    if (problem != null)
                        problems.add(problem);
                }

            }
        }, progress);

        return problems;
    }

    private static void attemptHighlightingPass(final List<TextEditorHighlightingPass> passes, final ProgressIndicator indicator) {
        ProgressManager.getInstance().executeProcessUnderProgress(new Runnable() {

            @Override
            public void run() {
                for (TextEditorHighlightingPass pass : passes) {
                    pass.doCollectInformation(indicator);
                }

            }
        }, indicator);
    }

    public static void ensureIndexesUpToDate(Project project) {
        if (!DumbService.isDumb(project)) {
            FileBasedIndex.getInstance().ensureUpToDate(StubUpdatingIndex.INDEX_ID, project, null);
            FileBasedIndex.getInstance().ensureUpToDate(TodoIndex.NAME, project, null);
        }
    }

}
