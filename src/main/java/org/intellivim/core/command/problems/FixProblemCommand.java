package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;

/**
 * @author dhleong
 */
@Command("quickfix")
public class FixProblemCommand extends ProjectCommand {

    @Required @Inject PsiFile file;
    @Required String fixId;

    public FixProblemCommand(Project project, String filePath, String fixId) {
        super(project);
        file = ProjectUtil.getPsiFile(project, filePath);
        this.fixId = fixId;
    }

    @Override
    public Result execute() {

        final Problems problems = Problems.collectFrom(project, file);
        final QuickFixDescriptor fix = problems.locateQuickFix(fixId);

        VimEditor editor = new VimEditor(project, file, 0);

        fix.execute(project, editor, file);
        return SimpleResult.success();
    }
}
