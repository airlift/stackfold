package io.airlift.stackfold;

import static com.google.common.base.MoreObjects.toStringHelper;

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
        return toStringHelper(this)
                .add("lockId", lockId)
                .add("type", type)
                .toString();
    }
}
