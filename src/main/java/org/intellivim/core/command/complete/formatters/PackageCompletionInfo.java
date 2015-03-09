package org.intellivim.core.command.complete.formatters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiPackage;
import org.intellivim.core.command.complete.CompletionInfo;

/**
 * @author dhleong
 */
public class PackageCompletionInfo
        extends CompletionInfo<PsiPackage> {
    public PackageCompletionInfo(final LookupElement lookup, final PsiPackage el) {
        super(TYPE_PACKAGE, lookup, el);
    }

    @Override
    protected String formatDetail(final LookupElement lookup, final PsiPackage el) {
        return el.getQualifiedName();
    }

    @Override
    protected String formatDoc(final PsiPackage el) {
        return el.getQualifiedName();
    }
}
