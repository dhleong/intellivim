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
    private final int startOffset;
    private final int endOffset;
    private final HighlightSeverity severity;
    private final String description;

    /** it is too slow if we include all these */
    private transient final List<QuickFixDescriptor> fixes;

    private Problem(int id, int line, int col,
            int startOffset, int endOffset,
            HighlightSeverity severity,
            String description,
            List<QuickFixDescriptor> fixes) {
        this.id = id;
        this.line = line;
        this.col = col;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.severity = severity;
        this.description = description;
        this.fixes = fixes;
    }

    public boolean containsOffset(int offset) {
        return offset >= startOffset && offset < endOffset;
    }

    public String getDescription() {
        return description;
    }

    public List<QuickFixDescriptor> getFixes() {
        return fixes;
    }

    public boolean isError() {
        return severity == HighlightSeverity.ERROR;
    }

    @Override
    public String toString() {
        return String.format("[%d@%d:%d][%s]%s",
                id, line, col,
                severity,
                description );
    }

    public static Problem from(int id, Document doc, HighlightInfo info) {
        if (info.getDescription() == null)
            return null;

        final String description = info.getDescription();
        final int line = doc.getLineNumber(info.getStartOffset());
        final int col = info.getStartOffset() - doc.getLineStartOffset(line);

        List<QuickFixDescriptor> quickFixes = new ArrayList<QuickFixDescriptor>();
        int quickFixNumber = 0;
        if (info.quickFixActionRanges != null) {
            for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> pair
                    : info.quickFixActionRanges) {

                final String quickFixId = "" + id + FIX_ID_SEPARATOR + quickFixNumber++;
                final HighlightInfo.IntentionActionDescriptor desc = pair.getFirst();
                final TextRange range = pair.getSecond();
                quickFixes.add(QuickFixDescriptor.from(description, quickFixId, desc, range));
            }
        }

        // the lines returned are 0-indexed, and we want 1-indexed
        // the offsets also start at 0, so our cols will be 0-indexed also
        return new Problem(id,
                line + 1, col + 1,
                info.getActualStartOffset(), info.getActualEndOffset(),
                info.getSeverity(),
                description,
                quickFixes);
    }

    public QuickFixDescriptor locateQuickFix(String fixId) {
        final String id = fixId.substring(fixId.indexOf(FIX_ID_SEPARATOR) + 1);
        final int index = Integer.parseInt(id);
        return fixes.get(index);
    }
}
