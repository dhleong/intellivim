package org.intellivim.java.command;

import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.java.JavaImportOptimizer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;


/**
 * Created by dhleong on 11/8/14.
 */
@Command("java_import_optimize")
public class OptimizeImportsCommand extends ProjectCommand {

    @Required String file;
    @Required int offset;

    public OptimizeImportsCommand(String projectPath, String filePath, int offset) {
        project = ProjectUtil.ensureProject(projectPath);
        file = filePath;
        this.offset = offset;
    }

    @Override
    public Result execute() {
        final VirtualFile virtualFile = ProjectUtil.getVirtualFile(project, file);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        ImportOptimizer optimizer = new JavaImportOptimizer();
        if (!optimizer.supports(psiFile)) {
            return SimpleResult.error(file + " is not supported by " + optimizer);
        }
        Runnable action = optimizer.processFile(psiFile);
        action.run();
        return SimpleResult.success();
    }

}
