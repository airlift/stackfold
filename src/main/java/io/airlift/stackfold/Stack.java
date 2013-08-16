package io.airlift.stackfold;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Stack
{
    private final String name;
    private final boolean daemon;
    private final String priority;
    private final String threadId;
    private final String nativeId;
    private final String stateMessage;
    private final Thread.State state;
    private final WaitOn waitOn;
    private final List<StackElement> trace;

    public Stack(
            String name,
            boolean daemon,
            String priority,
            String threadId,
            String nativeId,
            String stateMessage,
            Thread.State state,
            WaitOn waitOn,
            List<StackElement> trace)
    {
        this.name = name;
        this.daemon = daemon;
        this.priority = priority;
        this.threadId = threadId;
        this.nativeId = nativeId;
        this.stateMessage = stateMessage;
        this.state = state;
        this.waitOn = waitOn;
        this.trace = ImmutableList.copyOf(checkNotNull(trace, "trace is null"));
    }

    public String getName()
    {
        return name;
    }

    public List<StackElement> getTrace()
    {
        return trace;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, daemon, priority, threadId, nativeId, stateMessage, state, waitOn, trace);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Stack o = (Stack) obj;
        return Objects.equal(this.name, o.name) &&
                Objects.equal(this.daemon, o.daemon) &&
                Objects.equal(this.priority, o.priority) &&
                Objects.equal(this.threadId, o.threadId) &&
                Objects.equal(this.nativeId, o.nativeId) &&
                Objects.equal(this.stateMessage, o.stateMessage) &&
                Objects.equal(this.state, o.state) &&
                Objects.equal(this.waitOn, o.waitOn) &&
                Objects.equal(this.trace, o.trace);
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("daemon", daemon)
                .add("priority", priority)
                .add("threadId", threadId)
                .add("nativeId", nativeId)
                .add("stateMessage", stateMessage)
                .add("state", state)
                .add("waitOn", waitOn)
                .toString();
    }

    public static Function<Stack, List<StackElement>> traceGetter()
    {
        return new Function<Stack, List<StackElement>>()
        {
            @Nullable
            @Override
            public List<StackElement> apply(Stack input)
            {
                return input.getTrace();
            }
        };
    }

    public static Function<Stack, String> namePatternGetter()
    {
        return new Function<Stack, String>()
        {
            @Nullable
            @Override
            public String apply(Stack input)
            {
                return input.getName().replaceAll("\\d+", "*");
            }
        };
    }
}
