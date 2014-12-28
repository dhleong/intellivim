package org.intellivim.java.command.junit;

import com.intellij.rt.execution.junit.segments.PoolOfTestTypes;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.core.command.test.TestNode;

import java.util.Arrays;
import java.util.Map;

/**
 * TODO Refactor this away and just use TestNode. Can remain as utility
 *
 * @author dhleong
 */
public abstract class JunitTestInfo {

    static class JunitTestClassInfo extends JunitTestInfo {
        @Override public String getType() { return PoolOfTestTypes.TEST_CLASS; }
    }

    static class JunitTestPackageInfo extends JunitTestInfo {
        @Override public String getType() { return PoolOfTestTypes.ALL_IN_PACKAGE; }
    }

    static class JunitTestMethodInfo extends JunitTestInfo {

        String parent;

        @Override
        public String getType() {
            return PoolOfTestTypes.TEST_METHOD;
        }

        protected void readFrom(JunitObjectReader in) {
            super.readFrom(in);
            parent = in.readLimitedString();
        }

        @Override
        public String toString() {
            return super.toString() + " ^" + parent;
        }
    }

    static Map<String, Class<? extends JunitTestInfo>> sTypeToInfo =
            ContainerUtil.newHashMap(
                    Arrays.asList(PoolOfTestTypes.TEST_CLASS,
                            PoolOfTestTypes.TEST_METHOD,
                            PoolOfTestTypes.ALL_IN_PACKAGE),
                    Arrays.asList(JunitTestClassInfo.class,
                            JunitTestMethodInfo.class,
                            JunitTestPackageInfo.class)
            );

    String id;
    String name;
    int count;

    protected void readFrom(JunitObjectReader in) {
        name = in.readLimitedString();
    }

    public abstract String getType();

    @Override
    public String toString() {
        return String.format("(%s:%s w/%d) `%s`", id, getType(), count, name);
    }

    public TestNode toTestNode() {
        return new TestNode(id, name);
    }

    public static JunitTestInfo read(JunitObjectReader in) {
        final String id = in.nextReference();
        final String type = in.readLimitedString();
        final Class<? extends JunitTestInfo> typeClass = sTypeToInfo.get(type);

        final JunitTestInfo inflated;
        if (typeClass == null) {
            inflated = new JunitTestInfo() {

                @Override
                public String getType() {
                    return type;
                }
            };
        } else {
            try {
                inflated = typeClass.newInstance();
            } catch (Exception e) {
                // shouldn't happen
                throw new RuntimeException(e);
            }
        }

        inflated.id = id;
        inflated.readFrom(in);
        inflated.count = in.readInt();
        return inflated;
    }
}
