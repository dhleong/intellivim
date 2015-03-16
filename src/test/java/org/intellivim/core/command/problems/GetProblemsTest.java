package org.intellivim.core.command.problems;

import org.intellivim.BaseTestCase;
import org.intellivim.CommandExecutor;
import org.intellivim.IVGson;
import org.intellivim.SimpleResult;

/**
 * @author dhleong
 */
public class GetProblemsTest extends BaseTestCase {

    String filePath = PROBLEMATIC_FILE_PATH;

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testProblematic() {
        SimpleResult result = (SimpleResult) new GetProblemsCommand(getProject(), filePath).execute();
        assertSuccess(result);
        assertNotNull(result.result);

        Problems problems = (Problems) result.result;
        assertSize(2, problems);
    }

    public void testProblematicFromExecutor() {
        String command = "{'command':'get_problems'," +
                "'project': '" + getProjectPath() + "'," +
                "'file': '" + filePath + "'}";

        CommandExecutor ex = new CommandExecutor(IVGson.newInstance());

        SimpleResult result = (SimpleResult) ex.execute(command);
        assertSuccess(result);
        assertNotNull(result.result);

        Problems problems = (Problems) result.result;
        assertSize(2, problems);
    }
}
