package org.intellivim.core.command.run;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.impl.MockJdkWrapper;
import com.intellij.openapi.roots.JdkOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.roots.impl.ModuleJdkOrderEntryImpl;
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.roots.impl.RootModelImpl;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class RunTest extends BaseTestCase {

    Project project;

    @Override
    protected String getProjectPath() {
//        return null;
        return getProjectPath(RUNNABLE_PROJECT);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        project = getProject();
        Module module = myFixture.getModule();

        ModuleRootManagerImpl fixtureRoot = (ModuleRootManagerImpl) ModuleRootManager
                .getInstance(module);
        Sdk fixtureSdk = fixtureRoot.getSdk();
        assertThat(fixtureSdk).isNotNull();

        ModuleRootManagerImpl root = getModuleRoot(project);
        if (root.getSdk() == null) {
            // no SDK in unit test, as expected :(
            // Let's fix that.
            // There may be a better way to do this, but...

            // this may not work
            RootModelImpl model = root.getRootModel();
            ModuleJdkOrderEntryImpl entry = new ModuleJdkOrderEntryImpl(
                    "1.7", "JavaSDK",
                    model, ProjectRootManagerImpl.getInstanceImpl(project));

            ModuleJdkOrderEntryImpl spy = Mockito.spy(entry);
            Mockito.when(spy.getJdk()).thenReturn(new MockJdkWrapper(
                    System.getProperty("java.home"),
                    fixtureSdk
            ));

            Field myWritable = RootModelImpl.class.getDeclaredField("myWritable");
            myWritable.setAccessible(true);
            myWritable.setBoolean(model, true);

            boolean foundJdk = false;
            for (OrderEntry e : model.getOrderEntries()) {
                if (e instanceof JdkOrderEntry
                        && ((JdkOrderEntry) e).getJdk() == null) {

//                    // okay, let's mock THIS one
//                    JdkOrderEntry mocked = Mockito.spy((JdkOrderEntry) e);
//                    Mockito.when(mocked.getJdk()).thenReturn(fixtureSdk);
                    model.removeOrderEntry(e);
//                    model.addOrderEntry(mocked);
//                    foundJdk = true;
                }
            }

            if (!foundJdk) {
                // add ours
                model.addOrderEntry(spy);
            }

            myWritable.setBoolean(model, false);

            assertTrue(getModuleRoot(project) == getModuleRoot(project));

            assertThat(root.getSdk()).isNotNull();
        }
    }

    public void testRun() {
        System.out.println(project);
        SimpleResult result = (SimpleResult) new RunCommand(project).execute();
        assertSuccess(result);
    }

    static ModuleRootManagerImpl getModuleRoot(Project project) {
        RunnerAndConfigurationSettings settings =
                RunCommand.pickRunSetting(project, null);
        ApplicationConfiguration config =
                (ApplicationConfiguration) settings.getConfiguration();
        JavaRunConfigurationModule configurationModule = config.getConfigurationModule();
        return (ModuleRootManagerImpl) ModuleRootManager
                .getInstance(configurationModule.getModule());
    }

}
