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
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.local.FileWatcher;
import com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.WindowManagerImpl;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.ui.content.MessageView;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.UIUtil;
import org.intellivim.core.model.VimDocument;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Lots of stuff imported from ProjectManagerImpl to
 *  prevent extraneous UI from appearing
 *
 * @author dhleong
 */
public class ProjectUtil {

    static final HashMap<String, Project> sProjectCache = new HashMap<String, Project>();

    public static Project ensureProject(String projectPath) {

        final Project project = getProject(projectPath);
        if (project == null)
            throw new IllegalArgumentException("Couldn't find project at " + projectPath);
        if (project.isDisposed())
            throw new IllegalArgumentException("Project " + project + " was already disposed!");

        return project;
    }

    public static Project getProject(final String projectPath) {
        final ProjectManagerEx mgr = ProjectManagerEx.getInstanceEx();

        // this doesn't really work; use our own cache
//        // check for an open one first
//        for (Project p : mgr.getOpenProjects()) {
//            System.out.println("Check: " + p.getProjectFilePath());
//            if (p.getProjectFilePath().equals(projectPath))
//                return p;
//        }

        final Project cached = sProjectCache.get(projectPath);
        if (cached != null) {
            // we can't use this as a real cache for some reason;
            //  we have to "close" any previously-opened projects
            //  and re-open each time to prevent unit test failures
//            return cached;
            markProjectClosed(mgr, cached);
            deallocateFrame(cached);
        }

        try {
            if (!new File(projectPath).exists())
                return null;

            final Ref<Project> projectRef = new Ref<Project>();
            UIUtil.invokeAndWaitIfNeeded(new Runnable() {

                @Override
                public void run() {
                    try {
                        Project project = mgr.convertAndLoadProject(projectPath);
                        projectRef.set(project);

                        DirectoryIndex index = DirectoryIndex.getInstance(project);
                        waitForFileWatcher(project, index);
                        waitForStartup(project);
                        markProjectOpened(mgr, project);
                        allocateFrame(project);
                        mockMessageView(project);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            final Project project = projectRef.get();
            if (project == null)
                throw new IOException(":(");

            sProjectCache.put(projectPath, project);
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

    static void waitForFileWatcher(@NotNull Project project, final DirectoryIndex index) {
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

    static void waitForStartup(@NotNull Project project) {
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

//                startupManager.runPostStartupActivitiesFromExtensions();

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

    static void markProjectOpened(ProjectManagerEx mgr, Project project) {
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

    /**
     * We need this available for the compile step of
     *  compileAndRun to work
     */
    private static void allocateFrame(final Project project) {
        WindowManager mgr = WindowManager.getInstance();
        if (null != mgr.getFrame(project)) {
//            System.out.println("Frame already allocated");
            return; // already done
        }

        if (!(mgr instanceof WindowManagerImpl)) {
            // unit test?
            return;
        }

        IdeFrameImpl impl = ((WindowManagerImpl) mgr).allocateFrame(project);
        impl.setVisible(false);
    }

    /**
     * Project seems to be used as a key, but doesn't
     *  have a proper hashCode (wtf). So, all we can do is
     *  clean up the old one
     */
    private static void deallocateFrame(final Project project) {
        WindowManager mgr = WindowManager.getInstance();
        IdeFrameImpl frame = (IdeFrameImpl) mgr.getFrame(project);
        if (null == frame) {
//            System.out.println("Frame already deallocated");
            return; // already done
        }

        if (!(mgr instanceof WindowManagerImpl)) {
            // unit test?
            return;
        }

        ((WindowManagerImpl) mgr).releaseFrame(frame);
    }

    /**
     * Also needed to prevent NPE in CompilerTask
     */
    private static void mockMessageView(final Project project) {
        final ContentManager mgr = ContentFactory.SERVICE.getInstance()
                .createContentManager(false, project);

        mgr.addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentAdded(final ContentManagerEvent event) {
                System.out.println("ADD: " + event.getContent());
            }

            @Override
            public void contentRemoved(final ContentManagerEvent event) {

            }

            @Override
            public void contentRemoveQuery(final ContentManagerEvent event) {

            }

            @Override
            public void selectionChanged(final ContentManagerEvent event) {

            }
        });

        final MutablePicoContainer mutable =
                (MutablePicoContainer) project.getPicoContainer();

        final String key = MessageView.class.getName();
        mutable.unregisterComponent(key);

        // this is the dumbest interface ever. (Object, Object)? Seriously?
        mutable.registerComponentInstance(key,
                new MessageView() {

            @Override
            public ContentManager getContentManager() {
                return mgr;
            }

            @Override
            public void runWhenInitialized(final Runnable runnable) {
                runnable.run();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void markProjectClosed(ProjectManagerEx mgr, Project project) {
        // gross reflection
        try {
            Field f = ProjectManagerImpl.class.getDeclaredField("myOpenProjects");
            f.setAccessible(true);

            Method cacheOpenProjects = ProjectManagerImpl.class.getDeclaredMethod("cacheOpenProjects");
            cacheOpenProjects.setAccessible(true);

            List<Project> myOpenProjects = (List<Project>) f.get(mgr);
            synchronized (myOpenProjects) {
                if (!myOpenProjects.contains(project))
                    return; // done

                myOpenProjects.remove(project);
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
