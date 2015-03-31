package org.intellivim.java.command;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPackage;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.command.find.LocationResult;
import org.intellivim.inject.Inject;

import java.util.HashMap;

/**
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

    public JavaNewCommand(final Project project, final String type, final String name) {
        super(project);

        this.type = type;
        this.name = name;
    }

    @Override
    public Result execute() {
        // FIXME there may be multiple source directories...
        final PsiDirectory[] dirs = resolveNewDirectory();
        final PsiDirectory dir;
        if (dirs == null) {
            return SimpleResult.error("Invalid package");
        } else if (dirs.length == 0) {
            return SimpleResult.error("Unable to resolve package");
        } else if (dirs.length > 1) {
             // FIXME disambiguate directories
            dir = dirs[0];
        } else {
            dir = dirs[0];
        }

        final String templateName = resolveTemplateName();
        final String className = resolveClassName();
        final PsiClass created = JavaDirectoryService.getInstance()
                .createClass(dir, className, templateName, false);
        return SimpleResult.success(new LocationResult(created));
    }

    protected PsiDirectory[] resolveNewDirectory() {
        final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        final int lastPackageEnd = name.lastIndexOf('.');
        if (-1 != lastPackageEnd) {
            // separators included; assume that's the package
            final String packageName = name.substring(0, lastPackageEnd);
            PsiPackage psiPackage = psiFacade.findPackage(packageName);
            if (psiPackage != null) {
                return psiPackage.getDirectories();
            } else {
                // create it?
            }
        }

        if (file instanceof PsiJavaFile) {
            // same as the current file
//            return file.getContainingDirectory();
            return psiFacade.findPackage(((PsiJavaFile) file).getPackageName()).getDirectories();
        }

//        return project.getp
//        PsiManager.getInstance(project).dir
        // FIXME all of this
        return psiFacade.findPackage("").getDirectories();
    }

    private String resolveClassName() {
        final int lastPackageEnd = name.lastIndexOf('.');
        if (-1 == lastPackageEnd) {
            // no package
            return name;
        }

        return name.substring(lastPackageEnd + 1);
    }

    protected String resolveTemplateName() {
        final String resolved = TYPES_MAP.get(type);
        if (resolved == null) {
            throw new IllegalArgumentException("No such type: " + type);
        }

        return resolved;
    }
}
