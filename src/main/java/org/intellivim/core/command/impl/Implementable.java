package org.intellivim.core.command.impl;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.infos.CandidateInfo;
import org.intellivim.core.util.FormatUtil;

/**
 * @author dhleong
 */
public class Implementable {

    private transient final PsiElement element;
    private final CharSequence description;

    public Implementable(PsiElement element, CharSequence description) {
        this.element = element;
        this.description = description;
    }

    public static Implementable from(CandidateInfo info) {
        PsiElement element = info.getElement();
        if (element == null)
            return null;

        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;
            return new Implementable(element, describe(method));
        }

        System.err.println("Unexpected element type " + element.getClass());
        return null;
    }

    private static CharSequence describe(PsiMethod method) {
        final StringBuilder builder = new StringBuilder(128);
        final CharSequence modifiers = FormatUtil.buildModifierList(method);
        if (modifiers.length() > 0) {
            builder.append(modifiers)
                   .append(' ');
        }

        builder.append(FormatUtil.getTypeName(method.getReturnType()))
               .append(' ')
               .append(method.getName())
               .append(FormatUtil.buildParamsList(method));

        return builder;
    }
}
