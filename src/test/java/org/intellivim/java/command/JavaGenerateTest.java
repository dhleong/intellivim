package org.intellivim.java.command;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        final String filePath = "src/org/intellivim/javaproject/Dummy.java";
        int offset = 269; // new Dummy().

        final PsiFile file = ProjectUtil.getPsiFile(project, filePath);
        final SimpleResult result = execute(new JavaGenerateCommand(
                project, file, offset));
        assertSuccess(result);

        List<String> results = result.getResult();
        assertThat(results).isNotEmpty();
    }
}
