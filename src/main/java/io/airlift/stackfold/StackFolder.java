package io.airlift.stackfold;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.PeekingIterator;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.peekingIterator;
import static java.lang.Long.parseLong;
import static java.lang.String.format;

public final class StackFolder
{
    /**
     * Matches thread info as printed by jstack.
     * <p>
     * Example
     * <pre>
     * "Signal Dispatcher" #2 daemon prio=5 tid=0x00007fda14895800 nid=0x5003 runnable [0x0000000000000000]
     * </pre>
     */
    private static final Pattern JSTACK_THREAD_INFO_PATTERN = Pattern.compile("" +
            "^\"(?<name>.*)\"\\s*(?:#(\\d+))?\\s*(?<daemon>daemon)?\\s*" +
            "prio=(?<priority>\\d+)\\s*(?:os_prio=(\\d+))?\\s*tid=(?<threadId>\\w+)\\s*nid=(?<nativeId>\\w+)\\s*(?<stateMessage>[^\\[]*)(?:\\[([^\\]]*)\\])?$");

    /**
     * Matches thread info as printed by {@link java.lang.management.ThreadMXBean#dumpAllThreads(boolean, boolean)}.
     * Example
     * <pre>
     * "http-worker-560" Id=560 TIMED_WAITING on java.util.concurrent.SynchronousQueue$TransferStack@2a283975
     * </pre>
     */
    private static final Pattern THREAD_MXBEAN_THREAD_INFO_PATTERN = Pattern.compile("" +
            "^\"(?<name>.*)\"\\s+Id=(?<threadId>\\d+)\\s+(?<stateMessage>.*)$");

    private static final Pattern STACK_ELEMENT_PATTERN = Pattern.compile("(?:at)?\\s*([^\\(]+)\\.([^\\(]+)(?:\\(([^:]+)(?::(\\d+))?\\))?");
    private static final Pattern WAIT_ON_PATTERN = Pattern.compile("- parking to wait for  <(\\w+)> \\(a (\\S+)\\)");
    private static final Pattern LOCKED_PATTERN = Pattern.compile("- locked <(\\w+)> \\(a (\\S+)\\)");

    private StackFolder() {}

    public static void renderStacks(Set<Stack> stacks, PrintStream out)
    {
        ListMultimap<List<StackElement>, Stack> threadsGroupedByTrace = Multimaps.index(stacks, Stack.traceGetter());

        for (Collection<Stack> commonStacks : threadsGroupedByTrace.asMap().values()) {
            Stack stack = commonStacks.iterator().next();

            // print thread names
            Map<String, Collection<Stack>> threadsGroupedByNamePattern = new TreeMap<>(Multimaps.index(commonStacks, Stack.namePatternGetter()).asMap());
            if (threadsGroupedByNamePattern.size() < 50) {
                for (Map.Entry<String, Collection<Stack>> entry : threadsGroupedByNamePattern.entrySet()) {
                    if (entry.getValue().size() == 1) {
                        out.println(entry.getValue().iterator().next().getName());
                    }
                    else {
                        out.println(format("%s (%s threads)", entry.getKey(), entry.getValue().size()));
                    }
                }
            }
            else {
                out.println(format("%s threads", commonStacks.size()));
            }

            // print stack
            if (!stack.getTrace().isEmpty()) {
                for (StackElement element : stack.getTrace()) {
                    out.println("    at " + element.prettyPrint());
                }
            }

            out.println();
        }
        out.flush();
    }

    public static Set<Stack> loadStackTrace(List<String> lines, List<StackFolding> stackFoldings)
    {
        ImmutableSet.Builder<Stack> builder = ImmutableSet.builder();

        PeekingIterator<String> lineIterator = peekingIterator(lines.iterator());
        while (lineIterator.hasNext()) {
            String line = lineIterator.next();
            Matcher matcher = JSTACK_THREAD_INFO_PATTERN.matcher(line);
            if (matcher.matches()) {
                Stack stack = extractStack(
                        matcher.group("name"),
                        matcher.group("daemon"),
                        matcher.group("priority"),
                        matcher.group("threadId"),
                        matcher.group("nativeId"),
                        matcher.group("stateMessage"),
                        lineIterator,
                        stackFoldings);
                builder.add(stack);
            }

            matcher = THREAD_MXBEAN_THREAD_INFO_PATTERN.matcher(line);
            if (matcher.matches()) {
                Stack stack = extractStack(
                        matcher.group("name"),
                        null, // daemon
                        "0", // priority
                        matcher.group("threadId"),
                        "x", // nativeId
                        matcher.group("stateMessage"),
                        lineIterator,
                        stackFoldings);
                builder.add(stack);
            }
        }

        return builder.build();
    }

    private static Stack extractStack(
            String name,
            String daemon,
            String priority,
            String threadId,
            String nativeId,
            String stateMessage,
            PeekingIterator<String> lineIterator,
            List<StackFolding> stackFoldings)
    {
        Thread.State state = null;
        WaitOn waitOn = null;
        List<StackElement> trace = ImmutableList.of();
        List<Locked> locks = new ArrayList<>();

        if (lineIterator.hasNext()) {
            String line = lineIterator.peek().trim();
            if (line.startsWith("java.lang.Thread.State:")) {
                String javaThreadState = line.substring("java.lang.Thread.State:".length()).trim();
                try {
                    state = Thread.State.valueOf(javaThreadState);
                }
                catch (Exception ignored) {
                }

                // consume line
                lineIterator.next();
            }

            ImmutableList.Builder<StackElement> builder = ImmutableList.builder();
            while (lineIterator.hasNext()) {
                line = lineIterator.peek().trim();

                // stop when we see a blank line or the start of the next thread or if its a compiler thread
                // compiler thread spits out an extra line that does not fit the pattern in STACK_ELEMENT_PATTERN
                if (line.isEmpty()
                        || JSTACK_THREAD_INFO_PATTERN.matcher(line).matches()
                        || THREAD_MXBEAN_THREAD_INFO_PATTERN.matcher(line).matches()
                        || name.contains("CompilerThread")) {
                    break;
                }

                // process the line
                if (!line.startsWith("-")) {
                    // normal stack element
                    builder.add(extractStackElement(line, locks));
                    locks = new ArrayList<>();
                }
                else {
                    Matcher waitOnMatcher = WAIT_ON_PATTERN.matcher(line);
                    if (waitOnMatcher.matches()) {
                        checkState(waitOn == null, "Thread is waiting on multiple locks");
                        waitOn = new WaitOn(waitOnMatcher.group(1), waitOnMatcher.group(2));
                    }
                    else {
                        Matcher lockedMatcher = LOCKED_PATTERN.matcher(line);
                        if (lockedMatcher.matches()) {
                            locks.add(new Locked(lockedMatcher.group(1), lockedMatcher.group(2)));
                        }
                    }
                }

                // consume line
                lineIterator.next();
            }
            trace = builder.build();
        }

        for (StackFolding stackFolding : stackFoldings) {
            trace = stackFolding.foldTrace(trace);
        }

        return new Stack(name, daemon != null, priority, threadId, nativeId, stateMessage, state, waitOn, trace);
    }

    private static StackElement extractStackElement(String line)
    {
        return extractStackElement(line, ImmutableList.<Locked>of());
    }

    private static StackElement extractStackElement(String line, List<Locked> locks)
    {
        Matcher matcher = STACK_ELEMENT_PATTERN.matcher(line);
        checkState(matcher.matches(), "Expected state element line but got: %s", line);
        return new StackElement(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3),
                (matcher.group(4) == null) ? -1 : parseLong(matcher.group(4)),
                locks);
    }

    public static List<StackFolding> loadFolding(List<String> lines)
    {
        ImmutableList.Builder<StackFolding> builder = ImmutableList.builder();

        Iterator<String> lineIterator = lines.iterator();
        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim();
            if (line.isEmpty()) {
                continue;
            }
            StackElement replacement = extractStackElement(line);

            ImmutableList.Builder<StackElement> segment = ImmutableList.builder();
            while (lineIterator.hasNext()) {
                line = lineIterator.next().trim();

                // stop when we see a blank line or the start of the next thread
                if (line.isEmpty()) {
                    break;
                }

                // process the line
                segment.add(extractStackElement(line));
            }
            builder.add(new StackFolding(segment.build(), replacement));
        }

        return builder.build();
    }
}
