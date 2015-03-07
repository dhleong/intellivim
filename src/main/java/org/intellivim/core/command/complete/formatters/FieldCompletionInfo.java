package org.intellivim.core.command.complete.formatters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiField;
import org.intellivim.core.command.complete.CompletionInfo;

/**
 * @author dhleong
 */
public class FieldCompletionInfo extends CompletionInfo<PsiField> {
    public FieldCompletionInfo(final LookupElement el, final PsiField psi) {
        super(TYPE_FIELD, el, psi);
    }

    @Override
    protected String formatDetail(final LookupElement lookup, final PsiField field) {
        return field.getType().getPresentableText() + " " + field.getName();
    }
}
