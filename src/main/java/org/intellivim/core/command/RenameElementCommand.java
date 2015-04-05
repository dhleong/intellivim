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

import java.util.List;

/**
 * @author dhleong
 */
@Command("rename_element")
public class RenameElementCommand extends ProjectCommand {

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
                        System.out.println("Renamed: " + psiElement);
                    }
                });

            }
        });

        FileUtil.commitChanges(editor);

        // TODO return a list of modified files?
        return SimpleResult.success();
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
