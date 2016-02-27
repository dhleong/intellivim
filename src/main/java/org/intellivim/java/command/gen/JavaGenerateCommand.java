package org.intellivim.java.command.gen;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.intellivim.java.command.gen.JavaGenerateOptionsCommand.generateActionsForFile;

/**
 * @author dhleong
 */
@Command("java_generate")
public class JavaGenerateCommand extends ProjectCommand {

    @Required @Inject PsiFile file;
    @Required int offset;
    @Required String action;

    public JavaGenerateCommand(Project project) {
        super(project);
    }

    @Override
    public Result execute() {
        final EditorEx editor = createEditor(file, offset);
        final AnAction chosenAction = selectAction(editor, action);
        if (chosenAction == null) {
            return SimpleResult.error("Action " + action + " not found");
        }

        if (performAction(chosenAction, getDataContext(editor))) {
            return SimpleResult.success();
        } else {
            return SimpleResult.error("Unable to perform action");
        }
    }


    @Nullable AnAction selectAction(EditorEx editor, @NotNull String action) {
        List<AnAction> actions = generateActionsForFile(editor, file);
        for (AnAction a : actions) {
            if (action.equals(a.getTemplatePresentation().getText())) {
                return a;
            }
        }

        return null;
    }

    static boolean performAction(@NotNull AnAction action, @NotNull DataContext context) {

        final AnActionEvent event = new AnActionEvent(null, context, "",
                action.getTemplatePresentation(),
                ActionManager.getInstance(), 0);
        action.update(event);
        if (event.getPresentation().isEnabled()) {
            action.actionPerformed(event);
            return true;
        }

        return false;
    }
}
