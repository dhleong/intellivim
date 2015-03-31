package org.intellivim.core.command.problems;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.cache.impl.todo.TodoIndex;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.indexing.FileBasedIndex;
import org.intellivim.core.model.VimEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhleong
 */
public class Problems extends ArrayList<Problem> {

    private static final int ATTEMPTS = 5;

    public Problems filterByFixType(final Class<? extends QuickFixDescriptor> fixType) {
        return filter(new Condition<Problem>() {
            @Override
            public boolean value(Problem problem) {
                for (QuickFixDescriptor fix : problem.getFixes()) {
                    if (fixType.isAssignableFrom(fix.getClass())) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    public Problems filter(Condition<Problem> condition) {
        Problems filtered = new Problems();
        for (Problem problem : this) {
            if (condition.value(problem))
                filtered.add(problem);
        }

        return filtered;
    }

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

    /**
     * When resolving multiple ambiguous imports from OptimizeImportsCommand,
     *  we need to be able to use an old descriptor to locate the equivalent
     *  in a new set of problems and fixes
     * @param descriptor The old descriptor
     * @return The new, equivalent descriptor, if any
     * @throws java.lang.IllegalArgumentException if no such updated descriptor exists
     */
    public QuickFixDescriptor locateQuickFix(final QuickFixDescriptor descriptor) {
        for (Problem problem : this) {
            // just look at all problems (unfortunately) since the
            //  offsets have probably changed
            for (QuickFixDescriptor updated : problem.getFixes()) {
                if (updated.equals(descriptor))
                    return updated;
            }
        }

        throw new IllegalArgumentException("Unable to find " + descriptor);
    }

    public static Problems collectFrom(final Project project, final PsiFile psiFile) {

        final Problems problems = new Problems();

        final VimEditor editor = new VimEditor(project, psiFile, 0);
        final DocumentEx doc = editor.getDocument();

//        final ProgressIndicator progress = new ProgressIndicatorBase();
        final ProgressIndicator progress = new DaemonProgressIndicator();
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
