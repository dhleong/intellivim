package org.intellivim.core.command.find;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Find implementations for the element at the given offset
 * @author dhleong
 */
@Command("find_implementations")
public class FindImplementationsCommand extends ProjectCommand {

    @Required @Inject PsiFile file;
    @Required int offset;

    public FindImplementationsCommand(final Project project, final String filePath,
            int offset) {
        super(project);

        file = ProjectUtil.getPsiFile(project, filePath);
        this.offset = offset;
    }

    @Override
    public Result execute() {

        final PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return SimpleResult.error("No element under the cursor");
        }

        
        final Query<PsiElement> query = DefinitionsScopedSearch.search(element);
        final List<LocationResult> results = new ArrayList<LocationResult>();
        query.forEach(new Processor<PsiElement>() {
            @Override
            public boolean process(final PsiElement psiElement) {
                results.add(new LocationResult(psiElement));
                return true;
            }
        });

        return SimpleResult.success(results);
    }
}
