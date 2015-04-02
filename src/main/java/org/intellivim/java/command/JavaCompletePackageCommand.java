package org.intellivim.java.command;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

/**
 * @author dhleong
 */
@Command("java_complete_package")
public class JavaCompletePackageCommand
        extends ProjectCommand {

    @Required String input;

    public JavaCompletePackageCommand(final Project project, String input) {
        super(project);

        this.input = input;
    }

    @Override
    public Result execute() {
        final String input = StringUtil.trimEnd(this.input, ".");
        PsiPackage pkg = JavaPsiFacade.getInstance(project).findPackage(input);
        if (pkg == null) {
            // perhaps we're not finished typing a package
            final int lastPackage = input.lastIndexOf('.');
            final String previousPackageName = lastPackage == -1
                ? ""
                : input.substring(0, lastPackage);
            final PsiPackage previousPkg = JavaPsiFacade.getInstance(project)
                    .findPackage(previousPackageName);
            if (previousPkg == null) {
                return SimpleResult.error("Invalid package");
            }

            pkg = previousPkg;
        }

        return SimpleResult.success(
                ContainerUtil.map(pkg.getSubPackages(), new Function<PsiPackage, String>() {
                    @Override
                    public String fun(final PsiPackage psiPackage) {
                        return psiPackage.getQualifiedName();
                    }
                })
        );
    }
}
