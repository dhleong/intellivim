package org.intellivim.core.command.find;

import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.usages.FindUsagesProcessPresentation;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageSearcher;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhleong
 */
@Command("find_usages")
public class FindUsagesCommand extends ProjectCommand {

    static class UsageCollectingViewManager
            extends UsageViewManager
            implements Processor<Usage> {

        private final List<Usage> results;

        public UsageCollectingViewManager(final List<Usage> results) {
            this.results = results;
        }

        @NotNull
        @Override
        public UsageView createUsageView(final UsageTarget[] searchedFor, final Usage[] foundUsages,
                final UsageViewPresentation presentation,
                final Factory<UsageSearcher> usageSearcherFactory) {
            return showUsages(searchedFor, foundUsages, presentation);
        }

        @NotNull
        @Override
        public UsageView showUsages(final UsageTarget[] searchedFor, final Usage[] foundUsages,
                final UsageViewPresentation presentation,
                final Factory<UsageSearcher> factory) {
            return showUsages(searchedFor, foundUsages, presentation);
        }

        @NotNull
        @Override
        public UsageView showUsages(final UsageTarget[] searchedFor, final Usage[] foundUsages,
                final UsageViewPresentation presentation) {
            for (Usage usage : foundUsages) {
                process(usage);
            }

            // my initial tests don't seem to use this anyway....
            return null;
        }

        @Nullable
        @Override
        public UsageView searchAndShowUsages(final UsageTarget[] searchFor,
                final Factory<UsageSearcher> searcherFactory,
                final boolean showPanelIfOnlyOneUsage, final boolean showNotFoundMessage,
                final UsageViewPresentation presentation, final UsageViewStateListener listener) {
            searchAndShowUsages(searchFor, searcherFactory, null, presentation, listener);
            return null;
        }

        @Override
        public void searchAndShowUsages(final UsageTarget[] searchFor, final Factory<UsageSearcher> searcherFactory,
                final FindUsagesProcessPresentation processPresentation, final UsageViewPresentation presentation,
                final UsageViewStateListener listener) {
            searcherFactory.create().generate(this);
        }

        @Nullable
        @Override
        public UsageView getSelectedUsageView() {
            return null;
        }

        @Override
        public boolean process(final Usage usage) {
            results.add(usage);
            return true;
        }
    }

    @Required @Inject PsiFile file;
    @Required int offset;

    public FindUsagesCommand(final Project project, final String filePath, final int offset) {
        super(project);

        file = ProjectUtil.getPsiFile(project, filePath);
        this.offset = offset;
    }

    @Override
    public Result execute() {

        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return SimpleResult.error("No element at position");
        }

        final Editor editor = VimEditor.from(this, file, offset);
        List<Usage> rawResults = findUsages(editor);
        if (rawResults.isEmpty()) {
            return SimpleResult.error("No usages found");
        }

        List<LocationResult> results = ContainerUtil.map(rawResults, new Function<Usage, LocationResult>() {
            @Override
            public LocationResult fun(final Usage usage) {

                if (usage instanceof UsageInfo2UsageAdapter) {
                    PsiElement element = ((UsageInfo2UsageAdapter) usage).getElement();
                    if (element != null) {
                        return new LocationResult(element);
                    }
                }
                return null;
            }
        });

        return SimpleResult.success(ContainerUtil.filter(results, Condition.NOT_NULL));
    }

    public static List<Usage> findUsages(final Editor editor) {
        final PsiElement element = VimEditor.findTargetElement(editor);

        final List<Usage> rawResults = new ArrayList<Usage>();
        final FindUsagesManager manager = new FindUsagesManager(editor.getProject(),
                new UsageCollectingViewManager(rawResults));
        manager.findUsages(element, null, null, false, null);
        return rawResults;
    }
}
