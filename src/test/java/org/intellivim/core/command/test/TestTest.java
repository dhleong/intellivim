package org.intellivim.core.command.test;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.junit.TestClassConfigurationProducer;
import com.intellij.execution.junit.TestMethodConfigurationProducer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ui.UIUtil;
import org.assertj.core.api.SoftAssertions;
import org.intellivim.UsableSdkTestCase;
import org.intellivim.core.command.run.RunTest;
import org.intellivim.core.util.BuildUtil;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.java.command.junit.JUnitRunTestCommand;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.intellivim.IVAssertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;

/**
 * I swear I'm not crazy. It's the test for TestCommand
 *
 * @author dhleong
 */
public class TestTest extends UsableSdkTestCase {

    static final String TESTABLE = "src/org/intellivim/runnable/test/Testable.java";

    static final long TIMEOUT = 10000;

    static final String JUNIT_EP_NAME = "com.intellij.junitListener";
    static final String JUNIT_BEAN_NAME =  "com.intellij.rt.execution.junit.IDEAJUnitListener";

    @Override
    protected void invokeTestRunnable(final Runnable runnable) throws Exception {
        // DON'T run on Swing dispatch thread; some of the compile
        //  stuff wants to run there, and we'll never get the results
        //  if we do, too
        runnable.run();
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }


    public void setUp() throws Exception {
        super.setUp();

        ExtensionPoint<ConfigurationType> configEp =
                Extensions.getRootArea()
                        .getExtensionPoint(
                                ConfigurationType.CONFIGURATION_TYPE_EP);
        configEp.registerExtension(new JUnitConfigurationType());

        ExtensionPoint<RunConfigurationProducer> runEp =
                Extensions.getRootArea()
                        .getExtensionPoint(RunConfigurationProducer.EP_NAME);
        runEp.registerExtension(new TestClassConfigurationProducer());
        runEp.registerExtension(new TestMethodConfigurationProducer());

    }

    /** make sure some pre-requisites work */
    public void testMocksWorked() throws Exception {
        final int offset = 132;

        Project project = prepareProject(TESTABLE_PROJECT);
        PsiFile file = ProjectUtil.getPsiFile(project, TESTABLE);

        final PsiElement element = prepareElement(project, file, offset);

        final SoftAssertions softly = new SoftAssertions();
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                softly.assertThat(JUnitUtil.isTestMethodOrConfig((PsiMethod) element))
                        .as("isTestMethodOrConfig")
                        .isTrue();
            }
        });

        softly.assertThat(element)
              .as("Found element")
              .isNotNull();

        // separated to satisfy gradle, who thinks it's ambiguous
        final RunConfiguration config = BuildUtil.createConfiguration(project, element);
        softly.assertThat(config)
              .as("Created configuration")
              .isNotNull()
              .isInstanceOf(JUnitConfiguration.class);

//        BuildUtil.findConfigurationFor(project, file, offset);
        softly.assertAll();
    }

    public void testOutputHandling() throws Exception {

        final int offset = 115;

        Project project = prepareProject(TESTABLE_PROJECT);
        PsiFile file = ProjectUtil.getPsiFile(project, TESTABLE);

        prepareElement(project, file, offset);

        final LoggingTestRunner runner = new LoggingTestRunner();
        final JUnitRunTestCommand junit =
                new JUnitRunTestCommand(project, runner, file, offset);

        // NB: DON'T execute; just feed output to handler. It'd be nice
        //  to test actual junit execution, but it doesn't want to cooperate
        //  in our unit test environment. There's a to-do below for making it work

        // object declarations
        junit.processPacket("O2:2 TM5 test237 org.intellivim.runnable.test.Testable1 :",
                runner);
        junit.processPacket("O0:2 TC37 org.intellivim.runnable.test.Testable2 :",
                runner);
        junit.processPacket("O1:2 TM5 test137 org.intellivim.runnable.test.Testable1 :",
                runner);

        // no active test yet
        assertThat(ActiveTestManager.getActiveTestRoot(project))
                .as("Active test node")
                .isNull();

        // start testing
        assertThat(runner.testingRoot).as("TestingRoot (before)").isNull();
        junit.processPacket("T0:2 1:0 2:0 \n", runner);
        assertThat(runner.testingRoot).as("TestingRoot")
                .isNotNull()
                .hasId("0")
                .hasName("org.intellivim.runnable.test.Testable")
                .hasKidsCount(2)
                .hasKidWithId("1")
                .hasKidWithId("2");

        // now active test
        assertThat(ActiveTestManager.getActiveTestRoot(project))
                .as("Active test node")
                .isNotNull();

        // start running a test
        junit.processPacket("S1:3 ", runner);
        junit.processPacket("I1:", runner);
        assertThat(runner.lastStateChangedNode)
                .hasId("1")
                .hasState(TestState.RUNNING);

        // finish running a test
        junit.processPacket("S1:1 1 3469944 3469944 ", runner);
        assertThat(runner.lastStateChangedNode)
                .hasId("1")
                .hasState(TestState.PASSED);

        // start a failing test
        junit.processPacket("S2:3 ", runner);
        junit.processPacket("I2:", runner);
        assertThat(runner.lastStateChangedNode)
                .hasId("2")
                .hasState(TestState.RUNNING);

        // fail the test
        junit.processPacket("S2:6 51 junit.framework.AssertionFailedError: test2 failed\n" +
                "869 \tat org.intellivim.runnable.test.Testable.test2(Testable.java:12)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat com.intellij.junit3.JUnit3IdeaTestRunner.doRun(JUnit3IdeaTestRunner.java:141)\n" +
                "\tat com.intellij.junit3.JUnit3IdeaTestRunner.startRunnerWithArgs(JUnit3IdeaTestRunner.java:52)\n" +
                "\tat com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:211)\n" +
                "\tat com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:67)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)", runner);
        assertThat(runner.lastStateChangedNode)
                .hasId("2")
                .hasState(TestState.FAILED);

        junit.processPacket("S2:1 1 3469944 3469944 ", runner);
        assertThat(runner.lastStateChangedNode)
                .hasId("2")
                .hasState(TestState.FAILED); // still failed, please

        // finish testing
        junit.processPacket("D7 ", runner);
        assertThat(runner.finished).as("Testing Finished")
                .isTrue();

        // no more active test
        assertThat(ActiveTestManager.getActiveTestRoot(project))
                .as("Active test node")
                .isNull();

    }

    // TODO get executing the test in junit environment to work
//    public void testClass() throws Exception {
//        final int offset = 132;
//
//        Project project = prepareProject(RUNNABLE_PROJECT);
//        PsiFile file = ProjectUtil.getPsiFile(project, TESTABLE);
//
//        prepareElement(project, file, offset);
//
//        final LoggingTestRunner runner = new LoggingTestRunner();
//        final SimpleResult result = (SimpleResult)
//                new JUnitRunTestCommand(project, runner, file, offset).execute();
//        assertSuccess(result);
//
//        if (!runner.awaitTermination(TIMEOUT))
//            fail("Unit test did not finish execution in time");
//
//        SoftAssertions softly = new SoftAssertions();
//        softly.assertThat(runner.cancelled).as("Run Cancelled").isFalse();
//
//        // TODO test output
//
//        softly.assertAll();
//    }

    @Override
    public Project prepareProject(final String projectName) throws Exception {
        final Project project = super.prepareProject(projectName);
//        final Module module = getModule(project);

        UIUtil.invokeAndWaitIfNeeded(new Runnable() {

            @Override
            public void run() {
                // TODO force-add junit runtime libs so it compiles
                // (they're omitted by default for unit tests, I guess)
//                ModuleRootModificationUtil.addModuleLibrary(module,
//                        "path/to/junit.jar");
            }
        });

//        CompilerTestUtil.saveApplicationSettings();

        return project;
    }

    /**
     * Prepare some extra mocks so we can correctly
     *  interact with JUnit stuff.
     *
     * @return The PsiElement at the offset in the file, if you want it
     */
    PsiElement prepareElement(final Project project,
            final PsiFile file, final int offset) throws Exception {
        return ApplicationManager.getApplication().runReadAction(
                new ThrowableComputable<PsiElement, Exception>() {
                    @Override
                    public PsiElement compute() throws Exception{
                        return prepareElementImpl(project, file, offset);
                    }
                }
        );
    }

    private PsiElement prepareElementImpl(Project project, PsiFile file, int offset) throws Exception {
        PsiElement element = file.findElementAt(offset);
        while (element != null
                && !(element instanceof PsiMethod
                || element instanceof PsiClass)) {
            element = element.getParent();
        }

        Location<PsiElement> el = PsiLocation.fromPsiElement(project, element);
        Location<PsiClass> classLocation = el.getAncestorOrSelf(PsiClass.class);
        PsiClass aClass = classLocation.getPsiElement();
        Module module = JavaExecutionUtil.findModule(aClass);

        final String simpleName = "TestCase";
        final String qName = "junit.framework." + simpleName;
        PsiClass mockClass = Mockito.mock(PsiClass.class);
        Mockito.when(mockClass.getQualifiedName()).thenReturn(qName);
        Mockito.when(mockClass.getName()).thenReturn(simpleName);
        Mockito.when(mockClass.isValid()).thenReturn(true);
        Mockito.when(mockClass.getProject()).thenReturn(project);
        Mockito.when(mockClass.getManager()).thenReturn(aClass.getManager());

        // pretend we're a real class; a lot of these are lies, but whatever...
        //  sometimes you have to tell a white lie to get the platform to be happy
        Mockito.when(mockClass.getSupers()).thenReturn(new PsiClass[0]);
        Mockito.when(mockClass.getSuperTypes()).thenReturn(new PsiClassType[0]);
        Mockito.when(mockClass.getTypeParameters()).thenReturn(new PsiTypeParameter[0]);
        Mockito.when(mockClass.getImplementsListTypes()).thenReturn(new PsiClassType[0]);
        Mockito.when(mockClass.getExtendsListTypes()).thenReturn(new PsiClassType[0]);
        Mockito.when(mockClass.getFields()).thenReturn(new PsiField[0]);
        Mockito.when(mockClass.getMethods()).thenReturn(new PsiMethod[0]);
        Mockito.when(mockClass.getInnerClasses()).thenReturn(new PsiClass[0]);
        Mockito.when(mockClass.findMethodsByName(
                anyString(),
                anyBoolean()
        )).thenReturn(new PsiMethod[0]);

        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        JavaPsiFacade spy = isMocked(facade)
            ? facade
            : Mockito.spy(facade);
        doReturn(mockClass).when(spy).findClass(
                argThat(equalTo(qName)),
                any(GlobalSearchScope.class));

        Field INSTANCE_KEY = JavaPsiFacade.class.getDeclaredField("INSTANCE_KEY");
        INSTANCE_KEY.setAccessible(true);
        NotNullLazyKey<JavaPsiFacade, Project> key =
                (NotNullLazyKey<JavaPsiFacade, Project>) INSTANCE_KEY.get(null);
        key.set(project, spy);

        if (!hasJunitExtensionPoint()) {
            Extensions.getRootArea().registerExtensionPoint(JUNIT_EP_NAME,
                    JUNIT_BEAN_NAME);
        }

        // make sure it worked
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(hasJunitExtensionPoint())
                .as("Has JUnit EP")
                .isTrue();

        GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(module, true);
        softly.assertThat(JavaPsiFacade.getInstance(project)
                .findClass("junit.framework.TestCase", scope))
                .as("TestCase")
                .isNotNull();

        softly.assertThat(module)
                .as("Containing class's module")
                .isNotNull();

        softly.assertAll();

        return element;
    }

    static boolean isMocked(JavaPsiFacade facade) {
        MockUtil util = new MockUtil();
        return util.isSpy(facade) || util.isMock(facade);
    }

    static boolean hasJunitExtensionPoint() {

        try {
            Extensions.getExtensions(JUNIT_EP_NAME);
            return true;
        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
            return false;
        }
    }

    /**
     * Our mocked AsyncTestRunner for testing
     */
    static class LoggingTestRunner
            extends RunTest.LoggingRunner
            implements AsyncTestRunner {

        TestNode testingRoot;
        TestNode lastStateChangedNode;
        boolean finished = false;

        @Override
        public void onStartTesting(TestNode root) {
            testingRoot = root;
        }

        @Override
        public void onTestOutput(final TestNode owner, final String output,
                final OutputType type) {

            // TODO
            System.out.println("OUTPUT[" + owner + "]=" + output);
        }

        @Override
        public void onTestStateChanged(final TestNode node) {
            lastStateChangedNode = node;
        }

        @Override
        public void onFinishTesting() {
            finished = true;
        }
    }
}
