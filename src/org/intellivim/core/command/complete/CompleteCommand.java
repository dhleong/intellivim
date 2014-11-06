package org.intellivim.core.command.complete;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionData;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.JavaCompletionContributor;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.Consumer;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.model.VimLookup;
import org.intellivim.core.util.ProjectUtil;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by dhleong on 11/3/14.
 */
public class CompleteCommand {

    public void execute(String projectPath) {
        final Project project = ProjectUtil.getProject(projectPath);

        File file = new File(project.getBasePath() + "/src/org/intellivim/dummy/Dummy.java");
        System.out.println(file);
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        final int offset = 165;
//        final int offset = 196;

        System.out.println("Hello: " + project + " / " + virtualFile);

        final CompletionParameters params = newParams(project, virtualFile, offset);
        final Consumer<CompletionResult> consumer = new Consumer<CompletionResult>() {

            @Override
            public void consume(CompletionResult completionResult) {
                System.out.println("result: " + completionResult);
            }
        };

        final LookupElement[] results = performCompletion(params, consumer);
        System.out.println("completion results: " + results.length);
        for (LookupElement el : results) {
            System.out.println("result: " + el);
        }

//        PsiElement el = params.getPosition();
//        if (el.getParent() instanceof PsiReference) {
//            System.out.println("Parent: " + el.getParent());
//            PsiReference ref = (PsiReference) el.getParent();
//            for (Object o : ref.getVariants()) {
//                System.out.println("variant: " + o);
//            }
//        }
    }

    // imported from CompletionService to get around wacky UI requirements
    public LookupElement[] performCompletion(final CompletionParameters parameters, final Consumer<CompletionResult> consumer) {
        final Collection<LookupElement> lookupSet = new LinkedHashSet<LookupElement>();

        getVariantsFromContributors(parameters, null, new Consumer<CompletionResult>() {
            @Override
            public void consume(final CompletionResult result) {
                System.out.println("Consume!" + result);
                if (lookupSet.add(result.getLookupElement())) {
                    consumer.consume(result);
                }
            }
        });
        return lookupSet.toArray(new LookupElement[lookupSet.size()]);
    }
    /**
     * Run all contributors until any of them returns false or the list is exhausted. If from parameter is not null, contributors
     * will be run starting from the next one after that.
     * @param parameters
     * @param from
     * @param consumer
     * @return
     */
    public static void getVariantsFromContributors(final CompletionParameters parameters,
                                            final CompletionContributor from,
                                            final Consumer<CompletionResult> consumer) {
        final List<CompletionContributor> contributors = CompletionContributor.forParameters(parameters);
        final boolean dumb = DumbService.getInstance(parameters.getPosition().getProject()).isDumb();
        System.out.println("Found " + contributors.size() + " contributors");

        for (int i = contributors.indexOf(from) + 1; i < contributors.size(); i++) {
            final CompletionContributor contributor = contributors.get(i);
            if (dumb && !DumbService.isDumbAware(contributor)) continue;

//            System.out.println("Try " + contributor);
            CompletionResultSet result = createResultSet(parameters, consumer, contributor);
            if (contributor.getClass().getName().contains("Java")) {
                System.out.println("Hello" + contributor);
            }
            contributor.fillCompletionVariants(parameters, result);
            if (result.isStopped()) {
                System.out.println("Stopped by " + contributor);
                return;
            }
        }

    }

    static CompletionResultSet createResultSet(final CompletionParameters parameters, final Consumer<CompletionResult> consumer,
                                               final CompletionContributor contributor) {
        final PsiElement position = parameters.getPosition();
        final String prefix = CompletionData.findPrefixStatic(position, parameters.getOffset());
        final int lengthOfTextBeforePosition = parameters.getOffset();
        CamelHumpMatcher matcher = new CamelHumpMatcher(prefix);
        CompletionSorter sorter = CompletionService.getCompletionService().defaultSorter(parameters, matcher);
        return new CompletionResultSetImpl(consumer, lengthOfTextBeforePosition, matcher,
                contributor, parameters, sorter, null);
    }

    private CompletionParameters newParams(Project project, VirtualFile file, int offset) {
        PsiFile originalFile = PsiManager.getInstance(project).findFile(file);
        PsiElement position = originalFile.findElementAt(offset);
        CompletionType completionType = CompletionType.BASIC;

        Editor editor = new VimEditor(originalFile, offset);
        Lookup lookup = new VimLookup(project, editor);

        int invocationCount = 0;

        System.out.println("In " + originalFile + ": found: " + position + " with " + lookup);
        System.out.println(" --> inJavaContext? " + JavaCompletionContributor.isInJavaContext(position));

        CodeCompletionHandlerBase handler = new CodeCompletionHandlerBase(completionType);
        handler.invokeCompletion(project, editor);
        System.out.println("--------------");

        try {
            Constructor<CompletionParameters> ctor = CompletionParameters.class.getDeclaredConstructor(
                    PsiElement.class /* position */, PsiFile.class /* originalFile */,
                    CompletionType.class, int.class /* offset */, int.class /* invocationCount */,
                    Lookup.class
            );
            ctor.setAccessible(true);
            return ctor.newInstance(position, originalFile, completionType, offset, invocationCount, lookup);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }


}
