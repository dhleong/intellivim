package org.intellivim.core.command.complete;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.intellivim.core.util.FormatUtil;

/**
 * Created by dhleong on 11/7/14.
 */
public class CompletionInfo {

    public static final int TYPE_METHOD = 0;

    public final int type;
    public final String body;
    public final String detail;
    public final String doc;

    CompletionInfo(int type, final String body, final String detail, final String doc) {
        this.type = type;
        this.body = body;
        this.detail = detail;
        this.doc = doc;
    }

    public static CompletionInfo from(LookupElement el) {
        if (el.getPsiElement() instanceof PsiMethod) {
            final PsiMethod method = (PsiMethod) el.getPsiElement();
            return new CompletionInfo(TYPE_METHOD,
                    el.getLookupString(),
                    FormatUtil.buildParamsList(method) + "-> " + FormatUtil.getTypeName(method.getReturnType()),
                    FormatUtil.buildDocComment(method));
        }

        // FIXME other info types

        if (el != null) {
            final PsiElement psi = el.getPsiElement();
            final Class<?> type = psi == null ? null : psi.getClass();
            System.out.println("Unexpected completion: " + el
                    + " with element type " + type);
        } else {
            System.err.println("Null LookupElement?!");
        }
        return null;
    }
}
