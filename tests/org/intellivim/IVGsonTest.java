package org.intellivim;

import com.google.gson.Gson;
import org.intellivim.core.command.problems.GetProblemsCommand;

/**
 * I would much rather use JUnit 4 and assertj,
 *  but for the sake of homogeneity with the
 *  platform tests that require JUnit 3....
 *
 * Created by dhleong on 11/9/14.
 */
public class IVGsonTest extends BaseTestCase {

    Gson gson;

    public void setUp() throws Exception {
        super.setUp();

        gson = IVGson.newInstance();
    }

    public void testInvalidCommand() {
        try {
            gson.fromJson("{command: 'whatsit'}", ICommand.class);
            fail("Runtime exception should be thrown for invalid command");
        } catch (RuntimeException e) {
            assertEquals("Unknown command `whatsit`", e.getMessage());
        }
    }

    public void testValidCommandMissingProject() {
        try {
            gson.fromJson("{command: 'get_problems', file: 'bleh.java'}", ICommand.class);
            fail("Exception should be thrown for missing project");
        } catch (Exception e) {
            assertError("The `project` field is required", e);
        }
    }

    public void testValidCommandInvalidProject() {
        try {
            gson.fromJson("{command: 'get_problems', project: 'bleh', file: 'bleh.java'}",
                    ICommand.class);
            fail("Exception should be thrown for invalid project");
        } catch (Exception e) {
            assertError("Couldn't find project at bleh", e);
        }
    }

    public void testValidCommand() {
        String projectPath = getProjectPath(JAVA_PROJECT);
        String json = "{command: 'get_problems', project: '"
                + projectPath + "', file: 'src/SomeClass.java'}";
        ICommand command = gson.fromJson(json, ICommand.class);
        assertTrue("Parsed command should be a GetProblemsCommand",
                command instanceof GetProblemsCommand);
    }

    static void assertError(String expectedMessage, Throwable error) {
        final String actual = error.getMessage();
        if (actual.endsWith(expectedMessage))
            return;

        assertEquals(expectedMessage, actual);
    }

}
