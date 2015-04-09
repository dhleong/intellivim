package org.intellivim.core.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
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

        final SimpleResult result = rename(file, offset, "list2");
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
        final String originalFile = pathOf(file);
        final byte[] originalBytes = file.getVirtualFile().contentsToByteArray();
        final byte[] subclassBytes = subClass.getVirtualFile().contentsToByteArray();

        assertFileContains("class Dummy");

        final SimpleResult result = rename(file, offset, "Dummer");
        assertSuccess(result);

        // fetch these NOW so we can do restoration
        //  before our tests, in case they failed.
        //  It may make more sense to do this stuff in
        //  setUp/tearDown, but I don't really want
        //  to make a separate Test class....
        final RenameResult info = result.getResult();
        final String newSubclassDiskText =
                new String(subClass.getVirtualFile().contentsToByteArray(false));
        final String newSubclassPsiText = subClass.getText();

        // special restore
        PsiFile dummerClass = ProjectUtil.getPsiFile(getProject(),
                DUMMY_FILE_PATH.replace("mmy", "mmer"));
        String dummerPath = pathOf(dummerClass);
        delete(dummerClass);

        // manual restore, because the test env will be confused otherwise
        FileOutputStream out = new FileOutputStream(new File(originalFile));
        out.write(originalBytes);
        out.close();

        setFileBytes(subClass, subclassBytes);

        // make sure the file on disk was updated
        assertThat(newSubclassDiskText)
                .isEqualTo(newSubclassPsiText)
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
        final PsiFile subclass = ProjectUtil.getPsiFile(getProject(), SUBCLASS_FILE_PATH);
        final byte[] subclassBytes = subclass.getVirtualFile().contentsToByteArray();
        byte[] originalBytes = getPsiFile().getVirtualFile().contentsToByteArray();
        final String originalFile = pathOf(getPsiFile());

        assertFileContains("class Dummy");

        final SimpleResult result = rename(subclass, offset, "Dummer");
        assertSuccess(result);

//        assertFileDoesNotContain("class Dummy");
//        assertFileNowContains("class Dummer");

        final RenameResult info = result.getResult();

        // special restore
        PsiFile dummerClass = ProjectUtil.getPsiFile(getProject(),
                DUMMY_FILE_PATH.replace("mmy", "mmer"));
        String dummerPath = pathOf(dummerClass);
        delete(dummerClass);

        // manual restore, because the test env will be confused otherwise
        FileOutputStream out = new FileOutputStream(new File(originalFile));
        out.write(originalBytes);
        out.close();

        setFileBytes(subclass, subclassBytes);

        assertThat(info.changed)
                .hasSize(1)
                .contains(pathOf(subclass));
        assertThat(info.renamed)
                .hasSize(1)
                .containsValue(dummerPath);
    }

    private void delete(final PsiFile file) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                file.delete();
            }
        });
    }

    private SimpleResult rename(final PsiFile file,
            final int offset, final String rename) {
        return execute(new RenameElementCommand(getProject(), file, offset, rename));
    }

    private static String pathOf(final PsiFile psiFile) {
        return psiFile.getVirtualFile().getCanonicalPath();
    }

    private static void setFileBytes(final PsiFile file, final byte[] bytes) throws IOException {
        ApplicationManager.getApplication().runWriteAction(
                new ThrowableComputable<Void, IOException>() {
                    @Override
                    public Void compute() throws IOException {
                        file.getVirtualFile().setBinaryContent(bytes);

                        PsiManager.getInstance(file.getProject()).reloadFromDisk(file);
                        return null;
                    }
                });
    }

}
