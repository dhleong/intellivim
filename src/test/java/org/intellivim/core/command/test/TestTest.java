package org.intellivim.core.command.test;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.junit.TestClassConfigurationProducer;
import com.intellij.execution.junit.TestMethodConfigurationProducer;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import org.assertj.core.api.SoftAssertions;
import org.intellivim.SimpleResult;
import org.intellivim.UsableSdkTestCase;
import org.intellivim.core.command.run.RunTest;
import org.intellivim.core.util.BuildUtil;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.java.command.junit.JUnitRunTestCommand;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.equalTo;
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
        System.out.println("Invoke: " + runnable);
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

        Project project = prepareProject(RUNNABLE_PROJECT);
        PsiFile file = ProjectUtil.getPsiFile(project, TESTABLE);

        PsiElement element = prepareElement(project, file, offset);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(JUnitUtil.isTestMethodOrConfig((PsiMethod) element))
              .as("isTestMethodOrConfig")
              .isTrue();

        softly.assertThat(element)
              .as("Found element")
              .isNotNull();

        softly.assertThat(BuildUtil.createConfiguration(project, element))
              .as("Created configuration")
              .isNotNull()
              .isInstanceOf(JUnitConfiguration.class);

//        BuildUtil.findConfigurationFor(project, file, offset);
        softly.assertAll();
    }

    public void testClass() throws Exception {
        final int offset = 132;

        Project project = prepareProject(RUNNABLE_PROJECT);
        PsiFile file = ProjectUtil.getPsiFile(project, TESTABLE);

        prepareElement(project, file, offset);

        final LoggingTestRunner runner = new LoggingTestRunner();
        final SimpleResult result = (SimpleResult)
                new JUnitRunTestCommand(project, runner, file, offset).execute();
        assertSuccess(result);

        if (!runner.awaitTermination(TIMEOUT))
            fail("Unit test did not finish execution in time");

        // TODO test output
    }

    /**
     * Prepare some extra mocks so we can correctly
     *  interact with JUnit stuff.
     *
     * @return The PsiElement at the offset in the file, if you want it
     */
    PsiElement prepareElement(Project project, PsiFile file, int offset) throws Exception{

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
        Mockito.when(mockClass.getFields()).thenReturn(new PsiField[0]);
        Mockito.when(mockClass.getMethods()).thenReturn(new PsiMethod[0]);
        Mockito.when(mockClass.getInnerClasses()).thenReturn(new PsiClass[0]);
        Mockito.when(mockClass.findMethodsByName(
                anyString(),
                anyBoolean()
        )).thenReturn(new PsiMethod[0]);

        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        JavaPsiFacade spy = Mockito.spy(facade);
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

    static boolean hasJunitExtensionPoint() {

        try {
            Extensions.getExtensions(JUNIT_EP_NAME);
            return true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Our mocked AsyncTestRunner for testing
     */
    static class LoggingTestRunner
            extends RunTest.LoggingRunner
            implements AsyncTestRunner {
        
        @Override
        public void onStartTesting(TestNode root) {
            System.out.println("START " + root);
        }

        @Override
        public void onTestOutput(final TestNode owner, final String output,
                final OutputType type) {

            System.out.println("OUTPUT[" + owner + "]=" + output);
        }

        @Override
        public void onTestStateChanged(final TestNode node) {
            System.out.println("STATE CHANGE:" + node);
        }

        @Override
        public void onFinishTesting() {

            System.out.println("FINISH TESTING");
        }
    }
}
