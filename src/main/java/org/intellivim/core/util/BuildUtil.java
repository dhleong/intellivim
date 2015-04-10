package org.intellivim.core.util;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.MapDataContext;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author dhleong
 */
public class BuildUtil {
    public static final Key<RunConfiguration> RUN_CONFIGURATION =
            CompileStepBeforeRun.RUN_CONFIGURATION;
    public static final Key<String> RUN_CONFIGURATION_TYPE_ID =
            CompileStepBeforeRun.RUN_CONFIGURATION_TYPE_ID;

    @NonNls protected static final String MAKE_PROJECT_ON_RUN_KEY = "makeProjectOnRun";

    /**
     * From CompileStepBeforeRun
     *
     * @return True if started
     */
    public static boolean compileProject(final Project myProject,
            final RunConfiguration configuration,
            final CompileStatusNotification callback) {

        if (!(configuration instanceof RunProfileWithCompileBeforeLaunchOption)) {
            return false;
        }

        if (configuration instanceof RunConfigurationBase
                && ((RunConfigurationBase)configuration)
                    .excludeCompileBeforeLaunchOption()) {
            return false;
        }

        final RunProfileWithCompileBeforeLaunchOption runConfiguration =
                (RunProfileWithCompileBeforeLaunchOption)configuration;
        final CompileScope scope;
        final CompilerManager compilerManager = CompilerManager.getInstance(myProject);

        if (Boolean.valueOf(System.getProperty(MAKE_PROJECT_ON_RUN_KEY,
                Boolean.FALSE.toString()))) {
            // user explicitly requested whole-project make
            scope = compilerManager.createProjectCompileScope(myProject);
        } else {
            final Module[] modules = runConfiguration.getModules();
            if (modules.length > 0) {
                for (Module module : modules) {
                    if (module == null) {
//                        LOG.error("RunConfiguration should not return null modules. Configuration=" + runConfiguration.getName() + "; class=" +
//                                runConfiguration.getClass().getName());
                    }
                }
                scope = compilerManager.createModulesCompileScope(modules, true, true);
            } else {
                scope = compilerManager.createProjectCompileScope(myProject);
            }
        }

        if (!myProject.isDisposed()) {
            scope.putUserData(RUN_CONFIGURATION, configuration);
            scope.putUserData(RUN_CONFIGURATION_TYPE_ID, configuration.getType().getId());

            UIUtil.invokeAndWaitIfNeeded(new Runnable() {

                @Override
                public void run() {
//                    compilerManager.make(scope, callback);

                    System.out.println("Compiling...");

                    // NB We should prefer "make" since (I think)
                    //  it doesn't use "forceCompile," but it also just
                    //  doesn't work at all, so....
                    compilerManager.compile(scope, callback);
                }
            });
        }

        return true;
    }

//    public static RunnerAndConfigurationSettings findConfigurationFor(
//            final Project project, final PsiFile file, final int offset) {
//        Editor editor = new VimEditor(project, file, offset);
//        PsiElement element = file.findElementAt(offset);
//        if (element == null) {
//            return null;
//        }
//
//        Location<?> loc = new PsiLocation<PsiElement>(project, element);
//        System.out.println("loc=" + loc);
//        DataContext context = new VimDataContext(project, editor, loc);
//        ConfigurationContext configContext =
//                ConfigurationContext.getFromContext(context);
//        System.out.println("context=" + configContext);
//
//        RunnerAndConfigurationSettings configuration =
//                RuntimeConfigFinderDelegate.createConfiguration(loc, configContext);
////        assertThat(configuration)
////                .isNotNull();
//        System.out.println(configuration);
//
//        List<ConfigurationFromContext> configs =
//                RuntimeConfigFinderDelegate
//                        .getConfigurationsFromContext(loc, configContext);
//        System.out.println("configs: " + configs);
//        for (ConfigurationFromContext config : configs) {
//            System.out.println(" --> " + config);
//        }
//        return configuration;
//    }

    public static final <T extends RunConfiguration> T createConfiguration(
            @NotNull Project project, @NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        while (element != null &&
                !(element instanceof PsiClass
                    || element instanceof PsiMethod)) {
            element = element.getParent();
        }
        if (element == null) {
            element = file;
        }
        return createConfiguration(project, element, new MapDataContext());
    }


    public static final <T extends RunConfiguration> T createConfiguration(
            @NotNull Project project, @NotNull PsiElement psiElement) {
        return createConfiguration(project, psiElement, new MapDataContext());
    }

    @SuppressWarnings("unchecked")
    public static <T extends RunConfiguration> T createConfiguration(
            @NotNull final Project project, @NotNull final PsiElement psiElement,
            @NotNull final MapDataContext dataContext) {
        return ApplicationManager.getApplication().runReadAction(new Computable<T>() {
            @Override
            public T compute() {
                ConfigurationContext context = createContext(
                        project, psiElement, dataContext);
                RunnerAndConfigurationSettings settings = context.getConfiguration();
                return settings == null
                        ? null
                        : (T) settings.getConfiguration();
            }
        });
    }

    public static ConfigurationContext createContext(
            @NotNull Project project, @NotNull PsiElement psiClass,
            @NotNull MapDataContext dataContext) {
        dataContext.put(CommonDataKeys.PROJECT, project);
        if (LangDataKeys.MODULE.getData(dataContext) == null) {
            dataContext.put(LangDataKeys.MODULE,
                    ModuleUtilCore.findModuleForPsiElement(psiClass));
        }
        dataContext.put(Location.DATA_KEY, PsiLocation.fromPsiElement(psiClass));
        return ConfigurationContext.getFromContext(dataContext);
    }

}
