package org.intellivim.core.command.problems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.apache.http.util.TextUtils;
import org.intellivim.core.util.FileUtil;

/**
 * Created by dhleong on 11/12/14.
 */
public class QuickFixDescriptor {

    final String id;
    final String description;
    final int start;
    final int end;

    final transient HighlightInfo.IntentionActionDescriptor descriptor;

    private QuickFixDescriptor(String id,
           String description,
           int start, int end,
           HighlightInfo.IntentionActionDescriptor descriptor) {
        this.id = id;
        this.description = description;
        this.start = start;
        this.end = end;
        this.descriptor = descriptor;
    }

    public String getDescription() {
        return description;
    }

    public IntentionAction getFix() {
        return descriptor.getAction();
    }

    public void execute(final Project project, final Editor editor, final PsiFile file) {
        final IntentionAction action = descriptor.getAction();
        final Runnable runnable = prepareExecuteAction(action, project, editor, file);
        if (action.startInWriteAction()) {
            ApplicationManager.getApplication().runWriteAction(runnable);
        } else {
            runnable.run();
        }
    }

    private Runnable prepareExecuteAction(final IntentionAction action,
              final Project project, final Editor editor, final PsiFile file) {
        return new Runnable() {

                @Override
                public void run() {
                    action.invoke(project, editor, file);

                    FileUtil.commitChanges(editor);
                }
            };
    }

    static QuickFixDescriptor from(String id,
           HighlightInfo.IntentionActionDescriptor descriptor, TextRange range) {

        final String desc;
        if (!TextUtils.isEmpty(descriptor.getDisplayName())) {
            desc = descriptor.getDisplayName();
        } else if (!TextUtils.isEmpty(descriptor.getAction().getText())) {
            desc = descriptor.getAction().getText();
        } else if (!TextUtils.isEmpty(descriptor.getAction().getFamilyName())) {
            desc = descriptor.getAction().getFamilyName();
        } else {
            desc = descriptor.getAction().getClass().getSimpleName();
        }

        return new QuickFixDescriptor(id,
                desc,
                range.getStartOffset(),
                range.getEndOffset(),
                descriptor);
    }
}
