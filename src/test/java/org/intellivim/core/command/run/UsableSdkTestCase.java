package org.intellivim.core.command.run;

import com.intellij.compiler.CompilerManagerImpl;
import com.intellij.compiler.server.BuildManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.MockJdkWrapper;
import com.intellij.openapi.roots.JdkOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.ModuleJdkOrderEntryImpl;
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.roots.impl.RootModelImpl;
import org.apache.commons.lang.StringUtils;
import org.intellivim.BaseTestCase;
import org.intellivim.core.util.ProjectUtil;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

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

        // get a usable SDK from the fixture's module
        ModuleRootManagerImpl fixtureRoot =
                (ModuleRootManagerImpl) ModuleRootManager
                        .getInstance(myFixture.getModule());
        Sdk fixtureSdk = fixtureRoot.getSdk();
        assertThat(fixtureSdk).isNotNull();

        // do we already have a good SDK?
        ModuleRootManagerImpl root = getModuleRoot(project);
        if (root == null) {
            // no run config, probably. hopefully we know what we're doing
            return project;
        }

        if (root.getSdk() == null) {
            // nope, no SDK in unit test, as expected :(
            // Let's fix that.
            // There may be a better way to do this, but...

            // manufacture a usable spy
            RootModelImpl model = root.getRootModel();
//            fixtureSdk.ge
            System.out.println("Use " + fixtureSdk.getVersionString()
                    + ":" + fixtureSdk.getName());
            ModuleJdkOrderEntryImpl entry = new ModuleJdkOrderEntryImpl(
                    fixtureSdk.getVersionString(), fixtureSdk.getName(),
                    model, ProjectRootManagerImpl.getInstanceImpl(project));
            ModuleJdkOrderEntryImpl spy = Mockito.spy(entry);
            Mockito.when(spy.getJdk()).thenReturn(new MockJdkWrapper(
                    System.getProperty("java.home"),
                    fixtureSdk
            ));

            // sneakily make the model editable
            Field myWritable = RootModelImpl.class.getDeclaredField("myWritable");
            myWritable.setAccessible(true);
            myWritable.setBoolean(model, true);

            // strip out any boring and useless guys
            for (OrderEntry e : model.getOrderEntries()) {
                if (e instanceof JdkOrderEntry
                        && ((JdkOrderEntry) e).getJdk() == null) {
                    model.removeOrderEntry(e);
                }
            }

            // inject our useful spy
            model.addOrderEntry(spy);

            // restore
            myWritable.setBoolean(model, false);

            // make sure it worked
            assertTrue(getModuleRoot(project) == getModuleRoot(project));
            assertThat(root.getSdk()).isNotNull();

//            // use in-process so it sees our sdk (?)
//            CompilerWorkspaceConfiguration.getInstance(project)
//                    .USE_OUT_OF_PROCESS_BUILD = false;
        }

        // I... don't even.
        CompilerManagerImpl.testSetup();

        if (StringUtils.isEmpty(System.getProperty(PathManager.PROPERTY_CONFIG_PATH))) {
            String pluginsPath = System.getProperty(PathManager.PROPERTY_PLUGINS_PATH);
            final File systemParent;
            if (!StringUtils.isEmpty(pluginsPath)) {
                File pluginsFile = new File(pluginsPath);
                systemParent = pluginsFile.getParentFile();

            } else {
                File compilePath = BuildManager.getInstance().getBuildSystemDirectory();
                systemParent = compilePath.getParentFile() //  ./system
                                          .getParentFile(); // ./
            }
            System.setProperty(PathManager.PROPERTY_CONFIG_PATH,
                    new File(systemParent, "config").getAbsolutePath());

            System.clearProperty(PathManager.PROPERTY_HOME_PATH);
        }

        Field isUnitTestMode = BuildManager.class.getDeclaredField("IS_UNIT_TEST_MODE");
        isUnitTestMode.setAccessible(true);
        isUnitTestMode.setBoolean(BuildManager.getInstance(), false);

        return project;
    }


    static ModuleRootManagerImpl getModuleRoot(Project project) {
        RunnerAndConfigurationSettings settings =
                RunCommand.pickRunSetting(project, null);
        if (settings == null)
            return null;

        ApplicationConfiguration config =
                (ApplicationConfiguration) settings.getConfiguration();
        JavaRunConfigurationModule configurationModule = config.getConfigurationModule();
        return (ModuleRootManagerImpl) ModuleRootManager
                .getInstance(configurationModule.getModule());
    }

}
