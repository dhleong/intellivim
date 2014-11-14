package org.intellivim.core.command.problems;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.cache.impl.todo.TodoIndex;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.ArrayUtil;
import com.intellij.util.indexing.FileBasedIndex;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.List;

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

//        TextEditorHighlightingPassFactory factory =
//                ApplicationManager.getApplication()
//                        .getComponent(TextEditorHighlightingPassFactory.class);
        final Problems problems = new Problems();
//        HighlightInfoProcessor highlightInfoProcessor = new HighlightInfoProcessor() {
//            @Override
//            public void infoIsAvailable(HighlightingSession highlightingSession, HighlightInfo info) {
//                final Problem problem = Problem.from(problems.size(), doc, info);
//                if (problem == null)
//                    return;
//
//                problems.add(problem);
//            }
//        };
//
//        TextEditorHighlightingPass highlightingPass =
////            factory.createMainHighlightingPass(psiFile, doc, highlightInfoProcessor);
//                factory.createHighlightingPass(psiFile, editor);
//
////        ProperTextRange range = new ProperTextRange(0, editor.getDocument().getTextLength());
////        GeneralHighlightingPass highlightingPass = new GeneralHighlightingPass(project, psiFile, doc,
////                range.getStartOffset(), range.getEndOffset(),
////                false, range, editor, highlightInfoProcessor);
//        highlightingPass.collectInformation(new ProgressIndicatorBase());


        ensureIndexesUpToDate(project);

        DaemonCodeAnalyzerImpl analyzer =
                (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
        TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(editor);
//        List<HighlightInfo> found = analyzer.runMainPasses(psiFile, doc, new ProgressIndicatorBase());
        try {
            List<HighlightInfo> found = analyzer.runPasses(psiFile, doc, textEditor,
                    ArrayUtil.EMPTY_INT_ARRAY, false, null);

            for (HighlightInfo info : found) {
                final Problem problem = Problem.from(problems.size(), doc, info);
                if (problem != null) {
                    problems.add(problem);
                }
            }
        } catch (ProcessCanceledException e) {
            e.printStackTrace();
        }

        return problems;
    }

    public static void ensureIndexesUpToDate(Project project) {
        if (!DumbService.isDumb(project)) {
            FileBasedIndex.getInstance().ensureUpToDate(StubUpdatingIndex.INDEX_ID, project, null);
            FileBasedIndex.getInstance().ensureUpToDate(TodoIndex.NAME, project, null);
        }
    }

}
