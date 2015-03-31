package org.intellivim.java.command;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.find.LocationResult;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class JavaNewTest extends BaseTestCase {

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testNewFQN() {
        SimpleResult result = (SimpleResult) new JavaNewCommand(getProject(),
                "class", "org.intellivim.test.NewlyCreated").execute();
        assertSuccess(result);

        LocationResult location = result.getResult();
        System.out.println("Created: " + location);
        assertThat(location).isNotNull();

        File createdFile = new File(location.file);
        assertThat(createdFile).exists();

        // cleanup
        createdFile.delete();
    }
}
