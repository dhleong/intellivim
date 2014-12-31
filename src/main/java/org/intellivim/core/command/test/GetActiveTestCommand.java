package org.intellivim.core.command.test;

import org.intellivim.Command;
import org.intellivim.ICommand;
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
public class GetActiveTestCommand implements ICommand {

    /** if true, will return the "last" test if none active */
    boolean lazy = false;

    @Override
    public Result execute() {
        final TestNode activeTest = ActiveTestManager.getActiveTestRoot(null);
        if (activeTest != null) {
            return SimpleResult.success(activeTest);
        }

        final TestNode lastTest = ActiveTestManager.getLastTestRoot(null);
        if (lazy && lastTest != null) {
            System.out.println("Got lazy test! " + lastTest);
            return SimpleResult.success(lastTest);
        }

        return SimpleResult.error("No active test");
    }
}
