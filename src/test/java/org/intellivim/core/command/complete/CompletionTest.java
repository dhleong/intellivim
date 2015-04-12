package org.intellivim.core.command.complete;

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
public class CompletionTest extends BaseTestCase {

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testCompletion() {
        Project project = getProject();
        String filePath = "src/org/intellivim/javaproject/Dummy.java";
        int offset = 269; // new Dummy().

        final PsiFile file = ProjectUtil.getPsiFile(project, filePath);
        SimpleResult result = execute(new CompleteCommand(project, file, offset));
        assertSuccess(result);

        CompleteCommand.CompletionResultInfo completion = result.getResult();
        List<CompletionInfo<?>> infoList = completion.completions;
        assertThat(infoList).contains(
                CompletionInfo.forMethod("boring()", "()-> void", ""),
                CompletionInfo.forMethod("moreBoring()", "()-> void", ""),
                CompletionInfo.forMethod(
                        "notBoring(",
                        "(int number)-> void",
                        "/** I promise it's not boring */"
                ),
                CompletionInfo.forMethod(
                        "notBoring(", "(int number, String foo)-> void", ""),
                CompletionInfo.forMethod("fluid()", "()-> Dummy", "")
        );

    }

    // TODO test fields? local vars?
}
