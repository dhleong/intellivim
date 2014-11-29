package org.intellivim.core.command.impl;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.FileUtil;
import org.intellivim.core.util.IntelliVimUtil;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author dhleong
 */
public class Implementables extends ArrayList<Implementable> {

    final Project project;
    final VimEditor editor;
    final PsiClass targetClass;

    Implementables(Project project, VimEditor editor, PsiClass targetClass) {
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

//                for (PsiMethodMember member : members) {
//                    reformatMethod(member);
//                }
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

    void reformatMethod(PsiMethodMember member) {
        new ReformatCodeProcessor(project,
                targetClass.getContainingFile(),
                member.getElement().getTextRange(),
                false)
            .runWithoutProgress();

        // sometimes it complains with:
        // "Document and psi file texts should be equal"
        //  even if they are
    }


    private void addFrom(final Collection<CandidateInfo> collection) {
        for (final CandidateInfo info : collection) {
            Implementable impl = Implementable.from(this, info);
            if (impl != null)
                add(impl);
        }
    }

    public static Implementables collectFrom(Project project, String file, int offset)
            throws IllegalArgumentException {
        final PsiFile psiFile = ProjectUtil.getPsiFile(project, file);
        final VimEditor editor = new VimEditor(project, psiFile, offset);

        final PsiClass aClass = OverrideImplementUtil.getContextClass(project, editor, psiFile, true);
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
