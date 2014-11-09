package org.intellivim.core.command.complete;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

import java.util.List;

/**
 * Created by dhleong on 11/7/14.
 */
public class CompletionTest extends BaseTestCase {

    public void testCompletion() {
        String projPath = getProjectPath(JAVA_PROJECT);
        String filePath = "src/org/intellivim/javaproject/Dummy.java";
        int offset = 243;

        SimpleResult result = (SimpleResult) new CompleteCommand().execute(projPath, filePath, offset);
        assertSuccess(result);

        List<CompletionInfo> infoList = (List<CompletionInfo>) result.result;
        assertTrue("Expected results", infoList.size() > 0);
    }

}
