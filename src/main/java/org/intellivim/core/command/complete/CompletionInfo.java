package org.intellivim.core.command.complete;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import org.intellivim.core.command.complete.formatters.ClassCompletionInfo;
import org.intellivim.core.command.complete.formatters.FieldCompletionInfo;
import org.intellivim.core.command.complete.formatters.MethodCompletionInfo;
import org.intellivim.core.util.FormatUtil;

/**
 * @author dhleong
 */
public abstract class CompletionInfo<T extends PsiElement> {

    public static final int TYPE_METHOD = 0;
    public static final int TYPE_CLASS = 1;
    public static final int TYPE_FIELD = 2;

    public final int type;

    /** "Body" of the completion; what gets inserted */
    public final String body;

    /** Detailed info about the completion */
    public final String detail;

    /** Documentation */
    public final String doc;

    protected CompletionInfo(final int type, final LookupElement lookup, final T el) {
        this.type = type;
        this.body = formatBody(lookup, el);
        this.detail = formatDetail(lookup, el);
        this.doc = formatDoc(el);
    }

    protected String formatBody(final LookupElement lookup, final T el) {
        return lookup.getLookupString();
    }

    protected String formatDoc(final T el) {
        return FormatUtil.buildDocComment((PsiDocCommentOwner) el);
    }

    protected abstract String formatDetail(final LookupElement lookup, final T el);

    public static CompletionInfo from(LookupElement el) {
        final PsiElement psi = el.getPsiElement();
        if (psi instanceof PsiMethod) {
            return new MethodCompletionInfo(el, (PsiMethod) psi);
        } else if (psi instanceof PsiClass) {
            return new ClassCompletionInfo(el, (PsiClass) psi);
        } else if (psi instanceof PsiField) {
            return new FieldCompletionInfo(el, (PsiField) psi);
        }

        // any other (useful) info types?

        if (el != null) {
            final Class<?> type = psi == null ? null : psi.getClass();
            System.out.println("Unexpected completion: " + el
                    + " with element type " + type);
        } else {
            System.err.println("Null LookupElement?!");
        }
        return null;
    }
}
