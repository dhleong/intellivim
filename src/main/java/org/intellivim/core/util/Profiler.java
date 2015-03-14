package org.intellivim.core.util;

import com.intellij.openapi.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for profiling method calls
 *
 * @author dhleong
 */
public class Profiler {

    private static final boolean ENABLED = false;

    private static final Profiler DUMMY = new Profiler(null) {
        @Override
        public void mark(final String label) {
            // nop
        }

        @Override
        public void finish(final String label) {
            // nop
        }

        @Override
        public void switchContext(final Object newContext) {
            // nop
        }
    };

    private static Map<Object, Profiler> sActiveProfilers =
            new HashMap<Object, Profiler>();

    private final long start;
    private final List<Pair<String, Long>> intervals = new ArrayList<Pair<String, Long>>();

    private long last;
    private Object context;

    private Profiler(Object context) {
        start = System.nanoTime();
        last = start;

        this.context = context;
    }

    public void mark(final String label) {
        if (label == null) {
            throw new IllegalArgumentException("Label must not be null");
        } else if (!ENABLED) {
            // don't bother
            return;
        }

        final long now = System.nanoTime();
        final long delta = now - last;
        last = now;

        intervals.add(new Pair<String, Long>(label, delta));
    }

    public void finish(final String label) {
        sActiveProfilers.remove(context);

        if (!ENABLED) {
            return;
        }

        mark(label);
        final long total = System.nanoTime() - start;
        for (final Pair<String, Long> interval : intervals) {
            System.out.println(format(interval.getSecond(), interval.getFirst()));
        }
        System.out.println(format(total, null));
    }

    public void switchContext(final Object newContext) {
        sActiveProfilers.remove(context);
        sActiveProfilers.put(newContext, this);
        context = newContext;
    }

    private static final String format(final long duration, final String label) {
        final long millis = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
        if (label == null) {
            return String.format("[%6d] TOTAL", millis);
        } else {
            return String.format("[+%5d] %s", millis, label);
        }
    }

    public static Profiler start(final Object context) {
        final Profiler newProfiler = new Profiler(context);
        sActiveProfilers.put(context, newProfiler);
        return newProfiler;
    }

    /**
     * Retrieve an existing Profiler from its context. This
     *  will never return null, but if profiling is disabled
     *  or if you never actually started any profiling, this
     *  will just return a dummy.
     *
     * For a production class I would be skeptical of this decision,
     *  but this class is only really for debugging, has a
     *  central point of fail, and was designed to "stay out
     *  of the way," so I think this works
     *
     * @param context The context passed to #start(), or
     *                subsequently set with #switchContext()
     */
    public static Profiler with(final Object context) {
        final Profiler existing = sActiveProfilers.get(context);
        if (existing != null) {
            return existing;
        } else if (ENABLED && !sActiveProfilers.isEmpty()) {
            throw new IllegalArgumentException("No profiler for context " + context);
        }

        // either we're disabled or there are no active profilers at all.
        //  just give them a dummy so they don't crash
        return DUMMY;
    }
}
