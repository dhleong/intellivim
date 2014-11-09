package org.intellivim.core.command.problems;

import org.intellivim.core.BaseTestCase;
import org.intellivim.core.SimpleResult;
import org.intellivim.core.command.problems.GetProblemsCommand;

/**
 * Created by dhleong on 11/8/14.
 */
public class GetProblemsTest extends BaseTestCase {
    public void testProblematic() {
        String projPath = getProjectPath(JAVA_PROJECT);
        String filePath = "src/org/intellivim/javaproject/Problematic.java";

        SimpleResult result = (SimpleResult) new GetProblemsCommand().execute(projPath, filePath);
        assertSuccess(result);
        assertNotNull(result.result);

        Problems problems = (Problems) result.result;
        assertSize(1, problems);
    }
}
