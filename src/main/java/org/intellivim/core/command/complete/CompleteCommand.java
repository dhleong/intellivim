package org.intellivim.core.command.complete;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionData;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.Consumer;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.problems.ImportsQuickFixDescriptor;
import org.intellivim.core.command.problems.Problem;
import org.intellivim.core.command.problems.Problems;
import org.intellivim.core.command.problems.QuickFixDescriptor;
import org.intellivim.core.command.problems.QuickFixException;
import org.intellivim.core.model.VimEditor;
import org.intellivim.inject.Inject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Returns a CompletionResultInfo object.
 *  If completions is null, there were no problems
 *  If completions is non-null but empty, we auto-resolved problems
 *  Otherwise, there were either ambiguous problems, or multiple problems
 *
 * @author dhleong
 */
@Command("complete")
public class CompleteCommand extends ProjectCommand {


    public static class CompletionResultInfo {
        public final List<CompletionInfo<?>> completions;
        public final Problems problems;

        private CompletionResultInfo(List<CompletionInfo<?>> completions, Problems problems) {
            this.completions = completions;
            this.problems = problems;
        }
    }

    @Required @Inject PsiFile file;
    @Required int offset;

    /* optional */String prefix;

    private boolean myEmptyLookup;
    private transient Editor myEditor;

    public CompleteCommand(Project project, PsiFile file, int offset) {
        super(project);

        this.file = file;
        this.offset = offset;
    }

    @Override
    public Result execute() {
        final EditorEx editor = createEditor(file, offset);
        myEditor = editor;
        final CompletionParameters params = CompletionParametersUtil.from(editor, file, offset);
        final ArrayList<CompletionInfo<?>> infos = new ArrayList<CompletionInfo<?>>();

        // we must first gather problems; this will let us
        //  handle auto-imports. This seems to be fast enough...
        final RangeMarker marker;
        final Problems problems = Problems.collectFrom(project, file)
                .filterByFixType(ImportsQuickFixDescriptor.class);
        final Problems returnedProblems;
        if (problems.isEmpty()) {
            // nothing to do
            marker = null;
            returnedProblems = null;
        } else if (problems.size() == 1) {
            // let's attempt to resolve it right now
            marker = VimEditor.createRangeMarker(editor);
            returnedProblems = problems;

            try {
                final Problem problem = problems.get(0);
                final List<QuickFixDescriptor> fixes = problem.getFixes();
                if (!fixes.isEmpty()) {
                    final Object result = fixes.get(0).execute(project, editor, file, null);
                    if (null == result && offset != marker.getStartOffset()) {
                        // resolved unambiguously if execution returned null and
                        //  our cursor moved
                        problems.remove(problem);
                    }
                }
            } catch (QuickFixException e) {
                // bummer....
            }
        } else {
            // multiple import problems? User can deal
            marker = null;
            returnedProblems = problems;
        }

        // NB: We can take a shortcut here. If returnedProblems is an empty list,
        //  we're going to restart completion anyway, so don't bother now
        if (returnedProblems == null || !returnedProblems.isEmpty()) {
            performCompletion2(params, prefix, new Consumer<CompletionResult>() {
                @Override
                public void consume(CompletionResult completionResult) {
                    final LookupElement el = completionResult.getLookupElement();
                    final CompletionInfo<?> info = CompletionInfo.from(el);
                    if (info != null)
                        infos.add(info);
                }
            });
        }

        return SimpleResult.success(new CompletionResultInfo(infos, returnedProblems))
                .withOffsetFrom(marker);
    }

    public Iterable<LookupElement> performCompletion2(Object ignore,
            final String prefix, final Consumer<CompletionResult> consumer) {
        final List<LookupElement> results = new ArrayList<LookupElement>();
        final CodeCompletionHandlerBase handler = new CodeCompletionHandlerBase(
                CompletionType.SMART
        ) {

            @Override
            protected void completionFinished(CompletionProgressIndicator indicator, boolean hasModifiers) {
                final List<LookupElement> found = indicator.getLookup().getItems();
                results.addAll(found);
                System.out.println(results);
                myEmptyLookup = found.isEmpty();
                try {
                    super.completionFinished(indicator, hasModifiers);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        };
        final Editor editor = getCompletionEditor();
//        editor.getCaretModel().moveCaretRelatively(prefix.length(), 0, false, false, false);
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                // not supposed to invoke completion with write access
                handler.invokeCompletion(getProject(), editor, 1);
            }
        });
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments(); // to compare with file text

//        LookupElement[] results = getLookupElements();

//        final String prefix = userPrefix != null ? userPrefix : findPrefix(position, parameters.getOffset());
        final CamelHumpMatcher matcher = new CamelHumpMatcher("", false);
        final CompletionSorter sorter = CompletionService.getCompletionService()
                                                         .defaultSorter(null, matcher);
        for (LookupElement e : results) {
            consumer.consume(CompletionResult.wrap(e, matcher, sorter));
        }

        return results;
    }

    private Editor getCompletionEditor() {
        return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(
                myEditor, file);
    }

    @Nullable
    public LookupElement[] getLookupElements() {
        LookupImpl lookup = getLookup();
        if (lookup == null) {
            return myEmptyLookup ? LookupElement.EMPTY_ARRAY : null;
        }
        else {
            final List<LookupElement> list = lookup.getItems();
            return list.toArray(new LookupElement[list.size()]);
        }
    }

    public LookupImpl getLookup() {
        return (LookupImpl) LookupManager.getActiveLookup(myEditor);
    }

    /*
     * The following are mostly imported from CompletionService to get around wacky
     *  UI requirements, and tweaked for sanity
     */
    public LookupElement[] performCompletion(final CompletionParameters parameters,
                                             final String prefix,
                                             final Consumer<CompletionResult> consumer) {


        final Collection<LookupElement> lookupSet = new LinkedHashSet<LookupElement>();

        getVariantsFromContributors(parameters, prefix, null, new Consumer<CompletionResult>() {
            @Override
            public void consume(final CompletionResult result) {
                if (lookupSet.add(result.getLookupElement())
                        && consumer != null) {
                    consumer.consume(result);
                }
            }
        });
        return lookupSet.toArray(new LookupElement[lookupSet.size()]);
    }

    /**
     * Run all contributors until any of them returns false or the list is exhausted. If from parameter is not null, contributors
     * will be run starting from the next one after that.
     */
    public static void getVariantsFromContributors(final CompletionParameters parameters,
                                                   final String prefix, final CompletionContributor from,
                                                   final Consumer<CompletionResult> consumer) {
        final List<CompletionContributor> contributors = CompletionContributor.forParameters(parameters);
        for (int i = contributors.indexOf(from) + 1; i < contributors.size(); i++) {
            final CompletionContributor contributor = contributors.get(i);

            CompletionResultSet result = createResultSet(parameters, prefix, consumer, contributor);
            contributor.fillCompletionVariants(parameters, result);
            if (result.isStopped()) {
                return;
            }
        }

    }

    static CompletionResultSet createResultSet(final CompletionParameters parameters, final String userPrefix,
           final Consumer<CompletionResult> consumer, final CompletionContributor contributor) {
        final PsiElement position = parameters.getPosition();
        final String prefix = userPrefix != null ? userPrefix : findPrefix(position, parameters.getOffset());
        final int lengthOfTextBeforePosition = parameters.getOffset();
        final CamelHumpMatcher matcher = new CamelHumpMatcher(prefix, false);
        final CompletionSorter sorter = CompletionService.getCompletionService().defaultSorter(parameters, matcher);
        return new CompletionResultSetImpl(consumer, lengthOfTextBeforePosition, matcher,
                contributor, parameters, sorter, null);
    }

    @SuppressWarnings("deprecation")
    static String findPrefix(PsiElement position, int offset) {
        // Class is deprecated, but the method seems to be used...
        return CompletionData.findPrefixStatic(position, offset);
    }

}
