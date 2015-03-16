package org.intellivim.core.command.problems;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;

import java.util.Iterator;
import java.util.List;

/**
 * @author dhleong
 */
@Command("quickfix")
public class FixProblemCommand extends ProjectCommand {

    private static String sPendingFixesContext;
    private static List<? extends QuickFixDescriptor> sPendingFixes;

    @Required @Inject PsiFile file;
    @Required String fixId;

    /* optional */String arg;

    public FixProblemCommand(Project project, String filePath, String fixId) {
        this(project, filePath, fixId, null);
    }
    public FixProblemCommand(Project project, String filePath, String fixId, String arg) {
        super(project);
        file = ProjectUtil.getPsiFile(project, filePath);
        this.fixId = fixId;
        this.arg = arg;
    }

    @Override
    public Result execute() {

        final QuickFixDescriptor fix = selectFix();
        final VimEditor editor = new VimEditor(project, file, 0);

        try {
            return SimpleResult.success(
                    fix.execute(project, editor, file, arg)
            );
        } catch (QuickFixException e) {
            return SimpleResult.error(e);
        }
    }

    private QuickFixDescriptor selectFix() {

        if (sPendingFixes != null && file.getVirtualFile().getPath().equals(sPendingFixesContext)) {
            final Iterator<? extends QuickFixDescriptor> iter = sPendingFixes.iterator();
            while (iter.hasNext()) {
                final QuickFixDescriptor descriptor = iter.next();
                if (descriptor.id.equals(fixId)) {
                    iter.remove();

                    if (sPendingFixes.isEmpty()) {
                        // no more fixes
                        sPendingFixes = null;
                        sPendingFixesContext = null;
                    }
                    return descriptor;
                }
            }
        }

        // no? possibly they canceled half-way; just try from scratch
        sPendingFixes = null;
        sPendingFixesContext = null;
        final Problems problems = Problems.collectFrom(project, file);
        return problems.locateQuickFix(fixId);
    }

    public static void clearPendingFixes() {
        sPendingFixes = null;
        sPendingFixesContext = null;
    }

    /**
     * When doing batch import resolution, such as in the OptimizeImportsCommand,
     *  we may encounter multiple ambiguous imports. In such situations, the
     *  QuickFixDescriptors are provided to the client to be resolved serially.
     *  However, because they are resolved separately, the ids generated for the
     *  descriptors are not consistent. We could perhaps attempt to derive
     *  stable ids for each fix, but simply saving them here is a lot easier.
     *
     * This acts as a cache; the next FixProblemCommand issued for a different
     *  file will clear that cache. The cache may also be manually cleared
     *  using #clearPendingFixes()
     */
    public static void setPendingFixes(final PsiFile context,
            final List<? extends QuickFixDescriptor> fixes) {
        sPendingFixesContext = context.getVirtualFile().getPath();
        sPendingFixes = fixes;
    }
}
