package io.airlift.stackfold;

import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class StackFolding
{
    private final List<StackElement> segment;
    private final StackElement replacement;

    public StackFolding(List<StackElement> segment, StackElement replacement)
    {
        this.segment = ImmutableList.copyOf(checkNotNull(segment, "segment is null"));
        this.replacement = checkNotNull(replacement, "replacement is null");
    }

    public List<StackElement> foldTrace(List<StackElement> list)
    {
        for (int i = 0; i < list.size() - segment.size() + 1; i++) {
            if (list.subList(i, i + segment.size()).equals(segment)) {
                return ImmutableList.<StackElement>builder()
                        .addAll(list.subList(0, i))
                        .add(replacement)
                        .addAll(list.subList(i + segment.size(), list.size()))
                        .build();
            }
        }
        return list;
    }
}
