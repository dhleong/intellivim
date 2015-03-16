package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;

/**
 * @author dhleong
 */
public class FixProblemTest extends FileEditingTestCase {

//    public static final String IMPORT_STATEMENT = "import java.util.ArrayList;";
    public static final String IMPORT_STATEMENT = "import org.intellivim.javaproject.subpackage.NotImported;";

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
        SimpleResult result = (SimpleResult) new GetProblemsCommand(project, filePath).execute();
        assertSuccess(result);

        final Problems problems = result.getResult();
        assertSize(2, problems);

        final QuickFixDescriptor quickFix = problems.locateQuickFix("0.0");
        assertNotNull(quickFix);
        assertEquals("Import Class", quickFix.description);

        SimpleResult fixResult = (SimpleResult) new FixProblemCommand(project, filePath, quickFix.id).execute();
        assertSuccess(fixResult);
        assertFileNowContains(IMPORT_STATEMENT);
    }

}
