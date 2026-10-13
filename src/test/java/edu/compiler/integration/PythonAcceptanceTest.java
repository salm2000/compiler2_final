package edu.compiler.integration;

import static org.junit.jupiter.api.Assertions.*;
import edu.compiler.CompilerService;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PythonAcceptanceTest {
    @TempDir Path temp;
    @Test void preservesPrecedenceForwardCallsRecursionControlFlowAndMutation()throws Exception{
        Path source=temp.resolve("app.py");Path templates=Files.createDirectory(temp.resolve("templates"));Path css=Files.createDirectory(temp.resolve("static"));
        Files.writeString(source,"""
            app = Application()
            values = [1, 2]
            def arithmetic():
                return 2 + 3 * 4
            def forward(value):
                return twice(value) + 1
            def twice(value):
                return value * 2
            def factorial(value):
                if value <= 1:
                    return 1
                return value * factorial(value - 1)
            def controlled(limit):
                total = 0
                index = 0
                while index < limit:
                    index += 1
                    if index == 2:
                        continue
                    if index > 3:
                        break
                    total += index
                return total
            def mutate():
                values.append({"n": 3})
                return len(values)
            """,StandardCharsets.UTF_8);
        Path output=temp.resolve("out");var result=new CompilerService().compile(source,templates,css,output);assertTrue(result.successful(),()->result.diagnostics().toString());
        String app=output.resolve("app.py").toString().replace("\\","\\\\");
        String command="import runpy; d=runpy.run_path(r'"+app+"'); print(d['arithmetic'](), d['forward'](5), d['factorial'](5), d['controlled'](8), d['mutate']())";
        Process process=new ProcessBuilder("python","-c",command).redirectErrorStream(true).start();assertTrue(process.waitFor(20,TimeUnit.SECONDS));
        String text=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();assertEquals(0,process.exitValue(),text);assertEquals("14 11 120 4 3",text);
    }
}
