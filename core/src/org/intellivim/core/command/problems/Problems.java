package org.intellivim.core.command.problems;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoProcessor;
import com.intellij.codeInsight.daemon.impl.HighlightingSession;
import com.intellij.codeInspection.DefaultHighlightVisitorBasedInspection;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
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

//        // attempt 4: use the registrar
//        TextEditorHighlightingPassRegistrarImpl registrar =
//                (TextEditorHighlightingPassRegistrarImpl)
//                    TextEditorHighlightingPassRegistrar.getInstance(project);
//        final List<TextEditorHighlightingPass> passes =
//                registrar.instantiateMainPasses(psiFile, doc, highlightInfoProcessor);
//        final ProgressIndicator indicator = new ProgressIndicatorBase() {
//            @Override
//            public void cancel() {
//                // nop!
//                System.out.println("Some dummy tried to cancel!");
//                Thread.dumpStack();
//            }
//        };
//        for (int i=0; i < ATTEMPTS; i++) {
//            try {
//                attemptHighlightingPass(passes, indicator);
//                break;
//            } catch (ProcessCanceledException e) {
//                // not sure why it gets canceled sometimes,
//                //  but let's try again
//                problems.clear(); // prevent dups
//            }
//        }

        List<Pair<PsiFile, HighlightInfo>> pairs =
                DefaultHighlightVisitorBasedInspection
                        .runGeneralHighlighting(psiFile, true, true, true);
        for (Pair<PsiFile, HighlightInfo> pair : pairs) {
            HighlightInfo info = pair.getSecond();
            System.out.println(pair.getFirst() + " - " + info);
            Problem problem = Problem.from(problems.size(), doc, info);
            if (problem != null)
                problems.add(problem);
        }

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
