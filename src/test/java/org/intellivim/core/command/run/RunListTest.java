package org.intellivim.core.command.run;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class RunListTest extends BaseTestCase {
    @Override
    protected String getProjectPath() {
        return getProjectPath(RUNNABLE_PROJECT);
    }

    public void testList() {
        SimpleResult result = (SimpleResult) new RunListCommand(getProject()).execute();
        assertSuccess(result);
        List<RunListCommand.RunConfigInfo> configs = result.getResult();
        assertThat(configs)
                .extracting("name", String.class)
                .contains("RunnableMain");
    }
}
