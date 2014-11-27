package org.intellivim.core.util;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.javadoc.PsiDocComment;

/**
 * @author dhleong
 */
public class FormatUtil {
    public static String buildDocComment(PsiMethod method) {
        PsiDocComment docComment = method.getDocComment();
        if (docComment == null)
            return ""; // never be null?

        return docComment.getText();
    }

    public static CharSequence buildParamsList(PsiMethod method) {
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

            builder.append(getTypeName(param.getType()))
                   .append(' ')
                   .append(param.getName());
        }

        builder.append(')');
        return builder.toString();
    }

    public static CharSequence getTypeName(PsiType type) {
        return type.getPresentableText();
    }

    public static CharSequence buildModifierList(PsiMethod method) {
        return method.getModifierList().getText();
    }
}
