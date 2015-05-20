package org.intellivim.java.command;

import com.intellij.openapi.project.Project;
import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.problems.FixProblemCommand;
import org.intellivim.core.command.problems.FixProblemTest;
import org.intellivim.core.command.problems.Problems;
import org.intellivim.core.command.problems.QuickFixDescriptor;
import org.intellivim.core.util.ProjectUtil;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class OptimizeImportsTest extends FileEditingTestCase {

    final String projPath = getProjectPath(JAVA_PROJECT);
    final String filePath = PROBLEMATIC_FILE_PATH;

    @Override
    protected String getProjectPath() {
        return projPath;
    }

    @Override
    protected String getFilePath() {
        return filePath;
    }

    /**
     * The actual test
     */
    public void testCommand() throws IOException {

        SimpleResult result = execute(new OptimizeImportsCommand(getProject(), filePath));
        assertSuccess(result);
        assertFileNowContains(FixProblemTest.IMPORT_STATEMENT);
        assertFileNowContains("import org.intellivim.javaproject.subpackage.AlsoNotImported;");
    }

    /**
     * To resolve multiple ambiguous imports during OptimizeImportsCommand,
     *  we need to be able to use ids from an old, cached Problems instance
     *  to find updated QuickFixDescriptors. This is a test of that
     */
    public void testProblemsEquivalence() {
        Project project = getProject();
        Problems original = Problems.collectFrom(project, ProjectUtil.getPsiFile(project, filePath));

        Problems updated = Problems.collectFrom(project, ProjectUtil.getPsiFile(project, filePath));
        assertThat(updated)
                .hasSize(2)
                .hasSameSizeAs(original);

        assertThat(updated.locateQuickFix(original.locateQuickFix("0.0"))).isNotNull();

        assertSuccess(execute(new FixProblemCommand(project, filePath, "0.0")));
        assertFileNowContains(FixProblemTest.IMPORT_STATEMENT);

        final QuickFixDescriptor secondImportFix = original.locateQuickFix("1.0");
        Problems afterFix = Problems.collectFrom(project, ProjectUtil.getPsiFile(project, filePath));
        assertThat(afterFix).hasSize(1);

        // first, we should be able to find it from contains()
        assertThat(afterFix.get(0).getFixes())
            .contains(secondImportFix);

        // now, make sure locateQuickFix actually can
        assertThat(afterFix.locateQuickFix(original.locateQuickFix("1.0"))).isNotNull();
    }


}
