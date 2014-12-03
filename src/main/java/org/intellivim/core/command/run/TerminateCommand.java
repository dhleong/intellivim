package org.intellivim.core.command.run;

import com.intellij.openapi.project.Project;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

/**
 * @author dhleong
 */
@Command("terminate")
public class TerminateCommand extends ProjectCommand {

    /* Optional */String id;

    public TerminateCommand(Project project, String id) {
        super(project);

        this.id = id;
    }

    @Override
    public Result execute() {
        if (id == null)
            LaunchManager.terminateAll();
        else
            LaunchManager.terminate(id);
        return SimpleResult.success();
    }
}
