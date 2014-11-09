package org.intellivim.core.command.problems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;

import java.util.List;

/**
 * Created by dhleong on 11/8/14.
 */
public class Problem {

    private final int line;
    private final int col;
    private final HighlightSeverity severity;
    private final String description;
    private final List<Pair<HighlightInfo.IntentionActionDescriptor, TextRange>> quickFixActionRanges;

    Problem(int line, int col, HighlightSeverity severity, String description, List<Pair<HighlightInfo.IntentionActionDescriptor, TextRange>> quickFixActionRanges) {
        this.line = line;
        this.col = col;
        this.severity = severity;
        this.description = description;
        this.quickFixActionRanges = quickFixActionRanges;
    }

    public static Problem from(Document doc, HighlightInfo info) {
        if (info.getDescription() == null)
            return null;

        int line = doc.getLineNumber(info.getStartOffset());
        int col = info.getStartOffset() - doc.getLineStartOffset(line);
        System.out.println("Avail: " + info.getStartOffset()
                + " -- " + info.getEndOffset()
                + " (line: " + line + ")"
//                    + "; " + info.getText()
                + "; " + info.getDescription()
                + "; " + info.getToolTip()
                + "\n;quickr=" + info.quickFixActionRanges
                + "\n;params= " + info.paramString()
                + "\n; " + info.getSeverity());
        return new Problem(line, col, info.getSeverity(),
                info.getDescription(),
                info.quickFixActionRanges);
    }
}
