package org.intellivim.core.command.complete.formatters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import org.intellivim.core.command.complete.CompletionInfo;

/**
 * @author dhleong
 */
public class ClassCompletionInfo extends CompletionInfo<PsiClass> {
    public ClassCompletionInfo(final LookupElement el, final PsiClass psi) {
        super(TYPE_CLASS, el, psi);
    }

    @Override
    protected String formatDetail(final LookupElement lookup, final PsiClass el) {
        return el.getQualifiedName();
    }
}
