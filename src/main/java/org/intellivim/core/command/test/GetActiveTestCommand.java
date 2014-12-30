package org.intellivim.core.command.test;

import com.intellij.openapi.project.Project;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

/**
 * Get the currently-running Test root node.
 *  Async communication with Vim, for example, is done
 *  with the command-line; a full test tree for a large
 *  project may be too much to pass on commandline,
 *  so this lets clients grab it with a normal command
 *
 * @author dhleong
 */
@Command("get_active_test")
public class GetActiveTestCommand extends ProjectCommand {

    public GetActiveTestCommand(final Project project) {
        super(project);
    }

    @Override
    public Result execute() {
        TestNode activeTest = ActiveTestManager.getActiveTestRoot(project);
        if (activeTest != null)
            return SimpleResult.success(activeTest);

        return SimpleResult.error("No active test");
    }
}
