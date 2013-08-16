package io.airlift.stackfold;

import com.google.common.io.CharStreams;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import static io.airlift.stackfold.StackFolder.loadFolding;
import static io.airlift.stackfold.StackFolder.loadStackTrace;
import static io.airlift.stackfold.StackFolder.renderStacks;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class StackFold
{
    private StackFold() {}

    public static void main(String[] args)
            throws Exception
    {
        List<StackFolding> foldings = loadFolding(readResourceLines("folding.txt"));

        Set<Stack> stacks = loadStackTrace(readStacksFromStdin(), foldings);

        renderStacks(stacks, System.out);
    }

    private static List<String> readResourceLines(String name)
            throws IOException
    {
        return Resources.readLines(Resources.getResource(name), UTF_8);
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    private static List<String> readStacksFromStdin()
            throws IOException
    {
        // use default character set to match jstack output
        return CharStreams.readLines(new InputStreamReader(System.in));
    }
}
