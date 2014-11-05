package org.intellivim.core.util;

import com.intellij.CommonBundle;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.impl.local.FileWatcher;
import com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Lots of stuff imported from ProjectManagerImpl to
 *  prevent extraneous UI from appearing
 *
 * Created by dhleong on 11/5/14.
 */
public class ProjectUtil {
    public static Project getProject(String projectPath) {
        ProjectManager mgr = ProjectManager.getInstance();

        // check for an open one first
        for (Project p : mgr.getOpenProjects()) {
            System.out.println("Check: " + p);
            if (p.getProjectFilePath().equals(projectPath))
                return p;
        }

        ProjectManagerImpl impl = (ProjectManagerImpl) mgr;

        try {
            Project project = impl.convertAndLoadProject(projectPath);
            DirectoryIndex index = DirectoryIndex.getInstance(project);
            waitForFileWatcher(project, index);
            waitForStartup(project);
            return project;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void waitForFileWatcher(@NotNull Project project, final DirectoryIndex index) {
        LocalFileSystem fs = LocalFileSystem.getInstance();
        if (!(fs instanceof LocalFileSystemImpl)) return;

        final FileWatcher watcher = ((LocalFileSystemImpl)fs).getFileWatcher();
        if (!watcher.isOperational() || !watcher.isSettingRoots()) return;

//        LOG.info("FW/roots waiting started");
        Task.Modal task = new Task.Modal(project, ProjectBundle.message("project.load.progress"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(ProjectBundle.message("project.load.waiting.watcher"));
                if (indicator instanceof ProgressWindow) {
                    ((ProgressWindow)indicator).setCancelButtonText(CommonBundle.message("button.skip"));
                }
                while ((watcher.isSettingRoots() || !index.isInitialized()) && !indicator.isCanceled()) {
                    TimeoutUtil.sleep(10);
                }
//                LOG.info("FW/roots waiting finished");
            }
        };
        ProgressManager.getInstance().run(task);
    }

    private static void waitForStartup(@NotNull Project project) {
        final StartupManagerImpl startupManager = (StartupManagerImpl) StartupManager.getInstance(project);
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                startupManager.runStartupActivities();

                // dumb mode should start before post-startup activities
                // only when startCacheUpdate is called from UI thread, we can guarantee that
                // when the method returns, the application has entered dumb mode
                UIUtil.invokeAndWaitIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        startupManager.startCacheUpdate();
                    }
                });

                startupManager.runPostStartupActivitiesFromExtensions();

                // this doesn't seem to do anything useful, and gunks up stderr
//                UIUtil.invokeLaterIfNeeded(new Runnable() {
//                    @Override
//                    public void run() {
//                        startupManager.runPostStartupActivities();
//                    }
//                });
            }
        }, ProjectBundle.message("project.load.progress"), true, project);
    }
}
