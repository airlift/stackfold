package io.airlift.stackfold;

import com.google.common.base.Objects;

public class WaitOn
{
    private final String lockId;
    private final String type;

    public WaitOn(String lockId, String type)
    {
        this.lockId = lockId;
        this.type = type;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("lockId", lockId)
                .add("type", type)
                .toString();
    }
}
