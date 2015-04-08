package org.intellivim.core.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiFile;
import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.RenameElementCommand.RenameResult;
import org.intellivim.core.util.ProjectUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class RenameElementTest extends FileEditingTestCase {
    @Override
    protected String getFilePath() {
        return DUMMY_FILE_PATH;
    }

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testRenameLocalVariable() {
        final int offset = 314; // list
        final PsiFile file = getPsiFile();
//        final PsiElement element = file.findElementAt(offset);

        assertFileContains("list = new ArrayList");
        assertFileContains("list.add(");

        final SimpleResult result = (SimpleResult)
                new RenameElementCommand(getProject(), file, offset, "list2").execute();
        assertSuccess(result);

        assertFileDoesNotContain("list = new ArrayList");
        assertFileDoesNotContain("list.add(");
        assertFileNowContains("list2 = new ArrayList");
        assertFileNowContains("list2.add(");

        RenameResult info = result.getResult();
        assertThat(info.renamed).isEmpty();
        assertThat(info.changed)
                .hasSize(1)
                .containsExactly(pathOf(getPsiFile()));
    }

    /** rename a class from within its own file */
    public void testRenameClassInternally() throws IOException {
        final int offset = 147; // class [D]ummy
        final PsiFile file = getPsiFile();
        final PsiFile subClass = ProjectUtil.getPsiFile(getProject(), SUBCLASS_FILE_PATH);
//        final PsiElement element = file.findElementAt(offset);
        final String subclassFile = pathOf(subClass);
        final String originalFile = pathOf(file);
        byte[] originalBytes = file.getVirtualFile().contentsToByteArray();
        byte[] subclassBytes = subClass.getVirtualFile().contentsToByteArray();

        assertFileContains("class Dummy");

        final SimpleResult result = (SimpleResult)
                new RenameElementCommand(getProject(), file, offset, "Dummer").execute();
        assertSuccess(result);

//        assertFileDoesNotContain("class Dummy");
//        assertFileNowContains("class Dummer");

        RenameResult info = result.getResult();

        // special restore
        PsiFile dummerClass = ProjectUtil.getPsiFile(getProject(),
                DUMMY_FILE_PATH.replace("mmy", "mmer"));
        String dummerPath = pathOf(dummerClass);
        new File(dummerPath).delete();

        // manual restore, because the test env will be confused otherwise
        FileOutputStream out = new FileOutputStream(new File(originalFile));
        out.write(originalBytes);
        out.close();
        out = new FileOutputStream(new File(subclassFile));
        out.write(subclassBytes);
        out.close();

        // make sure the file on disk was updated
        assertThat(new String(subClass.getVirtualFile().contentsToByteArray(false)))
                .isEqualTo(subClass.getText())
                .contains("Dummer");

        assertThat(info.changed)
                .hasSize(1)
                .contains(pathOf(subClass));
        assertThat(info.renamed)
                .hasSize(1)
                .containsValue(dummerPath);
    }

    /** rename a class from a reference in another file */
    public void testRenameClassExternally() throws IOException {
        final int offset = 145; // extends [D]ummy
        final PsiFile file = ProjectUtil.getPsiFile(getProject(), SUBCLASS_FILE_PATH);
        final byte[] subclassBytes = file.getVirtualFile().contentsToByteArray();
        byte[] originalBytes = getPsiFile().getVirtualFile().contentsToByteArray();
        final String originalFile = pathOf(getPsiFile());

        assertFileContains("class Dummy");

        final SimpleResult result = (SimpleResult)
                new RenameElementCommand(getProject(), file, offset, "Dummer").execute();
        assertSuccess(result);

//        assertFileDoesNotContain("class Dummy");
//        assertFileNowContains("class Dummer");

        final PsiFile subClass = file;
        RenameResult info = result.getResult();

        // special restore
        PsiFile dummerClass = ProjectUtil.getPsiFile(getProject(),
                DUMMY_FILE_PATH.replace("mmy", "mmer"));
        String dummerPath = pathOf(dummerClass);
        new File(dummerPath).delete();

        // manual restore, because the test env will be confused otherwise
        FileOutputStream out = new FileOutputStream(new File(originalFile));
        out.write(originalBytes);
        out.close();

        ApplicationManager.getApplication().runWriteAction(
                new ThrowableComputable<Void, IOException>() {
            @Override
            public Void compute() throws IOException {
                subClass.getVirtualFile().setBinaryContent(subclassBytes);
                return null;
            }
        });

        assertThat(info.changed)
                .hasSize(1)
                .contains(pathOf(subClass));
        assertThat(info.renamed)
                .hasSize(1)
                .containsValue(dummerPath);
    }

    private static String pathOf(final PsiFile psiFile) {
        return psiFile.getVirtualFile().getCanonicalPath();
    }

}
