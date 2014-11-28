package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

/**
 * Created by dhleong on 11/8/14.
 */
@Command("get_problems")
public class GetProblemsCommand extends ProjectCommand {

    @Required String file;

    public GetProblemsCommand(Project project, String filePath) {
        super(project);
        file = filePath;
    }

    @Override
    public Result execute() {
        final Problems problems = Problems.collectFrom(project, file);
        return SimpleResult.success(problems);
    }
}
