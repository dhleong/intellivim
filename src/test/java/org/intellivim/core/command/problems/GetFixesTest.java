package org.intellivim.core.command.problems;

import org.intellivim.BaseTestCase;
import org.intellivim.CommandExecutor;
import org.intellivim.IVGson;
import org.intellivim.SimpleResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class GetFixesTest extends BaseTestCase {

    String filePath = PROBLEMATIC_FILE_PATH;

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testNoProblemReturnsNull() {
        SimpleResult result = (SimpleResult) new GetFixesCommand(getProject(), filePath, 0).execute();
        assertSuccess(result);
        assertThat(result.result).isNull();
    }

    public void testProblematic() {
        Problems problems = ((SimpleResult) new GetProblemsCommand(getProject(), filePath).execute()).getResult();
        Problem problem = problems.get(0);
        QuickFixDescriptor fix = problem.getFixes().get(0);

        SimpleResult result = (SimpleResult) new GetFixesCommand(getProject(), filePath, fix.start).execute();
        assertSuccess(result);

        List<QuickFixDescriptor> fixes = result.getResult();
        assertThat(fixes)
                .isNotNull()
                .isNotEmpty();
    }

    public void testClosestOnLine() {
        Problems problems = ((SimpleResult) new GetProblemsCommand(getProject(), filePath).execute()).getResult();
        Problem problem = problems.get(0);
        QuickFixDescriptor fix = problem.getFixes().get(0);

        SimpleResult result = (SimpleResult) new GetFixesCommand(getProject(), filePath, fix.start - 2).execute();
        assertSuccess(result);

        List<QuickFixDescriptor> fixes = result.getResult();
        assertThat(fixes)
                .isNotNull()
                .isNotEmpty();
    }
}
