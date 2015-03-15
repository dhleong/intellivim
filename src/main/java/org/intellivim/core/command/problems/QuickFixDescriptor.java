package org.intellivim.core.command.problems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiFile;
import org.apache.http.util.TextUtils;
import org.intellivim.core.util.FileUtil;

/**
 * Basic QuickFixDescriptor
 *
 * @author dhleong
 */
public class QuickFixDescriptor {

    final String id;
    final String description;
    final int start;
    final int end;

    final transient HighlightInfo.IntentionActionDescriptor descriptor;

    QuickFixDescriptor(String id,
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

    public final Object execute(final Project project, final Editor editor,
            final PsiFile file, final String arg) throws QuickFixException {
        final IntentionAction action = descriptor.getAction();
        final ThrowableComputable<?, QuickFixException> runnable =
                prepareExecuteAction(action, project, editor, file, arg);
        if (action.startInWriteAction()) {
            return ApplicationManager.getApplication().runWriteAction(runnable);
        } else {
            return runnable.compute();
        }
    }

    final ThrowableComputable<?, QuickFixException> prepareExecuteAction(final IntentionAction action,
            final Project project, final Editor editor, final PsiFile file,
            final String arg) {
        return new ThrowableComputable<Object, QuickFixException>() {

            @Override
            public Object compute() throws QuickFixException {
                final Object result = invoke(action, project, editor, file, arg);

                FileUtil.commitChanges(editor);
                return result;
            }
        };
    }

    /**
     * Actually performs the QuickFix. Subclasses may override
     *  this if they need special handling
     * @return A QuickFixPrompt instance, if needed to perform
     *  this fix, else null if it's already done. The default
     *  implementation assumes none is needed; only subclasses
     *  will ever return anything (which means if you do need
     *  a prompt, you must NOT return `super.invoke()`)
     */
    protected Object invoke(final IntentionAction action, final Project project,
            final Editor editor, final PsiFile file, final String arg)
            throws QuickFixException {
        action.invoke(project, editor, file);
        return null;
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

        if (ImportsQuickFixDescriptor.handles(descriptor)) {
            return new ImportsQuickFixDescriptor(id,
                    desc,
                    range.getStartOffset(),
                    range.getEndOffset(),
                    descriptor);
        } else {
            return new QuickFixDescriptor(id,
                    desc,
                    range.getStartOffset(),
                    range.getEndOffset(),
                    descriptor);
        }
    }
}
