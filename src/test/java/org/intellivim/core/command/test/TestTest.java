package org.intellivim.core.command.test;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.junit.TestClassConfigurationProducer;
import com.intellij.execution.junit.TestMethodConfigurationProducer;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
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
import com.intellij.util.Processor;
import org.assertj.core.api.SoftAssertions;
import org.intellivim.UsableSdkTestCase;
import org.intellivim.core.util.BuildUtil;
import org.intellivim.core.util.ProjectUtil;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;

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

    public void testClass() throws Exception {
        final int offset = 132;

        Project project = prepareProject(RUNNABLE_PROJECT);
        PsiFile file = ProjectUtil.getPsiFile(project, TESTABLE);

//        BuildUtil.findConfigurationFor(project, file, offset);

        PsiElement element = file.findElementAt(offset);
        while (element != null
                && !(element instanceof PsiMethod
                    || element instanceof PsiClass)) {
            element = element.getParent();
        }

        SoftAssertions softly = new SoftAssertions();

        Location<PsiElement> el = PsiLocation.fromPsiElement(project, element);
        Location<PsiClass> classLocation = el.getAncestorOrSelf(PsiClass.class);
        PsiClass aClass = classLocation.getPsiElement();
        Module module = JavaExecutionUtil.findModule(aClass);
        System.out.println(module);
        System.out.println("Dependencies: " + Arrays.asList(
                ModuleRootManager.getInstance(module)
                    .orderEntries().classes().getRoots()));
//        GlobalSearchScope scope2 = module.getModuleWithDependenciesAndLibrariesScope(false);
        GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(module, true);
        ModuleRootManager.getInstance(module).orderEntries().forEachLibrary(
                new Processor<Library>() {
                    @Override
                    public boolean process(final Library library) {
                        for (String each : library.getUrls(OrderRootType.CLASSES)) {
                            System.out.println("CLass!" + each);
                        }
                        return true;
                    }
                }
        );
        System.out.println("Scope=" + scope);

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

//        DumbServiceImpl dumb = (DumbServiceImpl) DumbService.getInstance(project);
//        boolean wasDumb = dumb.isDumb();
//        dumb.setDumb(true);
        softly.assertThat(JavaPsiFacade.getInstance(project)
                  .findClass("junit.framework.TestCase", scope))
              .as("TestCase")
              .isNotNull();
//        dumb.setDumb(wasDumb);

        softly.assertThat(module)
              .as("Containing class's module")
              .isNotNull();

        softly.assertThat(JUnitUtil.isTestMethodOrConfig((PsiMethod) element))
              .as("isTestMethodOrConfig")
              .isTrue();

        softly.assertThat(element)
              .as("Found element")
              .isNotNull();

        softly.assertThat(BuildUtil.createConfiguration(project, element))
              .as("Created configuration")
              .isNotNull();

        softly.assertAll();
    }

}
