package org.intellivim.core.command.impl;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.infos.CandidateInfo;
import org.intellivim.core.util.FormatUtil;

import java.util.Arrays;

/**
 * @author dhleong
 */
public class Implementable {

    private transient final Implementables context;
    transient final CandidateInfo candidate;
    private final CharSequence description;

    public Implementable(Implementables context, CandidateInfo candidate, CharSequence description) {
        this.context = context;
        this.candidate = candidate;
        this.description = description;
    }

    /**
     * Implement the method
     */
    public void execute() {

        context.implementCandidates(Arrays.asList(candidate));
    }

    public String getSignature() {
        return description.toString();
    }

    @Override
    public String toString() {
        return candidate + "->" + getSignature();
    }

    public static Implementable from(Implementables context, CandidateInfo info) {
        PsiElement element = info.getElement();
        if (element == null)
            return null;

        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;
            return new Implementable(context, info, describe(method));
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
