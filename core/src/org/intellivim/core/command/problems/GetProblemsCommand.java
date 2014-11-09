package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import org.intellivim.core.Result;
import org.intellivim.core.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

/**
 * Created by dhleong on 11/8/14.
 */
public class GetProblemsCommand {

    public Result execute(String projectPath, String filePath) {
        final Project project = ProjectUtil.ensureProject(projectPath);
        final Problems problems = Problems.collectFrom(project, filePath);
        return SimpleResult.success(problems);
    }
}
