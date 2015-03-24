package org.intellivim.core.command;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.find.FindDeclarationCommand;
import org.intellivim.core.command.find.LocationResult;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class FindDeclarationTest extends BaseTestCase {

    final String filePath = "src/org/intellivim/javaproject/Dummy.java";

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testFindProblematicInDummy() {
        SimpleResult result = locateAt(484);
        assertSuccess(result);

        LocationResult loc = result.getResult();
        assertNotNull(loc);

        assertThat(loc.file).endsWith("Problematic.java");
        assertThat(new File(loc.file)).exists();
        assertThat(loc.offset).isEqualTo(105);
    }

    SimpleResult locateAt(int offset) {

        return (SimpleResult) new FindDeclarationCommand(getProject(),
                filePath, offset).execute();
    }
}
