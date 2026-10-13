# Compiler project report

## Overview

The project implements the v1 language in `language-spec.md` as a Java source-to-source compiler. It accepts one restricted Python application, a template directory, and a CSS directory. Its output is a Python HTTP application that uses only `http.server`, `urllib.parse`, `html`, and other Python standard-library modules.

The implementation is deliberately domain-independent. Routes are derived from decorators, functions are emitted from their statements, and template context comes from named `render_template` arguments. The product application and the unrelated task application compile through exactly the same grammar, semantic passes, and generator.

## Compiler phases

1. **Lexing and parsing.** ANTLR 4 grammars independently parse restricted Python, mixed Jinja/HTML, and CSS. The Python lexer base emits `INDENT`/`DEDENT`. Error listeners turn ANTLR errors into located `LEX001` and `SYN001` diagnostics.
2. **AST construction.** Java visitors convert parse trees into independent Python and template AST object families. `AstNode` supplies source location, child traversal, and polymorphic visitor dispatch. CSS remains an attached validated asset rather than becoming a third primary AST.
3. **Symbol collection.** Module functions and variables are collected before body checks, enabling recursion and forward function references. Function parameters/locals and template bindings/loop variables occupy named scopes.
4. **Semantic analysis.** The analyzer checks undefined names, call arity, strict intrinsic/method support, route uniqueness and parameter agreement, template existence/bindings/iterability, supported HTML, and statically provable operand mistakes. The CSS validator enforces the property allowlist.
5. **Generation.** AST visitors emit ordinary Python functions and template render functions. A generated registry transfers live context values at request time. The web emitter adds route matching, form decoding, redirects, status handling, and safe static CSS service.

## AST and inspection

The detailed class diagram is in [`ast-diagram.md`](ast-diagram.md). `java -jar target/compiler.jar ast ...` recursively prints every node, relevant values, source location, and children. The `symbols` command prints symbol name, kind, scope, location, and bounded inferred type.

## Runtime and security policy

- Interpolation calls `html.escape(..., quote=True)`; `None` becomes an empty string.
- Attribute interpolation uses a non-evaluating dotted-name resolver and the same escaping policy.
- No generated path uses `eval` or `exec`.
- POST bodies are limited to 1 MiB and parsed as URL-encoded forms.
- Redirects must be local absolute paths and cannot begin with `//`.
- Static serving is restricted to resolved `.css` files below the generated static root.
- Typed path parameters accept decimal integers. Unknown routes return 404 and known paths with unsupported methods return 405.
- Unexpected handler failures expose only a 500 status, not a traceback in the response.

This remains an educational local server: state is process-local memory, and production concerns such as authentication, persistent storage, rate limiting, TLS, and multi-user isolation are outside v1.

## Diagnostics demonstrated

The test suite and `examples/negative-tests` demonstrate `SEM001` through `SEM007`, plus route mismatch `SEM011`, invalid static operands `SEM012`, HTML validation `SEM013`, and CSS validation `SEM014`. Diagnostics are reported as `file:line:column: severity code: message`. Dynamic dictionary shape checks are deferred to Python when the compiler cannot prove a shape safely.

## Verification

The Maven suite covers parsing, both AST builders, tree printing, symbol output, positive and negative semantics, generated Python syntax, escaping, and two application domains. Its HTTP integration test compiles and starts the product application, then verifies listing, details, add/303 redirect, delete/303 redirect, static CSS, escaping, 404, and 405 responses. A generated-code acceptance test covers arithmetic precedence, forward calls, recursion, conditionals, `while`, `break`, `continue`, indexing, and list mutation without Flask or Jinja2 installed.

The only submission metadata not inferable from source is intentionally marked `TODO` in `submission-info.txt`: the team must add its repository URL and member names before packaging the assigned group archive.
