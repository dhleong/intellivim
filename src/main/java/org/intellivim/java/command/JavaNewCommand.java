package org.intellivim.java.command;

import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.find.LocationResult;
import org.intellivim.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * If there are multiple possible directories to place the
 *  file in, we will return <code>{dirs: [...]}</code>.
 *  The chosen directory should be provided in a new
 *  command in the "dir" field, including all the same
 *  previous fields as well.
 *
 * If `file` is provided, and no packages are used in
 *  `name`, we will put the new class in the same package
 *  with the current file. This is slightly different
 *  from IntelliJ (which just appends packages to the
 *  current package), but without making an extra command,
 *  or making the command needlessly complicated, there's
 *  no good way to specify an absolute package.
 *
 * @author dhleong
 */
@Command("java_new")
public class JavaNewCommand extends ProjectCommand {

    static final HashMap<String, String> TYPES_MAP = new HashMap<String, String>();
    static {
        TYPES_MAP.put("@interface", "AnnotationType");
        TYPES_MAP.put("class", "Class");
        TYPES_MAP.put("enum", "Enum");
        TYPES_MAP.put("interface", "Interface");
    }

    @Required String type;
    @Required String name;

    /* Optional */@Inject PsiFile file;
    /* Optional */String dir;

    public JavaNewCommand(final Project project, final String type, final String name) {
        super(project);

        this.type = type;
        this.name = name;
    }

    @Override
    public Result execute() {
        return ApplicationManager.getApplication().runWriteAction(new Computable<Result>() {
            @Override
            public Result compute() {
                return doExecute();
            }
        });
    }

    Result doExecute() {
        final PsiDirectory[] dirs = resolveNewDirectory();
        final PsiDirectory dir;
        if (dirs == null) {
            return SimpleResult.error("Invalid package");
        } else if (dirs.length == 0) {
            return SimpleResult.error("Unable to resolve package");
        } else if (dirs.length > 1) {
            dir = resolveChosenDirectory(dirs);
            if (dir == null) {
                // ambiguous; let the user resolve it
                return SimpleResult.success(
                        ContainerUtil.newHashMap(
                                Collections.singletonList("dirs"),
                                Collections.singletonList(mapDirs(dirs))
                        )
                );
            }
        } else {
            // unambiguous
            dir = dirs[0];
        }

        // be up to date, please
        // (if we delete outside of IntelliJ, it gets confused)
        dir.getVirtualFile().refresh(false, true);

        final String templateName = resolveTemplateName();
        final String className = resolveClassName();
        final PsiClass created = JavaDirectoryService.getInstance()
                .createClass(dir, className, templateName, false);
        return SimpleResult.success(new LocationResult(created));
    }

    protected PsiDirectory resolveChosenDirectory(PsiDirectory[] choices) {
        if (StringUtil.isEmpty(dir)) {
            // user didn't provide a choice
            return null;
        }

        for (final PsiDirectory psiDirectory : choices) {
            if (dir.equals(psiDirectory.getVirtualFile().getPath())) {
                return psiDirectory;
            }
        }

        // not a valid choice
        return null;
    }

    protected PsiDirectory[] resolveNewDirectory() {
        final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        final int lastPackageEnd = name.lastIndexOf('.');
        final String packageName = name.substring(0, lastPackageEnd);
        if (-1 != lastPackageEnd) {
            // separators included; assume that's the package
            final PsiPackage psiPackage = psiFacade.findPackage(packageName);
            if (psiPackage != null) {
                return psiPackage.getDirectories();
            }
        }


        if (file instanceof PsiJavaFile) {
            // same as the current file
            final PsiPackage psiPackage = psiFacade.findPackage(
                    ((PsiJavaFile) file).getPackageName());
            if (psiPackage != null) {
                return psiPackage.getDirectories();
            }
        }

        final PsiPackage defaultPackage = psiFacade.findPackage("");
        if (defaultPackage == null) {
            throw new IllegalArgumentException("Unable to determine default package");
        }

        final PsiDirectory[] sourceDirectories = defaultPackage.getDirectories();
        if (-1 == lastPackageEnd) {
            // default package
            return sourceDirectories;
        }

        if (!PsiDirectoryFactory.getInstance(project).isValidPackageName(packageName)) {
            throw new IllegalArgumentException(packageName + " is not a valid package name");
        }

        final List<PsiDirectory> packageDirs =
                new ArrayList<PsiDirectory>(sourceDirectories.length);

        for (final PsiDirectory srcRoot : sourceDirectories) {
            // TODO If we created a directory here but don't use it, we should clean up after
            final PsiDirectory dir = DirectoryUtil
                    .createSubdirectories(packageName, srcRoot, ".");
            packageDirs.add(dir);
        }

        return packageDirs.toArray(sourceDirectories);
    }

    protected String resolveTemplateName() {
        final String resolved = TYPES_MAP.get(type);
        if (resolved == null) {
            throw new IllegalArgumentException("No such type: " + type);
        }

        return resolved;
    }

    private List<String> mapDirs(final PsiDirectory[] dirs) {
        return ContainerUtil.map(dirs, new Function<PsiDirectory, String>() {
            @Override
            public String fun(final PsiDirectory psiDirectory) {
                return psiDirectory.getVirtualFile().getPath();
            }
        });
    }

    private String resolveClassName() {
        final int lastPackageEnd = name.lastIndexOf('.');
        if (-1 == lastPackageEnd) {
            // no package
            return name;
        }

        return name.substring(lastPackageEnd + 1);
    }

}
