package org.intellivim.java.command;

import org.intellivim.BaseTestCase;
import org.intellivim.ICommand;
import org.intellivim.SimpleResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class JavaCompletePackageTest extends BaseTestCase {

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testFirst() {
        assertThat(suggestionsFor(""))
                .containsExactly("org");
    }

    public void testPartial() {
        assertThat(suggestionsFor("o"))
                .containsExactly("org");
        assertThat(suggestionsFor("org.int"))
                .containsExactly("org.intellivim");
    }

    public void testOrg() {
        assertThat(suggestionsFor("org."))
                .containsExactly("org.intellivim");
        assertThat(suggestionsFor("org"))
                .containsExactly("org.intellivim");
    }

    private List<String> suggestionsFor(final String s) {
        final ICommand command = new JavaCompletePackageCommand(getProject(), s);
        final SimpleResult result = (SimpleResult) command.execute();
        assertSuccess(result);
        return result.getResult();
    }
}
