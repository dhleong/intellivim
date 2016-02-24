package org.intellivim;

import static org.intellivim.IVAssertions.assertThat;

/**
 * @author dhleong
 */
public class CommandExecutorTest extends BaseTestCase {

    String filePath = PROBLEMATIC_FILE_PATH;

    private CommandExecutor ex;

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ex = new CommandExecutor(IVGson.newInstance(), "42.0");
    }

    public void testCorrectVersion() {
        final String command = "{'command':'get_problems'," +
                "'project': '" + getProjectPath() + "'," +
                "'file': '" + filePath + "'," +
                "'v': '42.0'}";

        assertThat(ex.execute(command)).isSuccess();
    }

    public void testWrongVersion() {
        final String command = "{'command':'get_problems'," +
                "'project': '" + getProjectPath() + "'," +
                "'file': '" + filePath + "'," +
                "'v': '3.5'}";

        assertThat(ex.execute(command))
                .hasErrorContaining("versions must match");
    }

    public void testEmptyVersion() {
        final String command = "{'command':'get_problems'," +
                "'project': '" + getProjectPath() + "'," +
                "'file': '" + filePath + "'," +
                "'v': ''}";

        assertThat(ex.execute(command))
                .hasErrorContaining("versions must match");
    }

    public void testMissingVersion() {
        final String command = "{'command':'get_problems'," +
                "'project': '" + getProjectPath() + "'," +
                "'file': '" + filePath + "'}";

        assertThat(ex.execute(command))
                .hasErrorContaining("versions must match");
    }

}
