package org.intellivim.core.command.complete;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
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

    private static String buildDocComment(PsiMethod method) {
        PsiDocComment docComment = method.getDocComment();
        if (docComment == null)
            return ""; // never be null?

        return docComment.getText();
    }

    private static String buildParamsString(PsiMethod method) {
        StringBuilder builder = new StringBuilder(128);
        builder.append('(');

        boolean first = true;
        PsiParameterList parameterList = method.getParameterList();
        for (PsiParameter param : parameterList.getParameters()) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }

            builder.append(getType(param.getType()))
                   .append(' ')
                   .append(param.getName());
        }

        builder.append(')');
        return builder.toString();
    }

    private static String getType(PsiType type) {
        return type.getPresentableText();
    }

    public static CompletionInfo from(LookupElement el) {
        if (el.getPsiElement() instanceof PsiMethod) {
            final PsiMethod method = (PsiMethod) el.getPsiElement();
            return new CompletionInfo(TYPE_METHOD,
                    el.getLookupString(),
                    buildParamsString(method) + "-> " + getType(method.getReturnType()),
                    buildDocComment(method));
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
