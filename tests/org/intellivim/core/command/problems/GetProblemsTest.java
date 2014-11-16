package org.intellivim.core.command.problems;

import org.intellivim.BaseTestCase;
import org.intellivim.CommandExecutor;
import org.intellivim.IVGson;
import org.intellivim.SimpleResult;

/**
 * Created by dhleong on 11/8/14.
 */
public class GetProblemsTest extends BaseTestCase {
    public void testProblematic() {
        String projPath = getProjectPath(JAVA_PROJECT);
        String filePath = "src/org/intellivim/javaproject/Problematic.java";

        SimpleResult result = (SimpleResult) new GetProblemsCommand(projPath, filePath).execute();
        assertSuccess(result);
        assertNotNull(result.result);

        Problems problems = (Problems) result.result;
        assertSize(1, problems);
    }

    public void testProblematicFromExecutor() {
        String projPath = getProjectPath(JAVA_PROJECT);
        String filePath = "src/org/intellivim/javaproject/Problematic.java";
        String command = "{'command':'get_problems'," +
                "'project': '" + projPath + "'," +
                "'file': '" + filePath + "'}";

        CommandExecutor ex = new CommandExecutor(IVGson.newInstance());

        SimpleResult result = (SimpleResult) ex.execute(command);
        assertSuccess(result);
        assertNotNull(result.result);

        Problems problems = (Problems) result.result;
        assertSize(1, problems);
    }
}
