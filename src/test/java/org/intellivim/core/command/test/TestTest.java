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
import com.intellij.execution.testframework.sm.runner.events.TestFailedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestIgnoredEvent;
import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent;
import com.intellij.execution.testframework.sm.runner.events.TestStartedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteStartedEvent;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
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
import org.intellivim.java.command.JUnitRunTestCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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


        // make sure it worked
        SoftAssertions softly = new SoftAssertions();

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

    /**
     * Our mocked AsyncTestRunner for testing
     */
    static class LoggingTestRunner
            extends RunTest.LoggingRunner
            implements AsyncTestRunner{
        @Override
        public void onStartTesting() {
            System.out.println("START");
        }

        @Override
        public void onTestsCountInSuite(final int count) {

            System.out.println("COUNT " + count);
        }

        @Override
        public void onTestStarted(@NotNull final TestStartedEvent testStartedEvent) {

            System.out.println("START " + testStartedEvent);
        }

        @Override
        public void onTestFinished(@NotNull final TestFinishedEvent testFinishedEvent) {

            System.out.println("FINISH " + testFinishedEvent);
        }

        @Override
        public void onTestFailure(@NotNull final TestFailedEvent testFailedEvent) {

            System.out.println("FAIL " + testFailedEvent);
        }

        @Override
        public void onTestIgnored(@NotNull final TestIgnoredEvent testIgnoredEvent) {

        }

        @Override
        public void onTestOutput(@NotNull final TestOutputEvent testOutputEvent) {

        }

        @Override
        public void onSuiteStarted(
                @NotNull final TestSuiteStartedEvent suiteStartedEvent) {

            System.out.println("SUITE START" + suiteStartedEvent);
        }

        @Override
        public void onSuiteFinished(
                @NotNull final TestSuiteFinishedEvent suiteFinishedEvent) {

            System.out.println("SUITE STOP" + suiteFinishedEvent);
        }

        @Override
        public void onUncapturedOutput(@NotNull final String text, final Key outputType) {

        }

        @Override
        public void onError(@NotNull final String localizedMessage,
                @Nullable final String stackTrace, final boolean isCritical) {

            System.out.println("ERROR " + localizedMessage);
        }

        @Override
        public void onFinishTesting() {

            System.out.println("FINISH TESTING");
        }
    }
}
