package org.intellivim.core.command.complete.formatters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiVariable;
import org.intellivim.core.command.complete.CompletionInfo;

/**
 * @author dhleong
 */
public class VariableCompletionInfo extends CompletionInfo<PsiVariable> {

    public VariableCompletionInfo(final LookupElement el, final PsiVariable psi) {
        super(TYPE_VARIABLE, el, psi);
    }

    @Override
    protected String formatDoc(final PsiVariable el) {
        return getType(el) + " " + el.getName() + ";";
    }

    @Override
    protected String formatDetail(final LookupElement lookup, final PsiVariable el) {
        return getType(el);
    }

    private static String getType(final PsiVariable el) {
        return el.getType().getPresentableText();
    }

}
