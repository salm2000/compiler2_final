package edu.compiler.integration;

import static org.junit.jupiter.api.Assertions.*;
import edu.compiler.CompilerService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecondApplicationTest {
    @TempDir Path temp;
    @Test void compilesDifferentDomainWithoutGeneratorSpecialCases()throws Exception{
        Path example=Path.of("examples","tasks");
        var result=new CompilerService().compile(example.resolve("app.py"),example.resolve("templates"),example.resolve("static"),temp.resolve("tasks"));
        assertTrue(result.successful(),()->result.diagnostics().toString());
    }
}
