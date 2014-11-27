package org.intellivim.core.command.impl;

import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.infos.CandidateInfo;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author dhleong
 */
public class Implementables extends ArrayList<Implementable> {

    private void addFrom(final Collection<CandidateInfo> collection) {
        for (final CandidateInfo info : collection) {
            Implementable impl = Implementable.from(info);
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

        final Implementables result = new Implementables();
        result.addFrom(override);
        result.addFrom(implement);

        // TODO sort them somehow?
        Collections.sort(result, new Comparator<Implementable>() {
            @Override
            public int compare(Implementable o1, Implementable o2) {
                return 0;
            }
        });

        return result;
    }
}
