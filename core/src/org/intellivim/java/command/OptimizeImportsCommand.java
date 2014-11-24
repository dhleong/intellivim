package org.intellivim.java.command;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFix;
import com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase;
import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.java.JavaImportOptimizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaReference;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.ResolveClassUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.problems.Problem;
import org.intellivim.core.command.problems.Problems;
import org.intellivim.core.command.problems.QuickFixDescriptor;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.FileUtil;
import org.intellivim.core.util.IntelliVimUtil;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by dhleong on 11/8/14.
 */
@Command("java_import_optimize")
public class OptimizeImportsCommand extends ProjectCommand {

    @Required String file;

    public OptimizeImportsCommand(Project project, String filePath) {
        super(project);
        file = filePath;
    }

    @Override
    public Result execute() {
        final VirtualFile virtualFile = ProjectUtil.getVirtualFile(project, file);
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        final VimEditor editor = new VimEditor(project, psiFile, 0);
        if (!(psiFile instanceof PsiJavaFile)) {
            return SimpleResult.error(file + " is not a Java file");
        }

        IntelliVimUtil.runInUnitTestMode(new Runnable() {

            @Override
            public void run() {
                final boolean old = CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY;
                CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY = true;

                for (PsiJavaCodeReferenceElement el
                        : findUnresolvedReferences((PsiJavaFile) psiFile)) {
                    // TODO handle ambiguous imports somehow
                    attemptAutoImport(editor, el);
                }

                CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY = old;

                FileUtil.commitChanges(editor);
            }
        });

        ImportOptimizer optimizer = new JavaImportOptimizer();
        if (optimizer.supports(psiFile)) {
            Runnable action = optimizer.processFile(psiFile);
            ApplicationManager.getApplication().runWriteAction(action);
        }

        FileUtil.commitChanges(editor);

        return SimpleResult.success();
    }

    private ImportClassFixBase.Result attemptAutoImport(EditorEx editor, PsiJavaCodeReferenceElement el) {
        editor.getCaretModel().moveToOffset(el.getTextOffset());
        return new ImportClassFix(el).doFix(editor, false, false);
    }

    private List<QuickFixDescriptor> findImportProblemFixes() {
        List<QuickFixDescriptor> fixes = new ArrayList<QuickFixDescriptor>();
        Problems problems = Problems.collectFrom(project, file);
        for (Problem problem : problems) {
            if (!problem.isError()) continue;

            QuickFixDescriptor importFix = findImportFix(problem);
            if (importFix != null) {
                // found import fix
                fixes.add(importFix);
            }
        }

        return fixes;
    }

    private QuickFixDescriptor findImportFix(Problem problem) {

        for (QuickFixDescriptor descriptor : problem.getFixes()) {
            if (descriptor.getFix() instanceof ImportClassFix)
                return descriptor;
        }

        return null;
    }

    private List<PsiJavaCodeReferenceElement> findUnresolvedReferences(PsiJavaFile file) {
        List<PsiJavaCodeReferenceElement> elements = new ArrayList<PsiJavaCodeReferenceElement>();

        // mostly borrowed from ImportHelper
        final LinkedList<PsiElement> stack = new LinkedList<PsiElement>();
        stack.add(file);
        while (!stack.isEmpty()) {
            final PsiElement child = stack.removeFirst();
            if (child instanceof PsiLiteralExpression) continue;
            ContainerUtil.addAll(stack, child.getChildren());

            for (final PsiReference reference : child.getReferences()) {
                if (!(reference instanceof PsiJavaReference)) continue;
                final PsiJavaReference javaReference = (PsiJavaReference) reference;
                if (javaReference instanceof JavaClassReference && ((JavaClassReference) javaReference).getContextReference() != null)
                    continue;
                PsiJavaCodeReferenceElement referenceElement = null;
                if (reference instanceof PsiJavaCodeReferenceElement) {
                    referenceElement = (PsiJavaCodeReferenceElement) child;
                    if (referenceElement.getQualifier() != null) {
                        continue;
                    }
//                    if (reference instanceof PsiJavaCodeReferenceElementImpl
//                            && ((PsiJavaCodeReferenceElementImpl) reference).getKind() == PsiJavaCodeReferenceElementImpl.CLASS_IN_QUALIFIED_NEW_KIND) {
//                        continue;
//                    }
                }

                final JavaResolveResult resolveResult = javaReference.advancedResolve(true);
                PsiElement refElement = resolveResult.getElement();
                if (refElement == null && referenceElement != null) {
                    refElement = ResolveClassUtil.resolveClass(referenceElement); // might be incomplete code
                }
                if (refElement != null) continue; // already resolved?

                if (referenceElement != null) {
                    System.out.println("Couldn't resolve: " + javaReference
                            + " @" + child.getTextOffset());

                    elements.add(referenceElement);
                }
            }
        }

        return elements;
    }

}
