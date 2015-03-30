package org.intellivim.core.command.find;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class FindUsagesTest extends BaseTestCase {

    final String filePath = "src/org/intellivim/javaproject/Dummy.java";

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testFindUsagesFromDeclaration() {
        SimpleResult result = locateAt(521); // [n]otBoring(int, string)
        assertSuccess(result);

        final List<LocationResult> results = result.getResult();
        assertThat(results)
                .isNotNull()
                .hasSize(2);
    }

    public void testFindUsagesFromUsage() {
        SimpleResult result = locateAt(891); // [n]otBoring(42, "foo")
        assertSuccess(result);

        final List<LocationResult> results = result.getResult();
        assertThat(results)
                .isNotNull()
                .hasSize(2);
        System.out.println(results);
    }

    SimpleResult locateAt(int offset) {

        return (SimpleResult) new FindUsagesCommand(getProject(),
                filePath, offset).execute();
    }
}
