package org.intellivim;

import org.assertj.core.api.Assertions;
import org.intellivim.core.command.problems.GetProblemsCommand;
import org.intellivim.core.command.run.AsyncRunner;
import org.intellivim.core.command.run.DummyRunner;
import org.intellivim.core.command.run.RunCommand;
import org.intellivim.core.command.run.VimAsyncRunner;

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

    @Override
    protected String getProjectPath() {
        return null; // NB unused here
    }

    public void testInvalidCommand() {
        try {
            inflateAndInject("{command: 'whatsit'}");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("Unknown command `whatsit`");
        }
    }

    public void testValidCommandMissingProject() {
        try {
            inflateAndInject("{command: 'get_problems', file: 'bleh.java'}");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("The `project` field is required");
        }
    }

    public void testValidCommandInvalidProject() {
        try {
            inflateAndInject(
                    "{command: 'get_problems', project: 'bleh', file: 'bleh.java'}");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("Couldn't find project at bleh");
        }
    }

    public void testValidCommand() {
        String projectPath = getProjectPath(JAVA_PROJECT);
        String json = "{command: 'get_problems', project: '"
                + projectPath + "', file: 'src/SomeClass.java'}";
        ICommand command = inflateAndInject(json);
        assertThat(command).isInstanceOf(GetProblemsCommand.class);
    }

    public void testInjectRunner() {
        String projectPath = getProjectPath(JAVA_PROJECT);
        String json = "{command: 'run', client: 'vim', exe: '/usr/bin/vim',"
                + "instance: 'VIM1', project: '"
                + projectPath + "'}";
        ICommand command = inflateAndInject(json);
        assertThat(command).isInstanceOf(RunCommand.class);

        final AsyncRunner runner = ((RunCommand) command).getRunner();
        assertThat(runner)
                .isNotNull()
                .isInstanceOf(VimAsyncRunner.class);

        VimAsyncRunner vim = (VimAsyncRunner) runner;
        assertThat(vim.getExe()).isEqualTo("/usr/bin/vim");
        assertThat(vim.getInstanceName()).isEqualTo("VIM1");
    }

    public void testDummyRunner() {
        String projectPath = getProjectPath(JAVA_PROJECT);
        String json = "{command: 'run',  project: '" + projectPath + "'}";
        ICommand command = inflateAndInject(json);
        assertThat(command).isInstanceOf(RunCommand.class);

        final AsyncRunner runner = ((RunCommand) command).getRunner();
        assertThat(runner)
                .isNotNull()
                .isInstanceOf(DummyRunner.class);
    }

    public static ICommand inflateAndInject(final String json) {
        try {
            return IVGson.newInstance().fromJson(json, IVGson.RawCommand.class).init();
        } catch (IllegalAccessException e) {
            Assertions.fail("Unexpected error parsing " + json, e);
            return null;
        }
    }
}
