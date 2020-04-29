package io.airlift.stackfold;

import com.google.common.io.Resources;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;

import static io.airlift.stackfold.StackFolder.loadFolding;
import static io.airlift.stackfold.StackFolder.loadStackTrace;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class TestStackFolder
{
    @Test
    public void testFolding()
            throws Exception
    {
        List<StackFolding> foldings = loadFolding(readResourceLines("folding.txt"));

        Set<Stack> stacks = loadStackTrace(readResourceLines("stack.txt"), foldings);

        assertEquals(renderStacks(stacks), "" +
                "Attach Listener\n" +
                "C* CompilerThread* (2 threads)\n" +
                "GC task thread#* (ParallelGC) (8 threads)\n" +
                "Service Thread\n" +
                "Signal Dispatcher\n" +
                "VM Periodic Task Thread\n" +
                "VM Thread\n" +
                "\n" +
                "Finalizer\n" +
                "    at java.lang.Object.wait(Native Method)\n" +
                "    at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:135)\n" +
                "    at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:151)\n" +
                "    at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:189)\n" +
                "\n" +
                "Reference Handler\n" +
                "    at java.lang.Object.wait(Native Method)\n" +
                "    at java.lang.Object.wait(Object.java:503)\n" +
                "    at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:133)\n" +
                "\n" +
                "RMI TCP Connection(idle)\n" +
                "    at java.util.concurrent.ThreadPoolExecutor$Worker.run\n" +
                "\n" +
                "main\n" +
                "    at java.io.FileInputStream.readBytes(Native Method)\n" +
                "    at java.io.FileInputStream.read(FileInputStream.java:242)\n" +
                "    at java.io.BufferedInputStream.read1(BufferedInputStream.java:273)\n" +
                "    at java.io.BufferedInputStream.read(BufferedInputStream.java:334)\n" +
                "    at sun.nio.cs.StreamDecoder.readBytes(StreamDecoder.java:283)\n" +
                "    at sun.nio.cs.StreamDecoder.implRead(StreamDecoder.java:325)\n" +
                "    at sun.nio.cs.StreamDecoder.read(StreamDecoder.java:177)\n" +
                "    at java.io.InputStreamReader.read(InputStreamReader.java:184)\n" +
                "    at com.google.common.io.LineReader.readLine(LineReader.java:76)\n" +
                "    at com.google.common.io.CharStreams.readLines(CharStreams.java:345)\n" +
                "    at io.airlift.stackfold.StackFold.readStacksFromStdin(StackFold.java:41)\n" +
                "    at io.airlift.stackfold.StackFold.main(StackFold.java:25)\n" +
                "\n");
    }

    @Test
    public void testThreadMxBeanDump()
            throws Exception
    {
        List<StackFolding> foldings = loadFolding(readResourceLines("folding.txt"));
        Set<Stack> stacks = loadStackTrace(readResourceLines("thread_mxbean_stack.txt"), foldings);
        assertEquals(renderStacks(stacks), String.join("\n", readResourceLines("thread_mxbean_stack_folded.txt")));
    }

    private static String renderStacks(Set<Stack> stacks)
    {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (PrintStream stream = new PrintStream(out, false, UTF_8.name())) {
                StackFolder.renderStacks(stacks, stream);
            }
            return out.toString(UTF_8.name());
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    private static List<String> readResourceLines(String name)
            throws IOException
    {
        return Resources.readLines(Resources.getResource(name), UTF_8);
    }
}
