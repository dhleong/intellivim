package org.intellivim.java.command;

import com.intellij.openapi.project.Project;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

/**
 * @author dhleong
 */
public class JavaGenerateTest extends BaseTestCase {
    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testGenerate() {
        final Project project = getProject();

        final SimpleResult result = (SimpleResult) new JavaGenerateCommand(project).execute();
    }
}
