package org.intellivim.core.command.problems;

import org.intellivim.*;
import org.intellivim.core.util.ProjectUtil;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by dhleong on 11/8/14.
 */
@Command("get_fixes")
public class GetFixesCommand extends ProjectCommand {

    @Required String file;
    @Required int offset;

    public GetFixesCommand(String projectPath, String filePath, int offset) {
        project = ProjectUtil.ensureProject(projectPath);
        file = filePath;
        this.offset = offset;
    }

    @Override
    public Result execute() {
        final Problems problems = Problems.collectFrom(project, file);
        for (Problem p : problems) {
            if (p.containsOffset(offset)) {
                return SimpleResult.success(p.getFixes());
            }
        }
        return SimpleResult.success(Collections.EMPTY_LIST);
    }
}
