package org.intellivim.java.command;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.Command;
import org.intellivim.core.command.test.AbstractRunTestCommand;
import org.intellivim.core.command.test.AsyncTestRunner;
import org.intellivim.core.util.BuildUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author dhleong
 */
@Command("junit")
public class JUnitRunTestCommand extends AbstractRunTestCommand {

    static final String JUNIT_CONFIG_CLASSNAME =
            "com.intellij.execution.junit.JUnitConfiguration";
    static final String JUNIT_CONSOLE_PROPS_CLASSNAME =
            "com.intellij.execution.junit2.ui.properties.JUnitConsoleProperties";

    static Constructor<? extends TestConsoleProperties> sJunitConsolePropertiesCtor;

    public JUnitRunTestCommand(final Project project,
           AsyncTestRunner runner, PsiFile file, int offset) {
        super(project, runner, file, offset);
    }

    @Override
    protected String getTestFrameworkName() {
        return "JUnit";
    }

    @Override
    protected TestConsoleProperties createProperties(final Project project,
            final Executor executor) {

        final RunConfiguration configuration =
                BuildUtil.createConfiguration(project, file, offset);

        // instanceof and klass.instanceOf both fail (WTF?)
        if (configuration == null
                || !JUNIT_CONFIG_CLASSNAME.equals(configuration.getClass().getName())) {
            return null;
        }

//        if (!(configuration instanceof JUnitConfiguration)) {
//            System.out.println("Got: " + configuration);
//            return null;
//        }

        // TODO cache class for faster acceptance; pre-fill cache


//        JUnitConfiguration config = (JUnitConfiguration) configuration;
//        return new JUnitConsoleProperties(config, executor);
        System.out.println("Go!" + configuration.getClass());
        return newJUnitConsoleProperties(configuration, executor);
//        return null;
    }

    static TestConsoleProperties newJUnitConsoleProperties(RunConfiguration config,
            Executor executor) {

        final Constructor<? extends TestConsoleProperties> ctor = getCtor(
                config.getClass());
        if (ctor == null)
            return null;

        try {
            return ctor.newInstance(config, executor);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    static Constructor<? extends TestConsoleProperties> getCtor(Class<?> configClass) {
        final Constructor<? extends TestConsoleProperties> cached =
                sJunitConsolePropertiesCtor;
        if (cached != null)
            return cached;

        try {

            final Class<? extends TestConsoleProperties> klass =
                    (Class<? extends TestConsoleProperties>)
                            configClass.getClassLoader().loadClass(
                                    JUNIT_CONSOLE_PROPS_CLASSNAME);

            final Constructor<? extends TestConsoleProperties> ctor =
                    klass.getConstructor(configClass, Executor.class);

            sJunitConsolePropertiesCtor = ctor;
            return ctor;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }
}
