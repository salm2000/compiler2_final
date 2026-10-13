# Restricted Python Web Compiler

A Java 17 / ANTLR 4 source-to-source compiler for the restricted Python, Flask-like, Jinja-style HTML, and CSS language defined in [`language-spec.md`](language-spec.md). It emits a runnable Python application that depends only on the Python standard library—never Flask or Jinja2.

## Build and test

Requirements: JDK 17 or newer, Maven 3.9 or newer, and Python 3.10 or newer for generated-code integration tests.

```powershell
mvn clean package
```

The executable shaded JAR is `target/compiler.jar`. Generated ANTLR Java sources live under `target/` and are intentionally not committed.

## Compiler CLI

```powershell
java -jar target/compiler.jar compile examples/products/app.py --templates examples/products/templates --css examples/products/static --out output/products
java -jar target/compiler.jar ast examples/products/app.py --templates examples/products/templates --css examples/products/static
java -jar target/compiler.jar symbols examples/products/app.py --templates examples/products/templates --css examples/products/static
python output/products/app.py 8000
```

Open `http://127.0.0.1:8000`. The reference application lists, adds, displays, and deletes in-memory products. `examples/tasks` is a second-domain generality check using different data and handler names.

Compilation fails before output generation on lexical, syntax, semantic, template, HTML, or CSS errors. Diagnostics use `file:line:column: severity code: message`. The `ast` command recursively prints every node and its children from both ASTs; `symbols` prints names, kinds, scopes, locations, and inferred types.

## Project layout

- `src/main/antlr4`: Python, template/HTML, and CSS grammars.
- `src/main/java/edu/compiler/ast`: two typed AST families and visitor/printer contracts.
- `frontend`, `builder`, `semantic`, `symbols`: compiler analysis phases.
- `generator`: Python, template, and standard-library HTTP emitters.
- `examples`: product integration app, second-domain app, and negative fixtures.
- [`docs/ast-diagram.md`](docs/ast-diagram.md): AST design diagram and cross-language binding overview.

The generated server is intended for local education: it includes bounded form bodies, HTML escaping, local redirects, typed integer routes, traversal-safe CSS serving, and 400/404/405/500 handling, but is not production hosting software.

