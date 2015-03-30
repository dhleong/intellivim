package org.intellivim.core.command.find;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

import java.util.List;

/**
 * @author dhleong
 */
public class FindUsagesTest extends BaseTestCase {

    final String filePath = "src/org/intellivim/javaproject/Dummy.java";

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testFindUsagesOfNotBoring() {
        SimpleResult result = locateAt(521);
        assertSuccess(result);

        List<LocationResult> loc = result.getResult();
        assertNotNull(loc);

        // TODO
    }

    // TODO find other usages when targeting a usage

    SimpleResult locateAt(int offset) {

        return (SimpleResult) new FindUsagesCommand(getProject(),
                filePath, offset).execute();
    }
}
