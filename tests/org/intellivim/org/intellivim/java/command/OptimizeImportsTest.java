package org.intellivim.org.intellivim.java.command;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.java.command.OptimizeImportsCommand;

import java.io.IOException;

/**
 * Created by dhleong on 11/18/14.
 */
public class OptimizeImportsTest extends BaseTestCase {

    String projPath = getProjectPath(JAVA_PROJECT);
    String filePath = "src/org/intellivim/javaproject/Problematic.java";
    private byte[] originalContents;

    public void setUp() throws Exception {
        super.setUp();

        Project project = ProjectUtil.getProject(projPath);
        VirtualFile file = ProjectUtil.getVirtualFile(project, filePath);
        originalContents = file.contentsToByteArray();

        DaemonCodeAnalyzerImpl daemonCodeAnalyzer =
                (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
        daemonCodeAnalyzer.prepareForTest();

        final StartupManagerImpl startupManager = (StartupManagerImpl) StartupManagerEx.getInstanceEx(project);
        startupManager.runStartupActivities();
        startupManager.startCacheUpdate();
        startupManager.runPostStartupActivities();
    }

    public void tearDown() throws Exception {
        super.tearDown();

        if (fileContentsChanged())
            restoreFile();

        Project project = ProjectUtil.getProject(projPath);
        ((StartupManagerImpl) StartupManager.getInstance(project)).checkCleared();
        DaemonCodeAnalyzerImpl daemonCodeAnalyzer =
                (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
        daemonCodeAnalyzer.cleanupAfterTest();
    }

    /**
     * The actual test
     */
    public void testCommand() throws IOException {

        SimpleResult result = (SimpleResult) new OptimizeImportsCommand(projPath, filePath).execute();
        assertSuccess(result);
        assertTrue("File contents should have changed", fileContentsChanged());
    }

    /*
     * utils
     */

    private VirtualFile getFile() {
        Project proj = ProjectUtil.getProject(projPath);
        return ProjectUtil.getVirtualFile(proj, filePath);
    }

    private boolean fileContentsChanged() throws IOException {
        VirtualFile file = getFile();
        byte[] updatedContents = file.contentsToByteArray();
        final int len = updatedContents.length;
        if (len != originalContents.length) {
            // definitely not same
            return true;
        } else {
            // is it the same?
            for (int i = 0; i < len; i++) {
                if (originalContents[i] != updatedContents[i]) {
                    return true;
                }
            }
        }

        // no change
        return false;
    }

    private void restoreFile() throws IOException {
        getFile().setBinaryContent(originalContents);
    }


}
