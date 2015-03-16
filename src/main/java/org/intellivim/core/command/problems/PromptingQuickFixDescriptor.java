package org.intellivim.core.command.problems;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;

import java.util.List;

/**
 * Base type for special QuickFixDescriptors
 *  that provide some sort of choice to the user.
 *
 * This is more to ensure a consistent wire
 *  interface; there's not much code shared
 *
 * @author dhleong
 */
public abstract class PromptingQuickFixDescriptor
        extends QuickFixDescriptor {

    final List<String> choices;

    PromptingQuickFixDescriptor(final String problemDescription, final String id, final String description,
            final int start, final int end,
            final HighlightInfo.IntentionActionDescriptor descriptor,
            final List<String> choices) {
        super(problemDescription, id, description, start, end, descriptor);

        this.choices = choices;
    }
}
