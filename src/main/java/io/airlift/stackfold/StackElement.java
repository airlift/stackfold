package io.airlift.stackfold;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class StackElement
{
    private final String className;
    private final String method;
    private final String file;
    private final long lineNumber;
    private final List<Locked> locks;

    public StackElement(String className, String method, String file, long lineNumber, List<Locked> locks)
    {
        this.className = className;
        this.method = method;
        this.file = file;
        this.lineNumber = lineNumber;
        this.locks = ImmutableList.copyOf(checkNotNull(locks, "locks is null"));
    }

    public String prettyPrint()
    {
        StringBuilder out = new StringBuilder();
        out.append(className).append('.').append(method);
        if (file != null) {
            out.append('(').append(file);
            if (lineNumber >= 0) {
                out.append(':').append(lineNumber);
            }
            out.append(')');
        }
        return out.toString();
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(className, method);
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
        StackElement o = (StackElement) obj;
        return Objects.equal(this.className, o.className) &&
                Objects.equal(this.method, o.method);
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("className", className)
                .add("method", method)
                .add("file", file)
                .add("lineNumber", lineNumber)
                .add("locks", locks)
                .toString();
    }
}
