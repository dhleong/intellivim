package org.intellivim.core.command.params;

import com.intellij.codeInsight.hint.ShowParameterInfoContext;
import com.intellij.codeInsight.hint.ShowParameterInfoHandler;
import com.intellij.lang.Language;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
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

    public static class ParamHints {
        public final List<String> hints;
        public final int start;

        private ParamHints(final List<String> hints, final int start) {
            this.hints = hints;
            this.start = start;
        }
    }

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

        final PsiElement psiElement = file.findElementAt(offset);
        if (psiElement == null) return SimpleResult.error("No element at offset");

        final ShowParameterInfoContext context = new ShowParameterInfoContext(
                editor,
                project,
                file,
                offset,
                findParametersStart(psiElement)
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

        return SimpleResult.success(new ParamHints(paramPresentations,
                context.getParameterListStart()));
    }

    private int findParametersStart(final PsiElement cursorElement) {
        int nesting = 1; // we assume we're "in" a paren already
        int start = offset - 1;
        do {
            final PsiElement atStart = file.findElementAt(start);
            if (atStart == null)
                continue;

            final String startText = atStart.getText();

            if (")".equals(startText)) {
                nesting++;
            } else if ("(".equals(startText)) {
                nesting--;

                if (nesting <= 0) {
                    // now, we make sure this offset is contained within
                    //  an expression that also contains the cursor. If
                    //  so, we found the paren we want to start from;
                    //  if not, we may be outside of a paren, so just
                    //  break out and return offset-1
                    if (findExpressionContaining(cursorElement, start)) {
                        return start;
                    }
                    break;
                }
            }
        } while (--start > 0);

        return offset - 1;
    }

    /**
     * Walk up the tree looking for a PsiExpression that encompasses the offset
     * @return True if we found one, else false
     */
    private boolean findExpressionContaining(final PsiElement cursorElement,
            final int offset) {

        PsiElement el = cursorElement;
        while (!(el instanceof PsiMethod || el instanceof PsiClass)) {
            if (el instanceof PsiExpression && el.getTextRange().contains(offset)) {
                return true;
            }

            el = el.getParent();
        }

        return false;
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
