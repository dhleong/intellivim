package org.intellivim.core.util;

import com.intellij.CommonBundle;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.local.FileWatcher;
import com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.UIUtil;
import org.intellivim.core.model.VimDocument;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Lots of stuff imported from ProjectManagerImpl to
 *  prevent extraneous UI from appearing
 *
 * Created by dhleong on 11/5/14.
 */
public class ProjectUtil {

    public static Project ensureProject(String projectPath) {

        final Project project = getProject(projectPath);
        if (project == null)
            throw new IllegalArgumentException("Couldn't find project at " + projectPath);

        return project;
    }

    public static Project getProject(String projectPath) {
        ProjectManagerEx mgr = ProjectManagerEx.getInstanceEx();

        // check for an open one first
        for (Project p : mgr.getOpenProjects()) {
            System.out.println("Check: " + p.getProjectFilePath());
            if (p.getProjectFilePath().equals(projectPath))
                return p;
        }

        try {
            if (!new File(projectPath).exists())
                return null;

            Project project = mgr.convertAndLoadProject(projectPath);
            DirectoryIndex index = DirectoryIndex.getInstance(project);
            waitForFileWatcher(project, index);
            waitForStartup(project);
            markProjectOpened(mgr, project);
            return project;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static PsiFile getPsiFile(Project project, String filePath) {
        return getPsiFile(project, getVirtualFile(project, filePath));
    }

    public static PsiFile getPsiFile(Project project, VirtualFile file) {
        return PsiManager.getInstance(project).findFile(file);
    }

    public static VirtualFile getVirtualFile(Project project, String filePath) {
        final File file = new File(project.getBasePath(), filePath);

        // load the VirtualFile and ensure it's up to date
        final VirtualFile virtual = LocalFileSystem.getInstance()
                .refreshAndFindFileByIoFile(file);
        LocalFileSystem.getInstance().refreshFiles(Arrays.asList(virtual));
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtual);

        // we do this eagerly so FileDocumentManger#getCachedDocument will
        //  return the exact same instance that we want to use
        VimDocument.getInstance(psiFile);
        return virtual;
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

    private static void markProjectOpened(ProjectManagerEx mgr, Project project) {
        // gross reflection
        try {
            Field f = ProjectManagerImpl.class.getDeclaredField("myOpenProjects");
            f.setAccessible(true);

            Method cacheOpenProjects = ProjectManagerImpl.class.getDeclaredMethod("cacheOpenProjects");
            cacheOpenProjects.setAccessible(true);

            List<Project> myOpenProjects = (List<Project>) f.get(mgr);
            synchronized (myOpenProjects) {
                if (myOpenProjects.contains(project))
                    return; // done

                myOpenProjects.add(project);
                cacheOpenProjects.invoke(mgr);
            }

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
