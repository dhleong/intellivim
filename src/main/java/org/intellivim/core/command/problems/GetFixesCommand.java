package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

import java.util.Collections;

/**
 * Created by dhleong on 11/8/14.
 */
@Command("get_fixes")
public class GetFixesCommand extends ProjectCommand {

    @Required String file;
    @Required int offset;

    public GetFixesCommand(Project project, String filePath, int offset) {
        super(project);
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