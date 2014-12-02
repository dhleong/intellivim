package org.intellivim.core.command.run;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
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
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.inject.UnsupportedClientException;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class RunTest extends BaseTestCase {

    Project project;

    @Override
    protected String getProjectPath() {
        return getProjectPath(RUNNABLE_PROJECT);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // NB get this ONCE for each run (important!)
        //  otherwise, the ModuleRootManager will NOT be the one we
        //  infiltrated with a spy
        project = getProject();

        // get a usable SDK from the fixture's module
        ModuleRootManagerImpl fixtureRoot =
                (ModuleRootManagerImpl) ModuleRootManager
                        .getInstance(myFixture.getModule());
        Sdk fixtureSdk = fixtureRoot.getSdk();
        assertThat(fixtureSdk).isNotNull();

        // do we already have a good SDK?
        ModuleRootManagerImpl root = getModuleRoot(project);
        if (root.getSdk() == null) {
            // nope, no SDK in unit test, as expected :(
            // Let's fix that.
            // There may be a better way to do this, but...

            // manufacture a usable spy
            RootModelImpl model = root.getRootModel();
            ModuleJdkOrderEntryImpl entry = new ModuleJdkOrderEntryImpl(
                    "1.7", "JavaSDK",
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
        }
    }

    public void testRun() {
        LoggingRunner runner = new LoggingRunner();
        SimpleResult result = (SimpleResult) new RunCommand(project, runner).execute();
        assertSuccess(result);

        try {
            runner.awaitTermination(5000);
        } catch (InterruptedException e) {
            fail("RunnableProject did not finish execution within 5s");
        }

        assertThat(runner.stdout).contains("Standard Output");
        assertThat(runner.stderr).contains("Standard Error");
        assertThat(runner.system)
                .hasSize(2) // first one is the command line
                .contains("Process finished with exit code 0");
    }

    public void testCompileError() {
        // FIXME try to run JAVA_PROJECT or something
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

    static class LoggingRunner implements AsyncRunner {

        List<String> stdout = new ArrayList<String>();
        List<String> stderr = new ArrayList<String>();
        List<String> system = new ArrayList<String>();

        @Override
        public void prepare() throws UnsupportedClientException {

        }

        @Override
        public void sendOut(String line) {
            stdout.add(line);
        }

        @Override
        public void sendErr(String line) {
            stderr.add(line);
        }

        @Override
        public void sendSys(String line) {
            system.add(line);
        }

        @Override
        public synchronized void terminate() {
            notify();
        }

        synchronized void awaitTermination(long timeout) throws InterruptedException {
            wait(timeout);
        }
    }
}
