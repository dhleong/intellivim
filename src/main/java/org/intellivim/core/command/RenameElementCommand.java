package org.intellivim.core.command;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.find.FindUsagesCommand;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.FileUtil;
import org.intellivim.core.util.IntelliVimUtil;
import org.intellivim.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Returns a map:
 * <code>
 *  {
 *      changed: [
 *          'src/path/to/ModifiedFile.java'
 *      ]
 *    , renamed: {
 *        'src/original/Name.java': 'src/updated/FileName.java'
 *      }
 *  }
 * </code>
 * @author dhleong
 */
@Command("rename_element")
public class RenameElementCommand extends ProjectCommand {

    public static class RenameResult {
        public final Set<String> changed;
        public final Map<String, String> renamed;

        private final transient PsiElement original;

        private RenameResult(final PsiElement element) {
            this.changed = new HashSet<String>();
            this.renamed = new HashMap<String, String>();

            original = element;
        }

        void addUsage(final PsiElement element) {
            final PsiFile renamedFile = element.getContainingFile();
            final String renamedPath = pathOf(renamedFile);
            if (!renamed.containsValue(renamedPath)) {
                changed.add(renamedPath);
            }
        }
    }

    @Required @Inject PsiFile file;
    @Required int offset;
    @Required String rename;

    public RenameElementCommand(final Project project, PsiFile file,
            int offset, String rename) {
        super(project);
        
        this.file = file;
        this.offset = offset;
        this.rename = rename;
    }

    @Override
    public Result execute() {
        final Editor editor = new VimEditor(project, file, offset);
        final PsiElement element = TargetElementUtilBase.findTargetElement(editor,
                TargetElementUtilBase.getInstance().getAllAccepted());
        final RenamePsiElementProcessor processor =
                RenamePsiElementProcessor.forElement(element);
        final UsageInfo[] usages = gatherUsages(project, file, offset);

        final String elementPath = pathOf(element.getContainingFile());

        // be up to date, please
        // (if we delete outside of IntelliJ, it gets confused)
        file.getContainingDirectory()
            .getVirtualFile()
            .refresh(false, true);

        final RenameResult result = new RenameResult(element);
        IntelliVimUtil.runWriteCommand(new Runnable() {

            @Override
            public void run() {
                processor.renameElement(element, rename, usages, RefactoringElementListener.DEAF);
            }
        });

        // was the element's file renamed?
        final String updatedElementPath = pathOf(element.getContainingFile());
        if (!elementPath.equals(updatedElementPath)) {
            result.renamed.put(elementPath, updatedElementPath);
        }

        for (final UsageInfo usage : usages) {
            final PsiElement el = usage.getElement();
            if (el != null) {
                result.addUsage(usage.getElement());
            }
        }

        FileUtil.commitChanges(editor);
        return SimpleResult.success(result);
    }

    private UsageInfo[] gatherUsages(final Project project,
            final PsiFile file, final int offset) {
        List<Usage> usages = FindUsagesCommand.findUsages(project, file, offset);

        List<UsageInfo> results = ContainerUtil.map(usages, new Function<Usage, UsageInfo>() {
            @Override
            public UsageInfo fun(final Usage usage) {

                if (usage instanceof UsageInfo2UsageAdapter) {
                    return ((UsageInfo2UsageAdapter) usage).getUsageInfo();
                }
                return null;
            }
        });

        final List<UsageInfo> filtered = ContainerUtil.filter(results, Condition.NOT_NULL);
        return filtered.toArray(new UsageInfo[filtered.size()]);
    }

    static String pathOf(PsiFile file) {
        return file.getVirtualFile().getCanonicalPath();
    }
}
