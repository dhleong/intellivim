package org.intellivim.core.command.problems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dhleong on 11/8/14.
 */
public class Problem {

    public static final char FIX_ID_SEPARATOR = '.';

    private final int id;
    private final int line;
    private final int col;
    private final HighlightSeverity severity;
    private final String description;

    /** it is too slow if we include all these */
    private transient final List<QuickFixDescriptor> fixes;

    private Problem(int id, int line, int col, HighlightSeverity severity,
            String description,
            List<QuickFixDescriptor> fixes) {
        this.id = id;
        this.line = line;
        this.col = col;
        this.severity = severity;
        this.description = description;
        this.fixes = fixes;
    }

    public List<QuickFixDescriptor> getFixes() {
        return fixes;
    }

    public static Problem from(int id, Document doc, HighlightInfo info) {
        if (info.getDescription() == null)
            return null;

        int line = doc.getLineNumber(info.getStartOffset());
        int col = info.getStartOffset() - doc.getLineStartOffset(line);

        List<QuickFixDescriptor> quickFixes = new ArrayList<QuickFixDescriptor>();
        int quickFixNumber = 0;
        if (info.quickFixActionRanges != null) {
            for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> pair
                    : info.quickFixActionRanges) {

                final String quickFixId = "" + id + FIX_ID_SEPARATOR + quickFixNumber;
                final HighlightInfo.IntentionActionDescriptor desc = pair.getFirst();
                final TextRange range = pair.getSecond();
                quickFixes.add(QuickFixDescriptor.from(quickFixId, desc, range));
            }
        }

        // the lines returned are 0-indexed, and we want 1-indexed
        return new Problem(id,
                line + 1, col,
                info.getSeverity(),
                info.getDescription(),
                quickFixes);
    }

    public QuickFixDescriptor locateQuickFix(String fixId) {
        final String id = fixId.substring(fixId.indexOf(FIX_ID_SEPARATOR) + 1);
        final int index = Integer.parseInt(id);
        return fixes.get(index);
    }
}
