package org.intellivim.core.command.locate;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class LocateFileTest extends BaseTestCase {

    static final LocatedFile DUMMY_FILE =
            new LocatedFile("java-project/src/org/intellivim/javaproject/Dummy.java");
    static final LocatedFile DUMMY_CLASS =
            new LocatedFile("org.intellivim.javaproject.Dummy");

    // just used for context
    final String filePath = "src/org/intellivim/javaproject/Dummy.java";

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testFile_dummy() {
        assertThat(searchFile("dummy"))
            .isNotEmpty()
            .contains(DUMMY_FILE);
    }

    public void testClass_dummy() {
        assertThat(searchClass("dummy"))
                .isNotEmpty()
                .contains(DUMMY_CLASS);
    }

    private List<LocatedFile> searchFile(String pattern) {
        return searchResult(LocateFileCommand.LocateType.FILE, pattern).getResult();
    }

    private List<LocatedFile> searchClass(String pattern) {
        return searchResult(LocateFileCommand.LocateType.CLASS, pattern).getResult();
    }

    private SimpleResult searchResult(LocateFileCommand.LocateType type,
              String pattern) {
        return (SimpleResult) new LocateFileCommand(
                getProject(), type, filePath, pattern).execute();
    }
}
