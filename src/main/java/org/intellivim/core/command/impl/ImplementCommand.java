package org.intellivim.core.command.impl;

import com.intellij.openapi.project.Project;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Result;
import org.intellivim.SimpleResult;

/**
 * @author dhleong
 */
@Command("implement")
public class ImplementCommand extends ProjectCommand {

    private final String file;
    private final String[] signatures;
    private final int offset;

    public ImplementCommand(Project project, String file, String signature, int offset) {
        this(project, file, offset, signature);
    }

    public ImplementCommand(Project project, String file, int offset, String... signatures) {
        super(project);

        this.file = file;
        this.signatures = signatures;
        this.offset = offset;
    }

    @Override
    public Result execute() {

        final Implementables all = Implementables.collectFrom(project, file, offset);
        final Implementables chosen;
        try {
            chosen = all.select(signatures);
        } catch (IllegalArgumentException e) {
            return SimpleResult.error(e);
        }

        // make it so!
        chosen.implementAll();
        return SimpleResult.success();
    }
}
