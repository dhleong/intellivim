package org.intellivim.core.command.params;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class GetParamHintsTest extends BaseTestCase {

    final String filePath = "src/org/intellivim/javaproject/Dummy.java";

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testMethod() {
        int offset = 387;

        SimpleResult result = (SimpleResult) new GetParamHintsCommand(getProject(), filePath, offset).execute();
        assertSuccess(result);

        final List<String> params = result.getResult();
        assertThat(params)
                .hasSize(2)
                .contains("int number",
                          "int number, String foo");
    }

    public void testConstructor() {
        int offset = 267;

        SimpleResult result = (SimpleResult) new GetParamHintsCommand(getProject(), filePath, offset).execute();
        assertSuccess(result);

        final List<String> params = result.getResult();
        assertThat(params)
                .hasSize(4)
                .contains("<no parameters>",
                          "int number",
                          "String string",
                          "int number, String andString");
    }
}
