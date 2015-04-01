package org.intellivim.java.command;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.find.LocationResult;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dhleong
 */
public class JavaNewTest extends BaseTestCase {

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testResolveDir() {
        final String mainDir = "src/java/main";
        final String testDir = "src/java/test";
        final PsiDirectory srcMain = mockDir(mainDir);
        final PsiDirectory srcTest = mockDir(testDir);
        final PsiDirectory[] choices = {srcMain, srcTest};

        assertThat(commandWithDir(null).resolveChosenDirectory(choices))
                .isNull();

        assertThat(commandWithDir(mainDir).resolveChosenDirectory(choices))
                .isSameAs(srcMain);

        assertThat(commandWithDir(testDir).resolveChosenDirectory(choices))
                .isSameAs(srcTest);
    }

    public void testNewFQN() {

        final Project project = getProject();
        final File expectedDir = new File(project.getBaseDir().getPath(),
                "src/org/intellivim/test");
        assertThat(expectedDir).doesNotExist();

        final SimpleResult result = (SimpleResult) new JavaNewCommand(project,
                "class", "org.intellivim.test.NewlyCreated").execute();
        assertSuccess(result);

        LocationResult location = result.getResult();
        System.out.println("Created: " + location);
        assertThat(location).isNotNull();

        assertThat(expectedDir).exists();

        final File createdFile = new File(location.file);
        assertThat(createdFile)
                .exists()
                .hasParent(expectedDir)
                .hasName("NewlyCreated.java");

        // cleanup
        createdFile.delete();
        expectedDir.delete();
    }

    private static PsiDirectory mockDir(final String path) {
        final VirtualFile virtual = mock(VirtualFile.class);
        when(virtual.getPath()).thenReturn(path);

        final PsiDirectory dir = mock(PsiDirectory.class);
        when(dir.getVirtualFile()).thenReturn(virtual);
        return dir;
    }

    JavaNewCommand commandWithDir(final String dir) {
        final JavaNewCommand command = new JavaNewCommand(getProject(),
                "class", "org.intellivim.test.NewlyCreated");
        command.dir = dir;
        return command;
    }
}
