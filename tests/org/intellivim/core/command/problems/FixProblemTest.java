package org.intellivim.core.command.problems;

import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;

/**
 * Created by dhleong on 11/12/14.
 */
public class FixProblemTest extends FileEditingTestCase {

    static final String IMPORT_STATEMENT = "import java.util.ArrayList;";

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

        SimpleResult result = (SimpleResult) new GetProblemsCommand(projPath, filePath).execute();
        assertSuccess(result);

        final Problems problems = result.getResult();
        assertSize(1, problems);

        final QuickFixDescriptor quickFix = problems.locateQuickFix("0.0");
        assertNotNull(quickFix);
        assertEquals("Import Class", quickFix.description);

        SimpleResult fixResult = (SimpleResult) new FixProblemCommand(projPath, filePath, quickFix.id).execute();
        assertSuccess(fixResult);
        assertFileNowContains(IMPORT_STATEMENT);
    }

}
