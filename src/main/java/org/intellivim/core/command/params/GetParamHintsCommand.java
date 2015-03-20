package org.intellivim.core.command.params;

import com.intellij.codeInsight.hint.ShowParameterInfoContext;
import com.intellij.codeInsight.hint.ShowParameterInfoHandler;
import com.intellij.lang.Language;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhleong
 */
@Command("get_param_hints")
public class GetParamHintsCommand extends ProjectCommand {

    @Required @Inject PsiFile file;
    @Required int offset;

    public GetParamHintsCommand(Project project, final String filePath, final int offset) {
        super(project);

        file = ProjectUtil.getPsiFile(project, filePath);
        this.offset = offset;
    }

    @Override
    public Result execute() {

        final VimEditor editor = new VimEditor(project, file, offset);
        final int lbraceOffset = offset - 1;

        final PsiElement psiElement = file.findElementAt(offset);
        if (psiElement == null) return SimpleResult.error("No element at offset");

        final ShowParameterInfoContext context = new ShowParameterInfoContext(
                editor,
                project,
                file,
                offset,
                lbraceOffset
        );

        final Language language = psiElement.getLanguage();
        ParameterInfoHandler[] handlers = ShowParameterInfoHandler.getHandlers(
                project, language, file.getViewProvider().getBaseLanguage());
        if (handlers == null) handlers = new ParameterInfoHandler[0];

        final List<String> paramPresentations = new ArrayList<String>();
        for (final ParameterInfoHandler handler : handlers) {
            final Object element = handler.findElementForParameterInfo(context);
            if (element instanceof PsiElement) {
                final Object[] itemsToShow = context.getItemsToShow();
                PsiExpressionList list = (PsiExpressionList) element;
                for (int i=0; i < itemsToShow.length; i++) {
                    final DummyParameterInfoUIContext infoContext = new DummyParameterInfoUIContext(list, i);
                    final String presentation = infoToString(itemsToShow[i], handler, infoContext);
                    paramPresentations.add(presentation);
                }
            }
        }

        return SimpleResult.success(paramPresentations);
    }

    static String infoToString(Object p, ParameterInfoHandler handler, DummyParameterInfoUIContext context) {

        handler.updateUI(p, context);

        if (context.isPresentationValid()) {
            return context.getPresentedString();
        } else {
            return null;
        }
    }

}
