package org.intellivim.core.command.problems.org.intellivim.core.command;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.lang.documentation.DocumentationProvider;
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
@Command("get_documentation")
public class GetDocumentationCommand extends ProjectCommand {

    @Required String file;
    @Required int offset;

    public GetDocumentationCommand(Project project, String file, int offset) {
        super(project);
        this.file = file;
        this.offset = offset;
    }

    @Override
    public Result execute() {

        final PsiFile psiFile = ProjectUtil.getPsiFile(project, file);
        final PsiReference ref = psiFile.findReferenceAt(offset);
        final PsiElement element = ref.getElement();
        final PsiElement lookup = ref.resolve();

        final DocumentationProvider provider = DocumentationManager.getProviderFromElement(element);
        final String doc = provider.generateDoc(lookup, element);

        if (doc == null)
            return SimpleResult.error("Could not find documentation");

        if (doc.isEmpty())
            return SimpleResult.error("No documentation for " + element);

        return SimpleResult.success(stripHtml(doc));
    }

    static final String stripHtml(String input) {
        return input.replaceAll("(<br[ /]*>|</?PRE>)", "\n")
                    .replaceAll("<li>", "\n - ")
                    .replaceAll("<style .*style>", "")
                    .replaceAll("<[^>]+>", "")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .trim();
    }
}
