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

        SimpleResult result = (SimpleResult) new CompleteCommand(
                projPath, filePath, offset).execute();
        assertSuccess(result);

        List<CompletionInfo> infoList = (List<CompletionInfo>) result.result;
        assertSize(3, infoList);

        CompletionInfo first = infoList.get(0);
        assertNotNull(first);
        assertEquals("boring", first.body);
        assertEquals("()-> void", first.detail);
        assertEquals("", first.doc);

        CompletionInfo second = infoList.get(1);
        assertNotNull(second);
        assertEquals("notBoring", second.body);
        assertEquals("(int number)-> void", second.detail);
        assertEquals("/** I promise it's not boring */", second.doc);

        CompletionInfo last = infoList.get(2);
        assertNotNull(last);
        assertEquals("fluid", last.body);
        assertEquals("()-> Dummy", last.detail);
        assertEquals("", last.doc);
    }

}
