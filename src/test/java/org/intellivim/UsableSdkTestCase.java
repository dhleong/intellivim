package org.intellivim;

import com.intellij.JavaTestUtil;
import com.intellij.compiler.CompilerManagerImpl;
import com.intellij.compiler.CompilerTestUtil;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.intellivim.core.util.CompileAndRunner;
import org.intellivim.core.util.ProjectUtil;

import java.io.IOException;

/**
 * Wacky test case tweaks the project configs
 *  so it can access a usable Jdk.
 *
 * You must call prepareProject(NAME) at the
 *  top of each test and ONLY use the Project
 *  instance returned there in the body your
 *  tests
 *
 * @author dhleong
 */
public abstract class UsableSdkTestCase extends BaseTestCase {

    private String currentProjectName = null;
    private byte[] projectFileContents;
    private byte[] compilerFileContents;

    @Override
    protected String getProjectPath() {
        return currentProjectName;
    }

    @Override
    protected Project getProject() {
        throw new IllegalStateException("Do not use getProject(); use prepareProject");
    }

    /**
     * Call this ONCE at the top of your test cases to get
     *  a Project instance to be used throughout. It's done
     *  this way so you can test multiple projects within
     *  a single Test file
     *
     * @param projectName
     * @return
     * @throws Exception
     */
    public Project prepareProject(String projectName) throws Exception {

        currentProjectName = projectName;

        // NB get this ONCE for each run (important!)
        //  otherwise, the ModuleRootManager will NOT be the one we
        //  infiltrated with a spy
        final Project project =
                ProjectUtil.ensureProject(getProjectPath(projectName));

        // NB The following stuff modifies stuff on disk. We'll want to restore
        //  all that when we're done
        final Module module = getModule(project);
        VirtualFile projectFile = module.getModuleFile();
        VirtualFile rootDir = projectFile.getParent();
        VirtualFile compilerFile = rootDir.findChild("compiler.xml");

        projectFileContents = projectFile.contentsToByteArray();
        compilerFileContents = compilerFile.contentsToByteArray();

        UIUtil.invokeAndWaitIfNeeded(new Runnable() {

            @Override
            public void run() {
                JavaTestUtil.setupTestJDK();
                ModuleRootModificationUtil.setModuleSdk(
                        module, JavaTestUtil.getTestJdk());
            }
        });

        // I... don't even.
        CompilerManagerImpl.testSetup();

        CompilerTestUtil.setupJavacForTests(project);
        CompilerTestUtil.enableExternalCompiler(project);
        CompilerTestUtil.scanSourceRootsToRecompile(project);
        CompilerTestUtil.saveApplicationSettings();

        return project;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (projectFileContents == null || compilerFileContents == null)
            fail("Missing project/compiler file contents");

        final Project project =
                ProjectUtil.ensureProject(getProjectPath(currentProjectName));

        // NB The following stuff modifies stuff on disk. We'll want to restore
        //  all that when we're done
        final Module module = getModule(project);
        final VirtualFile projectFile = module.getModuleFile();
        final VirtualFile rootDir = projectFile.getParent();
        final VirtualFile compilerFile = rootDir.findChild("compiler.xml");

        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
                try {
                    projectFile.setBinaryContent(projectFileContents);
                    compilerFile.setBinaryContent(compilerFileContents);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected static Module getModule(Project project) {
        RunnerAndConfigurationSettings settings =
                CompileAndRunner.pickRunSetting(project, null);
        if (settings == null)
            return null;

        ApplicationConfiguration config =
                (ApplicationConfiguration) settings.getConfiguration();
        JavaRunConfigurationModule configurationModule = config.getConfigurationModule();
        return configurationModule.getModule();
    }

}
