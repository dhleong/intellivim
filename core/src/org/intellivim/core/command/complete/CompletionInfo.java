package org.intellivim.core.command.complete;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.javadoc.PsiDocComment;

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
                    buildParamsString(method) + "-> " + method.getReturnType().toString(),
                    buildDocComment(method));
        }

        System.out.println("Unexpected completion: " + el
                + " with element type " + el.getPsiElement().getClass());
        return null;
    }

    private static String buildDocComment(PsiMethod method) {
        PsiDocComment docComment = method.getDocComment();
        if (docComment == null)
            return ""; // never be null?

        return docComment.getText();
    }

    private static String buildParamsString(PsiMethod method) {
        return method.getParameterList().toString(); // will it blend?
    }
}
