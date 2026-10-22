# Repository Guidelines

## Project Structure & Module Organization

Production code lives in `src/main/java/edu/compiler`. Compiler phases are separated into `frontend`, `builder`, `semantic`, `symbols`, and `generator`; shared AST types are under `ast`, while CLI and orchestration code sit at the package root. ANTLR lexer and parser grammars live in `src/main/antlr4/edu/compiler/antlr`. Keep Java tests in the matching package below `src/test/java/edu/compiler`.

Runnable language samples belong in `examples/<name>/`, with `app.py`, `templates/`, and `static/` together. Negative semantic fixtures are in `examples/negative-tests`. Design notes and generated-report documentation live in `docs/`. Treat `target/`, `output/`, and `reports/` as generated artifacts; do not commit them.

## Build, Test, and Development Commands

- `mvn clean package` regenerates ANTLR sources, runs tests, and builds `target/compiler.jar`.
- `mvn test` runs the complete JUnit suite, including integration and acceptance tests.
- `python compiler_tool.py test` and `python compiler_tool.py build --clean` provide cross-platform wrappers.
- `python compiler_tool.py run products --port 8000` compiles and launches the reference application.
- `java -jar target/compiler.jar ast examples/products/app.py --templates examples/products/templates --css examples/products/static` prints the parsed AST for focused debugging.

Use JDK 17+, Maven 3.9+, and Python 3.10+.

## Coding Style & Naming Conventions

Use four spaces in Java and Python, UTF-8 text, and no tab indentation. Java classes and records use `PascalCase`; methods, variables, and packages use `camelCase` or lowercase package segments. Name grammar files by language and role, such as `CssLexer.g4`. Keep compiler phases narrowly scoped and preserve diagnostic locations and stable error codes when changing validation.

No formatter or linter is configured, so follow nearby code and keep imports explicit and organized. Never edit generated ANTLR sources under `target/`; change the `.g4` grammar instead.

## Testing Guidelines

Tests use JUnit 5. Name classes `*Test` and test methods after observable behavior, for example `parsesNestedTemplate`. Add focused unit tests beside the affected phase and integration tests for cross-language behavior or generated Python execution. Run `mvn test` before submitting; grammar, semantic, and generator changes should include both successful and failing cases where applicable.

## Commit & Pull Request Guidelines

Recent history uses Conventional Commits with optional scopes: `feat(reports): ...`, `test(acceptance): ...`, and `docs: ...`. Keep each commit focused and write the subject in imperative, lowercase style.

Pull requests should explain the behavior change, identify affected compiler phases, and list validation commands. Link relevant issues. Include sample diagnostics or report screenshots when CLI output or generated HTML changes, and do not include generated build/output directories.
