package com.intellij.codeInsight.generation.actions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author dhleong
 */
public class BaseGenerateActionUtil extends BaseGenerateAction {

    static Method isValidForFile;
    static {
        try {
            isValidForFile = BaseGenerateAction.class.getDeclaredMethod(
                    "isValidForFile", Project.class, Editor.class, PsiFile.class);
            isValidForFile.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    BaseGenerateActionUtil() {
        super(null);
    }

    public static boolean isValidForFile(BaseGenerateAction a,
                                         Project project, Editor editor,
                                         PsiFile file) {
        final Method isValidForFile = BaseGenerateActionUtil.isValidForFile;
        if (isValidForFile == null) {
            // shouldn't happen, but for sanity's sake...
            return true;
        }

        try {
            return (Boolean) isValidForFile.invoke(a, project, editor, file);
        } catch (IllegalAccessException e) {
            // shouldn't happen...
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            // shouldn't happen...
            throw new RuntimeException(e);
        }
    }
}
