package org.intellivim;

import com.google.gson.Gson;
import org.assertj.core.api.ObjectAssert;
import org.intellivim.core.command.problems.GetProblemsCommand;
import org.intellivim.core.command.run.AsyncRunner;
import org.intellivim.core.command.run.DummyRunner;
import org.intellivim.core.command.run.RunCommand;
import org.intellivim.core.command.run.VimAsyncRunner;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * I would much rather use JUnit 4 for exceptions here,
 *  but for the sake of homogeneity with the
 *  platform tests that require JUnit 3....
 *
 * @author dhleong
 */
public class IVGsonTest extends BaseTestCase {

    Gson gson;

    @Override
    protected String getProjectPath() {
        return null; // NB unused here
    }

    public void setUp() throws Exception {
        super.setUp();

        gson = IVGson.newInstance();
    }

    public void testInvalidCommand() {
        try {
            gson.fromJson("{command: 'whatsit'}", ICommand.class);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("Unknown command `whatsit`");
        }
    }

    public void testValidCommandMissingProject() {
        try {
            gson.fromJson("{command: 'get_problems', file: 'bleh.java'}", ICommand.class);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("The `project` field is required");
        }
    }

    public void testValidCommandInvalidProject() {
        try {
            gson.fromJson("{command: 'get_problems', project: 'bleh', file: 'bleh.java'}",
                    ICommand.class);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("Couldn't find project at bleh");
        }
    }

    public void testValidCommand() {
        String projectPath = getProjectPath(JAVA_PROJECT);
        String json = "{command: 'get_problems', project: '"
                + projectPath + "', file: 'src/SomeClass.java'}";
        ICommand command = gson.fromJson(json, ICommand.class);
        assertThat(command).isInstanceOf(GetProblemsCommand.class);
    }

    public void testInjectRunner() throws NoSuchFieldException, IllegalAccessException {
        String projectPath = getProjectPath(JAVA_PROJECT);
        String json = "{command: 'run', client: 'vim', exe: '/usr/bin/vim',"
                + "instance: 'VIM1', project: '"
                + projectPath + "'}";
        ICommand command = gson.fromJson(json, ICommand.class);
        assertThat(command).isInstanceOf(RunCommand.class);

        final AsyncRunner runner = ((RunCommand) command).getRunner();
        assertThat(runner)
                .isNotNull()
                .isInstanceOf(VimAsyncRunner.class);

        VimAsyncRunner vim = (VimAsyncRunner) runner;
        assertThat(vim.getExe()).isEqualTo("/usr/bin/vim");
        assertThat(vim.getInstanceName()).isEqualTo("VIM1");
    }

    public void testDummyRunner() throws NoSuchFieldException, IllegalAccessException {
        String projectPath = getProjectPath(JAVA_PROJECT);
        String json = "{command: 'run',  project: '" + projectPath + "'}";
        ICommand command = gson.fromJson(json, ICommand.class);
        assertThat(command).isInstanceOf(RunCommand.class);

        final AsyncRunner runner = ((RunCommand) command).getRunner();
        assertThat(runner)
                .isNotNull()
                .isInstanceOf(DummyRunner.class);
    }
}
