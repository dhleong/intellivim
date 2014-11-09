package org.intellivim.java.command;

import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.java.JavaImportOptimizer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.intellivim.core.Result;
import org.intellivim.core.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

import java.io.File;


/**
 * Created by dhleong on 11/8/14.
 */
public class OptimizeImportsCommand {

    public Result execute(String projectPath, String filePath, int offset) {
        final Project project = ProjectUtil.getProject(projectPath);
        if (project == null)
            return SimpleResult.error("Couldn't find project at " + projectPath);

        File file = new File(project.getBasePath(), filePath);
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        ImportOptimizer optimizer = new JavaImportOptimizer();
        if (!optimizer.supports(psiFile)) {
            return SimpleResult.error(file.getAbsolutePath() + " is not supported by " + optimizer);
        }
        Runnable action = optimizer.processFile(psiFile);
        action.run();
        return SimpleResult.success();
    }

}
