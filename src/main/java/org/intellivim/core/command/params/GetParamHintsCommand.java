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

    private transient VimEditor editor;

    public GetParamHintsCommand(Project project, final String filePath, final int offset) {
        super(project);

        file = ProjectUtil.getPsiFile(project, filePath);
        this.offset = offset;
    }

    @Override
    public Result execute() {

        editor = new VimEditor(project, file, offset);
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
                buildParamPresentations((PsiElement) element, handler, context, paramPresentations);
                break;
            }
        }

        return SimpleResult.success(paramPresentations);
    }

    private void buildParamPresentations(PsiElement element, ParameterInfoHandler handler,
            ShowParameterInfoContext context, List<String> paramPresentations) {
        final Object[] itemsToShow = context.getItemsToShow();
        final DummyUpdateParameterInfoContext updateContext =
                new DummyUpdateParameterInfoContext(project, file, editor, itemsToShow);

        // first pass, build the UI
        final int len = itemsToShow.length;
        PsiExpressionList list = (PsiExpressionList) element;
        for (int i=0; i < len; i++) {
            final DummyParameterInfoUIContext infoContext = new DummyParameterInfoUIContext(list, i);
            handler.updateUI(itemsToShow[i], infoContext);

            updateContext.add(infoContext);
        }

        // I guess?
        updateContext.setParameterOwner(list);

        // second pass, run the update
        for (int i=0; i < len; i++) {
            handler.updateParameterInfo(list, updateContext);

            if (updateContext.isUIComponentEnabled(i)) {
                paramPresentations.add(updateContext.getPresentedString(i));
            }
        }
    }


}
