package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.intellivim.Command;
import org.intellivim.ICommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;

/**
 * Created by dhleong on 11/11/14.
 */
@Command("quickfix")
public class FixProblemCommand implements ICommand {

    @Required Project project;
    @Required String file;
    @Required String fixId;

    public FixProblemCommand(String projPath, String filePath, String fixId) {
        project = ProjectUtil.getProject(projPath);
        file = filePath;
        this.fixId = fixId;
    }

    @Override
    public Result execute() {

        final VirtualFile virtualFile = ProjectUtil.getVirtualFile(project, file);

        final Problems problems = Problems.collectFrom(project, virtualFile);
        final QuickFixDescriptor fix = problems.locateQuickFix(fixId);

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        VimEditor editor = new VimEditor(project, psiFile, 0);

        fix.execute(project, editor, psiFile);
        return SimpleResult.success();
    }
}
