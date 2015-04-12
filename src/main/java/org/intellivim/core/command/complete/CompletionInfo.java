package org.intellivim.core.command.complete;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiVariable;
import org.intellivim.core.command.complete.formatters.ClassCompletionInfo;
import org.intellivim.core.command.complete.formatters.FieldCompletionInfo;
import org.intellivim.core.command.complete.formatters.MethodCompletionInfo;
import org.intellivim.core.command.complete.formatters.PackageCompletionInfo;
import org.intellivim.core.command.complete.formatters.VariableCompletionInfo;
import org.intellivim.core.util.FormatUtil;
import org.jetbrains.annotations.TestOnly;

/**
 * @author dhleong
 */
public abstract class CompletionInfo<T extends PsiElement> {

    public static final int TYPE_METHOD = 0;
    public static final int TYPE_CLASS = 1;
    public static final int TYPE_FIELD = 2;
    public static final int TYPE_PACKAGE = 3;
    public static final int TYPE_VARIABLE = 4;

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

    /** test constructor */
    private CompletionInfo(int type, String body, String detail, String doc) {
        this.type = type;
        this.body = body;
        this.detail = detail;
        this.doc = doc;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CompletionInfo)) return false;

        final CompletionInfo<?> that = (CompletionInfo<?>) o;

        if (type != that.type) return false;
        if (!body.equals(that.body)) return false;
        if (detail != null ? !detail.equals(that.detail) : that.detail != null)
            return false;
        return !(doc != null ? !doc.equals(that.doc) : that.doc != null);

    }

    @Override
    public int hashCode() {
        int result = type;
        result = 31 * result + body.hashCode();
        result = 31 * result + (detail != null ? detail.hashCode() : 0);
        result = 31 * result + (doc != null ? doc.hashCode() : 0);
        return result;
    }

    protected String formatBody(final LookupElement lookup, final T el) {
        if (el instanceof PsiNamedElement) {
            final String name = ((PsiNamedElement) el).getName();
            if (!StringUtil.isEmpty(name)) {
                // use it. This fixes cases like Arrays.asList() where
                //  the lookup string contains the class (but we've
                //  already typed it)
                return name;
            }
        }

        // fallback
        return lookup.getLookupString();
    }

    protected String formatDoc(final T el) {
        return FormatUtil.buildDocComment((PsiDocCommentOwner) el);
    }

    protected abstract String formatDetail(final LookupElement lookup, final T el);

    @Override
    public String toString() {
        return String.format("%s[body=%s;detail=%s;doc=%s]",
                getClass().getSimpleName(), body, detail, doc);
    }

    public static CompletionInfo<?> from(LookupElement el) {
        final PsiElement psi = el.getPsiElement();
        if (psi instanceof PsiMethod) {
            return new MethodCompletionInfo(el, (PsiMethod) psi);
        } else if (psi instanceof PsiClass) {
            return new ClassCompletionInfo(el, (PsiClass) psi);
        } else if (psi instanceof PsiField) {
            return new FieldCompletionInfo(el, (PsiField) psi);
        } else if (psi instanceof PsiPackage) {
            return new PackageCompletionInfo(el, (PsiPackage) psi);
        } else if (psi instanceof PsiVariable) {
            return new VariableCompletionInfo(el, (PsiVariable) psi);
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

    @TestOnly
    public static CompletionInfo<?> forMethod(
            final String body, final String detail, final String doc) {
        return new CompletionInfo<PsiElement>(TYPE_METHOD, body, detail, doc) {
            @Override
            protected String formatDetail(final LookupElement lookup,
                    final PsiElement el) {
                return detail;
            }
        };
    }
}
