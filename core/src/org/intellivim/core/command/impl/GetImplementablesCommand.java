package org.intellivim.core.command.impl;

import com.intellij.openapi.project.Project;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

/**
 * @author dhleong
 */
@Command("get_implementables")
public class GetImplementablesCommand extends ProjectCommand {

    @Required String file;
    @Required int offset;

    public GetImplementablesCommand(Project project, String file, int offset) {
        super(project);

        this.file = file;
        this.offset = offset;
    }

    @Override
    public Result execute() {

        final Implementables implementables =
                Implementables.collectFrom(project, file, offset);

        if (implementables.isEmpty()) {
            return SimpleResult.error("Nothing to implement");
        }

        return SimpleResult.success(implementables);
    }
}
