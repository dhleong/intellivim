package org.intellivim.core.util;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;

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

                    // NB We should prefer "make" since (I think)
                    //  it doesn't use "forceCompile," but it also just
                    //  doesn't work at all, so....
                    compilerManager.compile(scope, callback);
                }
            });
        }

        return true;
    }
}
