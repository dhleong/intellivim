package org.intellivim.core.command.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;

/**
 * @author dhleong
 */
@Command("get_implementables")
public class GetImplementablesCommand extends ProjectCommand {

    @Required @Inject PsiFile file;
    @Required int offset;

    public GetImplementablesCommand(Project project, String file, int offset) {
        super(project);

        this.file = ProjectUtil.getPsiFile(project, file);
        this.offset = offset;
    }

    @Override
    public Result execute() {

        final Implementables implementables;
        try {
            implementables =
                    Implementables.collectFrom(project, file, offset);
        } catch (IllegalArgumentException e) {
            return SimpleResult.error(e);
        }

        if (implementables.isEmpty()) {
            return SimpleResult.error("Nothing to implement");
        }

        return SimpleResult.success(implementables);
    }
}
