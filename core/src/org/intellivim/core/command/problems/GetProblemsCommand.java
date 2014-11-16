package org.intellivim.core.command.problems;

import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

/**
 * Created by dhleong on 11/8/14.
 */
@Command("get_problems")
public class GetProblemsCommand extends ProjectCommand {

    @Required String file;

    public GetProblemsCommand(String projectPath, String filePath) {
        project = ProjectUtil.ensureProject(projectPath);
        file = filePath;
    }

    @Override
    public Result execute() {
        final Problems problems = Problems.collectFrom(project, file);
        return SimpleResult.success(problems);
    }
}
