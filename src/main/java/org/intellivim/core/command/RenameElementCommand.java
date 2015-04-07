package org.intellivim.core.command;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
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

        void addRenamed(final PsiElement element) {
            System.out.println("Renamed: " + element);
            System.out.println("containedIn:" + element.getContainingFile());
            System.out.println("   original:" + original);
            final PsiFile renamedFile = element.getContainingFile();
            if (original instanceof PsiClass
                    && !element.getContainingFile().equals(renamedFile)) {
                renamed.put(
                    pathOf(original.getContainingFile()),
                    pathOf(renamedFile));
            } else {
                changed.add(pathOf(renamedFile));
            }
        }

        private static String pathOf(PsiFile file) {
            return file.getVirtualFile().getCanonicalPath();
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

        // be up to date, please
        // (if we delete outside of IntelliJ, it gets confused)
        file.getContainingDirectory()
            .getVirtualFile()
            .refresh(false, true);

        final RenameResult result = new RenameResult(element);
        IntelliVimUtil.runWriteCommand(new Runnable() {

            @Override
            public void run() {
                processor.renameElement(element, rename, usages, new RefactoringElementListener() {
                    @Override
                    public void elementMoved(final PsiElement psiElement) {
                        System.out.println("Moved: " + psiElement);
                    }

                    @Override
                    public void elementRenamed(final PsiElement psiElement) {
                        result.addRenamed(psiElement);
                    }
                });

            }
        });

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
        System.out.println("usages=" + filtered);
        return filtered.toArray(new UsageInfo[filtered.size()]);
    }

}
