package org.intellivim.core.command.params;

import com.intellij.psi.PsiFile;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.params.GetParamHintsCommand.ParamHints;
import org.intellivim.core.util.ProjectUtil;

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

    /** When outside of (params), we should get nothing */
    public void testNone() {
        final int offset = 390; // notBoring(42)|;

        final ParamHints params = getParamsAt(offset);
        assertThat(params.start).isNotEqualTo(386);
        assertThat(params.hints).isEmpty();
    }

    public void testMethod() {
        final int offset = 387; // notBoring(|42);

        final ParamHints params = getParamsAt(offset);
        assertThat(params.start).isEqualTo(386);
        assertThat(params.hints)
                .hasSize(2)
                .contains("*int number*",
                          "*int number*, String foo");
    }

    public void testConstructor() {
        final int offset = 267; // new Dummy(|);

        final ParamHints params = getParamsAt(offset);
        assertThat(params.start).isEqualTo(266);
        assertThat(params.hints)
                .hasSize(4)
                .contains("*<no parameters>*",
                          "*int number*",
                          "*String string*",
                          "*int number*, String andString");
    }

    public void testSecondParam() {
        final int offset = 905; // notBoring(42, |"foo");

        final ParamHints params = getParamsAt(offset);
        assertThat(params.start).isEqualTo(900);
        assertThat(params.hints)
                .containsExactly("int number, *String foo*");
    }

    public void testNestedParam() {
        final int offset = 946; // notBoring(answerQuestion(|

        final ParamHints params = getParamsAt(offset);
        assertThat(params.start).isEqualTo(945);
        assertThat(params.hints)
                .containsExactly("*String question*");
    }

    public void testAfterNestedParam() {
        final int offset = 955; // notBoring(answerQuestion("life"), |"universe")

        final ParamHints params = getParamsAt(offset);
        assertThat(params.start).isEqualTo(930);
        assertThat(params.hints)
                .containsExactly("int number, *String foo*");
    }

    ParamHints getParamsAt(int offset) {
        final PsiFile file = ProjectUtil.getPsiFile(getProject(), filePath);
        SimpleResult result = execute(new GetParamHintsCommand(getProject(), file, offset));
        assertSuccess(result);

        return result.getResult();
    }
}
