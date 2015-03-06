package org.intellivim.morph;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.intellivim.BaseTestCase;
import org.intellivim.Command;
import org.intellivim.ICommand;
import org.intellivim.IVGson;
import org.intellivim.Result;
import org.intellivim.java.command.junit.JUnitRunTestCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

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


    Gson gson;

    @Override
    protected String getProjectPath() {
        return null; // NB unused here
    }

    public void setUp() throws Exception {
        super.setUp();

        gson = IVGson.newInstance();
    }

    public void testNoMorpher() {
        try {
            gson.fromJson("{command:'test__no_morpher', file:'foo.java'}",
                    ICommand.class);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("No morpher");
        }
    }


    public void testNoImpls() {
        try {
            gson.fromJson("{command:'test__no_impls', file:'foo.java'}",
                    ICommand.class);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (JsonSyntaxException e) {
            assertThat(e.getCause())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No @CommandImpl");
        }
    }

    public void testUnsupported() {
        try {
            gson.fromJson("{command:'run_test', file:'foo.superrandom'}",
                    ICommand.class);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (JsonSyntaxException e) {
            // TODO we could let the Polymorpher generate the error message
            assertThat(e.getCause())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No matching @CommandImpl");
        }
    }


    public void testJunit() {
        ICommand cmd = gson.fromJson("{command:'run_test', file:'foo.java'}",
                ICommand.class);
        assertThat(cmd).isInstanceOf(JUnitRunTestCommand.class);
    }

}
