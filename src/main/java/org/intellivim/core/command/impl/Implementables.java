package org.intellivim.core.command.impl;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.core.util.FileUtil;
import org.intellivim.core.util.IntelliVimUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author dhleong
 */
public class Implementables extends ArrayList<Implementable> {

    final Project project;
    final EditorEx editor;
    final PsiClass targetClass;

    Implementables(Project project, EditorEx editor, PsiClass targetClass) {
        this.project = project;
        this.editor = editor;
        this.targetClass = targetClass;
    }

    public Implementable find(String signature) {
        for (Implementable i : this) {
            if (signature.equals(i.getSignature()))
                return i;
        }

        return null;
    }

    public Implementables select(String...signatures) throws IllegalArgumentException {
        Implementables filtered = new Implementables(project, editor, targetClass);

        // just be lazy
        for (String signature : signatures) {
            Implementable found = find(signature);
            if (found == null)
                throw new IllegalArgumentException("No such signature `" + signature + "`");

            filtered.add(found);
        }

        return filtered;
    }

    /**
     * Implement every method signature in this object
     */
    public void implementAll() {
//        for (Implementable item : this)
//            item.execute();

        List<CandidateInfo> candidates = ContainerUtil.map(this,
                new Function<Implementable, CandidateInfo>() {
            @Override
            public CandidateInfo fun(Implementable implementable) {
                return implementable.candidate;
            }
        });

        implementCandidates(candidates);
    }

    void implementCandidates(List<CandidateInfo> candidates) {
        List<PsiMethod> psiMethods = OverrideImplementUtil.overrideOrImplementMethodCandidates(
                targetClass, candidates, true);

        final List<PsiMethodMember> members = ContainerUtil.map(psiMethods,
                new Function<PsiMethod, PsiMethodMember>() {
                    @Override
                    public PsiMethodMember fun(PsiMethod method) {
                        return new PsiMethodMember(method);
                    }
                });

        IntelliVimUtil.runWriteCommand(new Runnable() {

            @Override
            public void run() {
                OverrideImplementUtil.overrideOrImplementMethodsInRightPlace(
                        editor, targetClass,
                        members, false
                );

                // these must be done in separate commit blocks
                //  to avoid errors

                for (PsiMethodMember member : members) {
                    cleanOverrideAnnotation(member);
                }
                FileUtil.commitChanges(editor);

                for (PsiMethodMember member : members) {
                    reformatMethod(editor, member);
                }
                FileUtil.commitChanges(editor);

            }
        });
    }

    /**
     * The default generation uses @java.lang.Override for some reason;
     *  Let's clean that up
     */
    void cleanOverrideAnnotation(PsiMethodMember member) {

        final PsiMethod[] ms = targetClass.findMethodsBySignature(
                member.getElement(), false);
        for (PsiMethod m : ms) {
            PsiModifierList modifierList = m.getModifierList();
            PsiAnnotation annotation = modifierList.findAnnotation(Override.class.getName());
            if (null != annotation) {
                annotation.delete();
                modifierList.addAnnotation("Override");
            }
        }
    }

    void reformatMethod(EditorEx editor, PsiMethodMember member) {

        final PsiMethod[] ms = targetClass.findMethodsBySignature(
                member.getElement(), false);
        final TextRange range;
        if (ms.length > 0) {
            range = ms[0].getTextRange();
        } else {
            // just in case, but the above should always work
            int offset = editor.getCaretModel().getOffset();
            range = new TextRange(offset, offset + member.getElement().getTextLength());
        }

        // NB: The setUncommited hack no longer works
        //  in IntelliJ 14. The tests pass without it,
        //  but now the complaint mentioned below is back....
        // TODO Can we remove the complaint another way?
//        // NB see the comments in this method
//        ((VimDocument) editor.getDocument()).setUncommited(true);

        new ReformatCodeProcessor(project,
                targetClass.getContainingFile(),
                range,
                false)
            .runWithoutProgress();

//        // the above sometimes complains with:
//        // "Document and psi file texts should be equal"
//        //  even if they are. The `setUncommited` hack seems
//        //  to prevent that now
//
//        ((VimDocument) editor.getDocument()).setUncommited(false);
    }


    private void addFrom(final Collection<CandidateInfo> collection) {
        for (final CandidateInfo info : collection) {
            Implementable impl = Implementable.from(this, info);
            if (impl != null)
                add(impl);
        }
    }

    public static Implementables collectFrom(EditorEx editor, PsiFile file)
            throws IllegalArgumentException {

        final Project project = editor.getProject();
        final PsiClass aClass = OverrideImplementUtil.getContextClass(project, editor, file, true);
        if (aClass == null) {
            throw new IllegalArgumentException("No context for implement");
        }

        // collectFrom all candidates
        final Collection<CandidateInfo> override =
                OverrideImplementUtil.getMethodsToOverrideImplement(aClass, false);
        final Collection<CandidateInfo> implement =
                OverrideImplementUtil.getMethodsToOverrideImplement(aClass, true);

        final Implementables result = new Implementables(project, editor, aClass);
        result.addFrom(override);
        result.addFrom(implement);

        // TODO sort them somehow?
//        Collections.sort(result, new Comparator<Implementable>() {
//            @Override
//            public int compare(Implementable o1, Implementable o2) {
//                return 0;
//            }
//        });

        return result;
    }
}
