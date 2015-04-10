package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class FixProblemTest extends FileEditingTestCase {

//    public static final String IMPORT_STATEMENT = "import java.util.ArrayList;";
    public static final String IMPORT_STATEMENT = "import org.intellivim.javaproject.subpackage.NotImported;";

    /** Offset of cursor when on top of [N]otImported before the problem fix */
    private static final int OFFSET_BEFORE = 152;
    /** Offset of cursor after the problem fix */
    private static final int OFFSET_AFTER = 211;

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

    public void testExecute() {
        // clean slate
        assertFileDoesNotContain(IMPORT_STATEMENT);

        Project project = getProject();
        SimpleResult result = execute(new GetProblemsCommand(project, filePath));
        assertSuccess(result);

        final Problems problems = result.getResult();
        assertThat(problems).hasSize(2);

        final QuickFixDescriptor quickFix = problems.locateQuickFix("0.0");
        assertNotNull(quickFix);
        assertThat(quickFix.description).isEqualToIgnoringCase("Import Class");

        FixProblemCommand command = new FixProblemCommand(project, filePath, quickFix.id);
        command.offset = OFFSET_BEFORE;
        SimpleResult fixResult = execute(command);
        assertSuccess(fixResult);
        assertFileNowContains(IMPORT_STATEMENT);

        assertThat(fixResult.getNewOffset())
            .isGreaterThan(0)
            .isEqualTo(OFFSET_AFTER);
    }

}
