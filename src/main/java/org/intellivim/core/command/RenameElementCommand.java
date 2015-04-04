package org.intellivim.core.command;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.inject.Inject;

/**
 * @author dhleong
 */
@Command("rename_element")
public class RenameElementCommand extends ProjectCommand {

    @Required @Inject PsiFile file;
    @Required @Inject PsiElement element;
    @Required String rename;

    public RenameElementCommand(final Project project, PsiFile file,
            PsiElement element, String rename) {
        super(project);
        
        this.file = file;
        this.element = element;
        this.rename = rename;
    }

    @Override
    public Result execute() {
        final RenamePsiElementProcessor processor =
                RenamePsiElementProcessor.forElement(element);
        final UsageInfo[] usages = null; // TODO
        processor.renameElement(element, rename, usages, new RefactoringElementListener() {
            @Override
            public void elementMoved(final PsiElement psiElement) {

            }

            @Override
            public void elementRenamed(final PsiElement psiElement) {

            }
        });
        return null;
    }
}
