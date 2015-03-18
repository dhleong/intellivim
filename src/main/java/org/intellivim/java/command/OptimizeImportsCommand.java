package org.intellivim.java.command;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.java.JavaImportOptimizer;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaReference;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.ResolveClassUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.problems.FixProblemCommand;
import org.intellivim.core.command.problems.ImportsQuickFixDescriptor;
import org.intellivim.core.command.problems.Problem;
import org.intellivim.core.command.problems.Problems;
import org.intellivim.core.command.problems.QuickFixDescriptor;
import org.intellivim.core.command.problems.QuickFixException;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.FileUtil;
import org.intellivim.core.util.IntelliVimUtil;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Attempt to automatically resolve unambiguous imports,
 *  and provide a means to prompt for resolving ambiguous ones.
 *  Returns a list of ImportQuickFixDescriptors if there were
 *  any ambiguous imports, which will have the <code>choices[]</code>
 *  for resolution. {@see FixProblemCommand} for how to deal with these
 *
 * @author dhleong
 */
@Command("java_import_optimize")
public class OptimizeImportsCommand extends ProjectCommand {

    @Required String file;

    int offset;

    // NB: for multiple imports, we need to re-load these each time
    transient PsiFile psiFile;
    transient VimEditor editor;

    // created once, on first prepare()
    transient RangeMarker marker;

    public OptimizeImportsCommand(Project project, String filePath) {
        super(project);
        file = filePath;
    }

    @Override
    public Result execute() {
        // always go ahead and clear this
        FixProblemCommand.clearPendingFixes();

        final List<ImportsQuickFixDescriptor> ambiguous =
                new ArrayList<ImportsQuickFixDescriptor>();
        IntelliVimUtil.runInUnitTestMode(new Runnable() {

            @Override
            public void run() {
                final boolean old = CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY;
                CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY = true;

                for (final ImportsQuickFixDescriptor desc : findImportProblemFixes()) {
                    try {
//                        System.out.println("Executing " + desc.getDescription());
//                        if (!psiFile.textMatches(editor.getDocument().getCharsSequence())) {
//                            System.out.println("Using " + psiFile.getText());
//                            System.out.println("Vs: " + editor.getDocument().getCharsSequence());
//                        }
                        Object result = desc.execute(project, editor, psiFile, null);
                        if (null != result) {
                            // it was ambiguous
                            ambiguous.add(desc);
                        }
//                        System.out.println("Executed: " + desc.getDescription());

                    } catch (QuickFixException e) {
                        // don't care
                    }
                }

                CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY = old;

                FileUtil.commitChanges(editor);
            }
        });

        // prepare again
        prepare();

        final ImportOptimizer optimizer = new JavaImportOptimizer();
        if (optimizer.supports(psiFile)) {
            final Runnable action = optimizer.processFile(psiFile);

            CommandProcessor.getInstance().runUndoTransparentAction(
                    IntelliVimUtil.asWriteAction(action));

            FileUtil.commitChanges(editor);
        }

        if (ambiguous.isEmpty()) {
            return SimpleResult.success().withOffsetFrom(marker);
        } else {
            FixProblemCommand.setPendingFixes(psiFile, ambiguous);
            return SimpleResult.success(ambiguous).withOffsetFrom(marker);
        }
    }

    private void prepare() {
        psiFile = ProjectUtil.getPsiFile(project, file);
        editor = new VimEditor(project, psiFile, offset);
        if (!(psiFile instanceof PsiJavaFile)) {
            throw new IllegalArgumentException(file + " is not a Java file");
        }

        if (marker == null) {
            marker = editor.createRangeMarker();
        }
    }

    private Iterable<ImportsQuickFixDescriptor> findImportProblemFixes() {
        return new ImportsQuickFixIterator();
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
                    refElement = ResolveClassUtil.resolveClass(referenceElement, file); // might be incomplete code
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

    private static ImportsQuickFixDescriptor findImportFix(Problem problem) {

        for (QuickFixDescriptor descriptor : problem.getFixes()) {
            if (descriptor instanceof ImportsQuickFixDescriptor)
                return (ImportsQuickFixDescriptor) descriptor;
        }

        return null;
    }

    private class ImportsQuickFixIterator
            implements Iterable<ImportsQuickFixDescriptor>,
                       Iterator<ImportsQuickFixDescriptor> {

        final Iterator<ImportsQuickFixDescriptor> fixes = fetch().iterator();

        @Override
        public boolean hasNext() {
            return fixes.hasNext();
        }

        public ImportsQuickFixDescriptor next() {
            ImportsQuickFixDescriptor fix = fixes.next();

            // make sure we're using the most up-to-date PsiFile and a matching Editor
            prepare();
            return (ImportsQuickFixDescriptor) Problems.collectFrom(project, psiFile)
                    .locateQuickFix(fix);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<ImportsQuickFixDescriptor> iterator() {
            return this;
        }

        private List<ImportsQuickFixDescriptor> fetch() {

            prepare();
            List<ImportsQuickFixDescriptor> fixes = new ArrayList<ImportsQuickFixDescriptor>();
            Problems problems = Problems.collectFrom(project, psiFile);
            for (Problem problem : problems) {
                if (!problem.isError()) continue;

                ImportsQuickFixDescriptor importFix = findImportFix(problem);
                if (importFix != null) {
                    // found import fix
                    fixes.add(importFix);
                }
            }

            return fixes;
        }
    }
}
