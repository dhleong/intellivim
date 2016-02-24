package org.intellivim;

import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class ResultAssert extends AbstractAssert<ResultAssert, Result> {
    protected ResultAssert(Result actual) {
        super(actual, ResultAssert.class);
    }

    public ResultAssert isSuccess() {
        assertThat(actual.isSuccess())
                .overridingErrorMessage("Expected successful, but was not")
                .isTrue();
        return myself;
    }

    public ResultAssert isNotSuccess() {
        assertThat(actual.isSuccess())
                .overridingErrorMessage("Expected not successful, but was")
                .isFalse();
        return myself;
    }

    public ResultAssert hasErrorContaining(String expected) {
        isNotNull();
        isNotSuccess();

        assertThat(asSimpleResult().error)
                .describedAs("Error Message")
                .contains(expected);
        return myself;
    }

    private SimpleResult asSimpleResult() {
        assertThat(actual).isInstanceOf(SimpleResult.class);
        return (SimpleResult) actual;
    }

}
