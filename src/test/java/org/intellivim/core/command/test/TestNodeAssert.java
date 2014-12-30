package org.intellivim.core.command.test;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

/**
 * @author dhleong
 */
public class TestNodeAssert
        extends AbstractAssert<TestNodeAssert, TestNode> {
    public TestNodeAssert(final TestNode actual) {
        super(actual, TestNodeAssert.class);
    }

    // 3 - A fluent entry point to your specific assertion class, use it with static import.
    public static TestNodeAssert assertThat(TestNode actual) {
        return new TestNodeAssert(actual);
    }

    public TestNodeAssert hasId(final String expected) {
        isNotNull();

        Assertions.assertThat(actual.id).as("ID")
                .isNotEmpty()
                .isEqualTo(expected);
        return this;
    }

    public TestNodeAssert hasName(final String expected) {
        isNotNull();

        Assertions.assertThat(actual.name).as("name")
                .isNotEmpty()
                .isEqualTo(expected);
        return this;
    }

    public TestNodeAssert hasKidsCount(final int expected) {
        isNotNull();

        Assertions.assertThat(actual.kids).as("kids count")
                .isNotEmpty()
                .hasSize(expected);
        return this;
    }

    public TestNodeAssert hasKidWithId(final String expected) {
        isNotNull();

        Assertions.assertThat(actual.kids)
                .as("Kids IDs")
                .extracting("id", String.class)
                .contains(expected);
        return this;
    }

    public TestNodeAssert hasState(final TestState expected) {
        isNotNull();

        Assertions.assertThat(actual.state)
                .as("state")
                .isEqualTo(expected);
        return this;
    }
}
