package edu.compiler.integration;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TerminalToolTest {
    @Test void listsRunnableExamplesFromScriptableCli() throws Exception {
        Process process=new ProcessBuilder("python","compiler_tool.py","list").redirectErrorStream(true).start();
        assertTrue(process.waitFor(20,TimeUnit.SECONDS));
        String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        assertEquals(0,process.exitValue(),output);assertTrue(output.contains("products"),output);assertTrue(output.contains("tasks"),output);
    }
}
