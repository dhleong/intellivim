package org.intellivim.org.intellivim.core.command;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.problems.org.intellivim.core.command.GetDocumentationCommand;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class GetDocumentationTest extends BaseTestCase {

    final String filePath = "src/org/intellivim/javaproject/Dummy.java";

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    /** new [D]ummy() */
    public void testClassConstructor() {
        String doc = getDocAt(261);
        assertThat(doc)
            .isNotEmpty()
            .contains("Some dummy class for testing purposes");
        System.out.println(doc);
    }

    /** [n]otBoring() */
    public void testMethod() {
        assertThat(getDocAt(377))
                .isNotEmpty()
                .contains("I promise it's not boring") // the doc
                .contains("int&nbsp;number"); // the parameters (NB: it's html right now...)
    }

    private String getDocAt(int offset) {
        SimpleResult result = (SimpleResult)
                new GetDocumentationCommand(getProject(), filePath, offset).execute();
        assertSuccess(result);
        return result.getResult();
    }
}
