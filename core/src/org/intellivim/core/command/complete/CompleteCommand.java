package org.intellivim.core.command.complete;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionData;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by dhleong on 11/3/14.
 */
@Command("complete")
public class CompleteCommand extends ProjectCommand {

    @Required String file;
    @Required int offset;

    public CompleteCommand(Project project, String filePath, int offset) {
        super(project);
        file = filePath;
        this.offset = offset;
    }

    public Result execute() {
        final VirtualFile virtualFile = ProjectUtil.getVirtualFile(project, file);

        final CompletionParameters params = CompletionParametersUtil.from(project, virtualFile, offset);
        final LookupElement[] results = performCompletion(params, null);
        ArrayList<CompletionInfo> infos = new ArrayList<CompletionInfo>(results.length);
        for (LookupElement el : results) {
//            System.out.println("result: " + el.getPsiElement() + " / " + el + " / " + el.getClass());

            CompletionInfo info = CompletionInfo.from(el);
            if (info != null)
                infos.add(info);
        }

        return SimpleResult.success(infos);
    }

    /*
     * The following are mostly imported from CompletionService to get around wacky
     *  UI requirements, and tweaked for sanity
     */
    public LookupElement[] performCompletion(final CompletionParameters parameters,
                                             final Consumer<CompletionResult> consumer) {
        final Collection<LookupElement> lookupSet = new LinkedHashSet<LookupElement>();

        getVariantsFromContributors(parameters, null, new Consumer<CompletionResult>() {
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
     * @param parameters
     * @param from
     * @param consumer
     * @return
     */
    public static void getVariantsFromContributors(final CompletionParameters parameters,
                                            final CompletionContributor from,
                                            final Consumer<CompletionResult> consumer) {
        final List<CompletionContributor> contributors = CompletionContributor.forParameters(parameters);
        for (int i = contributors.indexOf(from) + 1; i < contributors.size(); i++) {
            final CompletionContributor contributor = contributors.get(i);

            CompletionResultSet result = createResultSet(parameters, consumer, contributor);
            contributor.fillCompletionVariants(parameters, result);
            if (result.isStopped()) {
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


}
