package org.intellivim.morph;

import com.google.gson.JsonSyntaxException;
import org.intellivim.BaseTestCase;
import org.intellivim.Command;
import org.intellivim.ICommand;
import org.intellivim.Result;
import org.intellivim.java.command.junit.JUnitRunTestCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.intellivim.IVGsonTest.inflateAndInject;

/**
 * @author dhleong
 */
public class PolymorphTest extends BaseTestCase {

    @Command(value="test__no_morpher")
    static abstract class NoMorpher implements ICommand {
        @Override
        public Result execute() {
            return null;
        }
    }

    @Command(value="test__no_impls", morpher=FileExtensionsMorpher.class)
    static abstract class NoImpls implements ICommand {
        @Override
        public Result execute() {
            return null;
        }
    }

    final String filePath = "src/org/intellivim/javaproject/Dummy.java";

    @Override
    protected String getProjectPath() {
        return null; // NB unused here
    }


    public void testNoMorpher() {
        try {
            inflateAndInject("{command:'test__no_morpher', file:'foo.java'}");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("No morpher");
        }
    }


    public void testNoImpls() {
        try {
            inflateAndInject("{command:'test__no_impls', file:'foo.java'}");
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (JsonSyntaxException e) {
            assertThat(e.getCause())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No @CommandImpl");
        }
    }

    public void testUnsupported() {
        try {
            inflateAndInject("{command:'run_test', file:'foo.superrandom'}");
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (JsonSyntaxException e) {
            // TODO we could let the Polymorpher generate the error message
            assertThat(e.getCause())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No matching @CommandImpl");
        }
    }

    public void testJunit() {
        final String projectPath = getProjectPath(JAVA_PROJECT);
        final ICommand cmd = inflateAndInject("{command:'run_test'," +
                "file:'" + filePath + "'," +
                "project:'" + projectPath + "'}");
        assertThat(cmd).isInstanceOf(JUnitRunTestCommand.class);
    }

}
