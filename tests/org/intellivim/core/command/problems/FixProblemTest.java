package org.intellivim.core.command.problems;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

/**
 * Created by dhleong on 11/12/14.
 */
public class FixProblemTest extends BaseTestCase {

    public void testExecute() {
        String projPath = getProjectPath(JAVA_PROJECT);
        String filePath = "src/org/intellivim/javaproject/Problematic.java";
        SimpleResult result = (SimpleResult) new GetProblemsCommand(projPath, filePath).execute();
        assertSuccess(result);

        Problems problems = (Problems) result.result;
        assertSize(1, problems);

        QuickFixDescriptor quickFix = problems.locateQuickFix("0.0");
        assertNotNull(quickFix);

        SimpleResult fixResult = (SimpleResult) new FixProblemCommand(projPath, filePath, quickFix.id).execute();
        assertSuccess(fixResult);
    }
}
