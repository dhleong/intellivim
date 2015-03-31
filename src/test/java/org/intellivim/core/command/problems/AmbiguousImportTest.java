package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class AmbiguousImportTest extends FileEditingTestCase {

    public static final String IMPORT_CHOICE = "org.intellivim.javaproject.subpackage2.JavaMain";
    public static final String ALT_IMPORT_CHOICE = "org.intellivim.javaproject.subpackage.JavaMain";

    public static final String IMPORT_STATEMENT = "import " + IMPORT_CHOICE + ";";
    public static final String ALT_IMPORT_STATEMENT = "import " + ALT_IMPORT_CHOICE + ";";

    final String projPath = getProjectPath(JAVA_PROJECT);
    final String filePath = PROBLEMATIC_TWICE_FILE_PATH;

    @Override
    protected String getProjectPath() {
        return projPath;
    }

    @Override
    protected String getFilePath() {
        return filePath;
    }

    public void testExecute() {

        // clean slate
        assertFileDoesNotContain(IMPORT_STATEMENT);
        assertFileDoesNotContain(ALT_IMPORT_STATEMENT);

        Project project = getProject();
        GetProblemsCommand command = new GetProblemsCommand(project, filePath);
        SimpleResult result = (SimpleResult) command.execute();
        assertSuccess(result);

        final Problems problems = result.getResult();
        assertSize(1, problems);

        final QuickFixDescriptor quickFix = problems.locateQuickFix("0.0");
        assertThat(quickFix)
                .isNotNull()
                .isInstanceOf(ImportsQuickFixDescriptor.class);
        assertThat(quickFix.description).isEqualToIgnoringCase("Import Class");

        ImportsQuickFixDescriptor importsQuickFix = (ImportsQuickFixDescriptor) quickFix;
        assertThat(importsQuickFix.choices)
                .isNotNull()
                .hasSize(2)
                .contains(IMPORT_CHOICE, ALT_IMPORT_CHOICE);

        // Attempting to fix without an arg will return the list
        SimpleResult fixResult = (SimpleResult) new FixProblemCommand(project, filePath, quickFix.id).execute();
        assertSuccess(fixResult);

        // still no imports yet...
        assertFileDoesNotContain(IMPORT_STATEMENT);
        assertFileDoesNotContain(ALT_IMPORT_STATEMENT);

        // ... but we should prompt!
        assertThat(fixResult.result)
                .isNotNull()
                .isInstanceOf(List.class);
        List<String> list = fixResult.getResult();
        assertThat(list)
                .hasSize(2)
                .contains(IMPORT_CHOICE, ALT_IMPORT_CHOICE);

        // now, let's do the RIGHT thing
        SimpleResult fixed = (SimpleResult) new FixProblemCommand(project, filePath, quickFix.id,
                IMPORT_CHOICE).execute();
        assertSuccess(fixed);

        // no choices prompt...
        assertThat(fixed.result).isNull();

        // ... and it's finally imported
        assertFileNowContains(IMPORT_STATEMENT);
    }


}
