package org.intellivim.java.command;

import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.codeInsight.generation.actions.BaseGenerateActionUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.inject.Inject;

import java.util.List;

/**
 * @author dhleong
 */
@Command("java_generate")
public class JavaGenerateCommand extends ProjectCommand {

    @Required @Inject PsiFile file;
    @Required int offset;

    public JavaGenerateCommand(Project project, PsiFile file, int offset) {
        super(project);

        this.file = file;
        this.offset = offset;
    }

    @Override
    public Result execute() {

        final EditorEx editor = createEditor(file, offset);
        List<AnAction> actions = generateActionsForFile(editor, file);
        if (actions.isEmpty()) {
            return SimpleResult.error("No generate actions available here");
        }

        for (AnAction a : actions) {
            System.out.println(a.getClass() + " -> " + a);
        }

        return SimpleResult.success(ContainerUtil.map(actions,
                new Function<AnAction, String>() {
            @Override
            public String fun(AnAction anAction) {
                return anAction.getTemplatePresentation().getText();
            }
        }));
    }

    static List<AnAction> generateActionsForFile(final Editor editor, final PsiFile file) {
        final Project proj = file.getProject();

        final ActionManagerEx man = ActionManagerEx.getInstanceEx();

        // NB: this doesn't get all of them:
//        final DefaultActionGroup group = (DefaultActionGroup)
//                man.getAction(IdeActions.GROUP_GENERATE);
//        AnAction[] actions = group.getChildren(null);

        // so we'll do it by hand:
        String[] ids = man.getActionIds("Generate");
        List<AnAction> actions = ContainerUtil.map(ids, new Function<String, AnAction>() {
            @Override
            public AnAction fun(String id) {
                return man.getAction(id);
            }
        });

        return ContainerUtil.filter(actions, new Condition<AnAction>() {
            @Override
            public boolean value(AnAction anAction) {
                if (!(anAction instanceof BaseGenerateAction)) {
                    return false;
                }

                final BaseGenerateAction generateAction = (BaseGenerateAction) anAction;
                return BaseGenerateActionUtil.isValidForFile(
                        generateAction, proj, editor, file);
            }
        });
    }
}
