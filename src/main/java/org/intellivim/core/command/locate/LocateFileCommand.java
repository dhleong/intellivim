package org.intellivim.core.command.locate;

import com.intellij.ide.util.gotoByName.ChooseByNameBase;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.DefaultChooseByNameItemProvider;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.CommonProcessors;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhleong
 */
@Command("locate")
public class LocateFileCommand extends ProjectCommand {

    public enum LocateType {
        FILE,
        CLASS
    }

    @Required LocateType type;
    @Required String pattern;

    /* optional */String file;

    public LocateFileCommand(Project project, LocateType type, String fileContext, String pattern) {
        super(project);

        this.type = type;
        this.pattern = pattern;

        file = fileContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result execute() {

        // be unchecked for testing convenience
        final List results = new ArrayList<LocatedFile>();

        final PsiFile context = file == null
            ? null
            : ProjectUtil.getPsiFile(project, file);

        final ChooseByNameModel model = pickModel();
        if (model == null) {
            return SimpleResult.error("Invalid locate file type");
        }

        final ChooseByNameBase chooser = new NullChooseByNameBase(project, model, pattern, context);
        new DefaultChooseByNameItemProvider(context).filterElements(
                chooser, pattern,
                /* everywhere */false,
                new ProgressIndicatorBase(),
                new CommonProcessors.CollectProcessor<Object>(results) {

            @Override
            public boolean process(Object o) {
                return super.process(LocatedFile.from(model, o));
            }
        });

        return SimpleResult.success(results);
    }

    private ChooseByNameModel pickModel() {
        if (type == null)
            return null;

        switch (type) {
        case FILE:
            return new GotoFileModel(project);
        case CLASS:
            return new GotoClassModel2(project);
        }

        return null;
    }

    /** UI-less implementation */
    private static class NullChooseByNameBase extends ChooseByNameBase {
        public NullChooseByNameBase(Project project, ChooseByNameModel model,
                String pattern, PsiFile context) {
            super(project, model, pattern, context);
        }

        @Override
        protected boolean isCheckboxVisible() {
            return false;
        }

        @Override
        protected boolean isShowListForEmptyPattern() {
            return false;
        }

        @Override
        protected boolean isCloseByFocusLost() {
            return false;
        }

        @Override
        protected void showList() {

        }

        @Override
        protected void hideList() {

        }

        @Override
        protected void close(boolean isOk) {

        }
    }
}
