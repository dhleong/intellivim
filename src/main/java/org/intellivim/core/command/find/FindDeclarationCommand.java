package org.intellivim.core.command.find;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

/**
 * @author dhleong
 */
@Command("find_declaration")
public class FindDeclarationCommand extends ProjectCommand {

    @Required PsiFile file;
    @Required int offset;

    public FindDeclarationCommand(Project project, String file, int offset) {
        super(project);

        this.file = ProjectUtil.getPsiFile(project, file);
        this.offset = offset;
    }

    @Override
    public Result execute() {

        final PsiReference ref = file.findReferenceAt(offset);
        if (ref == null)
            return SimpleResult.error("No element found.");

        final PsiElement lookup = ref.resolve();

        if (lookup == null)
            return SimpleResult.error("Definition not found.");

        return SimpleResult.success(new LocationResult(lookup));
    }
}
