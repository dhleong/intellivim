package org.intellivim.core.command.complete;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.completion.impl.CompletionSorterImpl;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.Consumer;

/**
 * Created by dhleong on 11/5/14.
 */
class CompletionResultSetImpl extends CompletionResultSet {
    private final int myLengthOfTextBeforePosition;
    private final CompletionParameters myParameters;
    private final CompletionSorter mySorter;
    private final CompletionResultSetImpl myOriginal;
    private final CompletionContributor myContributor;

    public CompletionResultSetImpl(final Consumer<CompletionResult> consumer, final int lengthOfTextBeforePosition,
                                   final PrefixMatcher prefixMatcher,
                                   CompletionContributor contributor,
                                   CompletionParameters parameters,
                                   CompletionSorter sorter,
                                   CompletionResultSetImpl original) {
        super(prefixMatcher, consumer, contributor);
        myContributor = contributor;
        myLengthOfTextBeforePosition = lengthOfTextBeforePosition;
        myParameters = parameters;
        mySorter = sorter;
        myOriginal = original;
    }

    @Override
    public void addElement(final LookupElement element) {
        if (!element.isValid()) {
            System.out.println("Invalid lookup element: " + element);
            return;
        }

        CompletionResult matched = CompletionResult.wrap(element, getPrefixMatcher(), mySorter);
        if (matched != null) {
            passResult(matched);
        }
    }

    @Override
    public CompletionResultSet withPrefixMatcher(final PrefixMatcher matcher) {
        return new CompletionResultSetImpl(getConsumer(), myLengthOfTextBeforePosition, matcher, myContributor, myParameters, mySorter, this);
    }

    @Override
    public void stopHere() {
//        if (LOG.isDebugEnabled()) {
//            LOG.debug("Completion stopped\n" + DebugUtil.currentStackTrace());
//        }
        super.stopHere();
        if (myOriginal != null) {
            myOriginal.stopHere();
        }
    }

    @Override
    public CompletionResultSet withPrefixMatcher(final String prefix) {
        return withPrefixMatcher(new CamelHumpMatcher(prefix));
    }

    @Override
    public CompletionResultSet withRelevanceSorter(CompletionSorter sorter) {
        return new CompletionResultSetImpl(getConsumer(), myLengthOfTextBeforePosition, getPrefixMatcher(),
                myContributor, myParameters, (CompletionSorterImpl)sorter, this);
    }

    @Override
    public void addLookupAdvertisement(String text) {
        getCompletionService().setAdvertisementText(text);
    }

    @Override
    public CompletionResultSet caseInsensitive() {
        return withPrefixMatcher(new CamelHumpMatcher(getPrefixMatcher().getPrefix(), false));
    }

    @Override
    public void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition) {
//        System.out.println("restartCompletionOnPrefixChange:" + prefixCondition);
//        final CompletionProgressIndicator indicator = getCompletionService().getCurrentCompletion();
//        if (indicator != null) {
//            indicator.addWatchedPrefix(myLengthOfTextBeforePosition - getPrefixMatcher().getPrefix().length(), prefixCondition);
//        }
    }

    @Override
    public void restartCompletionWhenNothingMatches() {
//        System.out.println("restartCompletionWhenNothingMatches");
//        final CompletionProgressIndicator indicator = getCompletionService().getCurrentCompletion();
//        if (indicator != null) {
//            indicator.getLookup().setStartCompletionWhenNothingMatches(true);
//        }
    }

    public void runRemainingContributors(CompletionParameters parameters, Consumer<CompletionResult> consumer, final boolean stop) {
        if (stop) {
            stopHere();
        }
        CompleteCommand.getVariantsFromContributors(parameters, myContributor, consumer);
    }

    public static CompletionService getCompletionService() {
        return CompletionService.getCompletionService();
    }
}

