package org.intellivim.core.command.complete.formatters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import org.intellivim.core.command.complete.CompletionInfo;
import org.intellivim.core.util.FormatUtil;

/**
 * @author dhleong
 */
public class MethodCompletionInfo extends CompletionInfo<PsiMethod> {

    public MethodCompletionInfo(final LookupElement lookup,
            final PsiMethod el) {
        super(TYPE_METHOD, lookup, el);
    }

    @Override
    protected String formatBody(final LookupElement lookup, final PsiMethod method) {

        final PsiParameterList parameterList = method.getParameterList();
        final String parens = (parameterList.getParameters().length == 0)
                ? "()"
                : "(";
        return super.formatBody(lookup, method) + parens;
    }

    @Override
    protected String formatDetail(final LookupElement lookup, final PsiMethod method) {
        return FormatUtil.buildParamsList(method)
                + "-> " + FormatUtil.getTypeName(method.getReturnType());
    }
}
