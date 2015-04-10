package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

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
        SimpleResult result = fixAt(0);
        assertSuccess(result);
        assertThat(result.result).isNull();
    }

    public void testProblematic() {
        Problems problems = loadProblems();
        Problem problem = problems.get(0);
        QuickFixDescriptor fix = problem.getFixes().get(0);

        SimpleResult result = fixAt(fix.start);
        assertSuccess(result);

        List<QuickFixDescriptor> fixes = result.getResult();
        assertThat(fixes)
                .isNotNull()
                .isNotEmpty();
    }

    public void testClosestOnLine() {
        Problems problems = loadProblems();
        Problem problem = problems.get(0);
        QuickFixDescriptor fix = problem.getFixes().get(0);

        SimpleResult result = fixAt(fix.start - 2);
        assertSuccess(result);

        List<QuickFixDescriptor> fixes = result.getResult();
        assertThat(fixes)
                .isNotNull()
                .isNotEmpty();
    }

    Problems loadProblems() {
        return execute(new GetProblemsCommand(getProject(), filePath)).getResult();
    }

    SimpleResult fixAt(int offset) {
        final Project project = getProject();
        final PsiFile file = ProjectUtil.getPsiFile(project, filePath);
        return execute(new GetFixesCommand(project, file, offset));
    }
}
