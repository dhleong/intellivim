package org.intellivim.core.command.run;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author dhleong
 */
public class LaunchManager {

    static Map<String, ProcessHandler> sRunningProcs =
            new HashMap<String, ProcessHandler>();


    public static synchronized String allocateId(
            Project project, RunnerAndConfigurationSettings setting) {
        final String base = project.getName() + ":" + setting.getName();
        if (!sRunningProcs.containsKey(base)) {
            sRunningProcs.put(base, null);
            return base;
        }

        String alternate;
        int i = 0;
        do {
            alternate = base + ":" + i;
        } while (sRunningProcs.containsKey(alternate));
        sRunningProcs.put(alternate, null);
        return alternate;
    }

    public static synchronized void register(
            final String launchId, final ProcessHandler handler) {
        sRunningProcs.put(launchId, handler);
        handler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(ProcessEvent event) {
                super.processTerminated(event);

                onTerminated(launchId);
            }
        });
    }

    /** Pretty much only for testing */
    public static synchronized ProcessHandler get(String launchId) {
        return sRunningProcs.get(launchId);
    }

    public static synchronized void terminate(String launchId) {
        final ProcessHandler handler = sRunningProcs.remove(launchId);
        if (handler != null) // could just be allocated
            handler.destroyProcess();
    }

    public static synchronized void terminateAll() {
        Iterator<ProcessHandler> iterator = sRunningProcs.values().iterator();
        while (iterator.hasNext()) {
            final ProcessHandler handler = iterator.next();
            iterator.remove();

            if (handler != null)
                handler.destroyProcess();
        }
    }

    private static synchronized void onTerminated(String launchId) {
        sRunningProcs.remove(launchId);
    }

}
