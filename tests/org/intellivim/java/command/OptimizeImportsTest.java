package org.intellivim.java.command;

import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.problems.FixProblemTest;
import org.intellivim.java.command.OptimizeImportsCommand;

import java.io.IOException;

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

        SimpleResult result = (SimpleResult) new OptimizeImportsCommand(getProject(), filePath).execute();
        assertSuccess(result);
        assertFileNowContains(FixProblemTest.IMPORT_STATEMENT);
    }

}
