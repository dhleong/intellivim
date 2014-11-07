package org.intellivim.core.command.complete;

import com.intellij.codeInsight.completion.CompletionContext;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionData;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.OffsetMap;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Consumer;
import org.intellivim.core.SimpleResult;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.model.VimLookup;
import org.intellivim.core.util.ProjectUtil;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by dhleong on 11/3/14.
 */
public class CompleteCommand {

    public Object execute(String projectPath, String filePath, int offset) {
        final Project project = ProjectUtil.getProject(projectPath);
        if (project == null)
            return SimpleResult.error("Couldn't find project at " + projectPath);

        File file = new File(project.getBasePath() + filePath);
        System.out.println(file);
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
//        final int offset = 164;
//        final int offset = 165;
//        final int offset = 196;
//        final int offset = 237; // dummy.
//        final int offset = 326; // arrayList.

        final CompletionParameters params = newParams(project, virtualFile, offset);
        final Consumer<CompletionResult> consumer = new Consumer<CompletionResult>() {

            @Override
            public void consume(CompletionResult completionResult) {
//                System.out.println("result: " + completionResult);
            }
        };

        final LookupElement[] results = performCompletion(params, consumer);
        ArrayList<CompletionInfo> infos = new ArrayList<CompletionInfo>(results.length);
        System.out.println("completion results: " + results.length);
        for (LookupElement el : results) {
            System.out.println("result: " + el.getPsiElement() + " / " + el + " / " + el.getClass());

            CompletionInfo info = CompletionInfo.from(el);
            if (info != null)
                infos.add(info);
        }

        return SimpleResult.success(infos);
    }

    // imported from CompletionService to get around wacky UI requirements
    public LookupElement[] performCompletion(final CompletionParameters parameters, final Consumer<CompletionResult> consumer) {
        final Collection<LookupElement> lookupSet = new LinkedHashSet<LookupElement>();

        getVariantsFromContributors(parameters, null, new Consumer<CompletionResult>() {
            @Override
            public void consume(final CompletionResult result) {
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
//        final boolean dumb = DumbService.getInstance(parameters.getPosition().getProject()).isDumb();
        System.out.println("Found " + contributors.size() + " contributors");

        for (int i = contributors.indexOf(from) + 1; i < contributors.size(); i++) {
            final CompletionContributor contributor = contributors.get(i);
//            System.out.println(contributor + " isDumbAware?" + DumbService.isDumbAware(contributor));
//            if (dumb && !DumbService.isDumbAware(contributor)) continue;

            CompletionResultSet result = createResultSet(parameters, consumer, contributor);
            if (contributor.getClass().getName().endsWith("JavaCompletionContributor")) {
                System.out.println("Hello: " + contributor);
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

        PsiFile fileCopy = originalFile;
        // dup and insert dummy
//        PsiElement copy = originalFile.copy();
//        PsiFile fileCopy = copy.getContainingFile();
//        PsiElement position = new PsiIdentifierImpl(CompletionUtilCore.DUMMY_IDENTIFIER);
//        PsiElement anchor = copy.findElementAt(offset);
//        anchor.getParent().addBefore(position, anchor);

        PsiElement position = fileCopy.findElementAt(offset);
        CompletionType completionType = CompletionType.BASIC;

        Editor editor = new VimEditor(project, fileCopy, offset);
        Lookup lookup = new VimLookup(project, editor);

        int invocationCount = 0;

        OffsetMap offsetMap = new OffsetMap(editor.getDocument());
        CompletionContext context = new CompletionContext(originalFile, offsetMap);
        position.putUserData(CompletionContext.COMPLETION_CONTEXT_KEY, context);

        System.out.println("In " + originalFile + ": found: " + position + " with " + lookup);
        System.out.println("After caret:["
                + editor.getDocument().getText(new TextRange(offset, offset + 10))
                + "]");

//        System.out.println("Project initialized: " + project.isInitialized()
//                + "; open? " + project.isOpen());
//        CodeCompletionHandlerBase handler = new CodeCompletionHandlerBase(completionType);
//        handler.invokeCompletion(project, editor);
//        System.out.println("--------------");

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
