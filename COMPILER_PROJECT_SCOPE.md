# Compiler Project — Scope & Implementation Plan

> **Status:** Proposed implementation baseline (v1.0)  
> **Academic reference:** Compiler Project Announcement, 2025/2026  
> **Compiler implementation language:** **Java only**  
> **Parsing technology:** ANTLR 4, Java target  
> **Input:** Restricted Python + Flask-like web syntax, Jinja2-style templates, HTML, CSS  
> **Output:** Runnable Python web application using the Python standard library; **no Flask runtime dependency**

## 1. Purpose

Build a **reusable source-to-source compiler**, not a product-management script generator. The compiler reads a web application written in a deliberately limited but **general-purpose** subset of Python, plus Jinja2-like HTML templates and CSS. It lexes and parses the inputs with ANTLR, constructs **two independent ASTs (Python and Jinja2/template)**, performs semantic analysis, transfers application data and template bindings, and generates a Python application that a Python interpreter can run as an HTTP server.

The product-management website in the announcement is our **reference integration test**. The grammar, AST, and generator must **not** have production-specific rules such as `delete_product`, `find_product`, or a fixed `products` variable name. An arbitrary user-defined function name should work when its body uses supported syntax.

**Success is not equivalent to parsing Python.** The project is complete only when generated components work together, the required web pages behave correctly, and the ASTs, diagnostics, and symbol table can be inspected.

## 2. Requirements and origin

### 2.1 Mandatory requirements from the announcement

1. Lexers/parsers and custom syntax covering Python, Jinja2, HTML, and CSS.
2. Two ASTs: **Python AST** and **Jinja2 AST**; generator must pass data from a Python data array/list into Jinja2 processing.
3. OOP AST nodes using inheritance and polymorphism; preserve node identity/type and source line information.
4. Semantic checking in both parts, demonstrating **at least five semantic errors** in total.
5. Code generation whose generated parts operate **together**.
6. Integrated pages for **product listing, adding a product, product details, deleting a product**, with smooth navigation.
7. Diagnostic printing of **every AST node and its children**, the **complete AST**, and the **symbol table**.
8. Submission bundle: source code; report documenting project components and an AST diagram; text file with GitHub repository URL and group member names. The announcement also specifies one compressed archive named with the assigned group number.

### 2.2 Additional project decisions / clarifications

These are **our agreed design decisions**, not claims that the announcement spells out the implementation choices:

- Entire **compiler implementation is Java**; ANTLR generates Java lexer/parser classes.
- Input resembles Python with Flask-like routing/template idioms but **does not require importing or using Flask**.
- Generated application runs under **Python**, serving HTTP with standard-library facilities such as `http.server`; it does **not** import Flask.
- Python, Jinja2, HTML, and CSS are **subsets**, not full implementations.
- The compiler handles **general Python functions** and expressions in its subset; application-level behaviors are expressed through those constructs.
- Jinja2-like template syntax is compiled to Python rendering logic, not executed through the Jinja2 library.

**Out-of-scope interpretation to avoid:** The announcement's “printing” item is compiler diagnostics (AST and symbol table printing), **not** a mandatory browser `window.print()` feature.

## 3. End-to-end user workflow

```text
source/
  app.py                  # restricted Python + web constructs
  templates/
    index.html            # Jinja2 + HTML
    details.html
    add.html
  static/
    style.css             # supported CSS subset
          |
          v
     Java compiler CLI
          |
    ANTLR lexers/parsers
          |
      Parse trees
          |
  Python AST + Template AST
          |
  Symbol collection + semantic checks
          |
  Template binding and code generation
          |
output/
  app.py                  # generated runnable Python program
  ...                     # optional runtime/static modules
          |
    python output/app.py
          |
  http://localhost:8000
```

Representative CLI (proposed):

```bash
java -jar compiler.jar compile source/app.py --templates source/templates --css source/static --out output
java -jar compiler.jar ast source/app.py --templates source/templates
java -jar compiler.jar symbols source/app.py --templates source/templates
python output/app.py
```

The CLI names are proposed interfaces; they are not commitments imposed by the announcement.

## 4. Scope and supported language features

### 4.1 Python subset — **general syntax**

| Category | In scope for v1 | Out of scope for v1 |
|---|---|---|
| Lexical structure | Identifiers, whitespace/indentation (`INDENT`/`DEDENT`), comments, newlines, numeric/string literals | Every Python lexical edge case |
| Data | `int`, `float`, `str`, `bool`, `None`; lists and dictionaries | Sets, tuples as general values, complex numbers |
| Variables | Simple assignment, reassignment, augmented assignment | Type annotations, destructuring assignments |
| Expressions | Arithmetic `+ - * / // %`, comparisons, `and/or/not`, parentheses, unary operators | Lambdas, walrus, conditional expressions initially |
| Access | Indexing, dictionary key access, attribute access, nested chains | Slicing initially; arbitrary reflection |
| Control flow | `if/elif/else`, `for`, `while`, `break`, `continue`, `pass` | `try/except`, `match`, `with`, async |
| Functions | `def` with arbitrary legal names, positional parameters, nested function calls, `return`, recursion | `*args`, `**kwargs`, closures/nonlocal, generators, async |
| Calls | Calls to user-defined functions; a deliberately documented subset of built-ins and collection methods | Arbitrary imports and third-party modules |
| Scope | Module scope and function-local scopes; parameter binding | Full Python scope and object model |
| Web syntax | Restricted `@app.route(...)` decorator and selected web primitives | Arbitrary decorators and full Flask API |

**Function rule:** Function behavior is defined by its statements, not its name. The following must both be accepted by the same grammar:

```python
def compute_total(price, quantity):
    return price * quantity

def remove_by_id(items, target_id):
    for item in items:
        if item["id"] == target_id:
            items.remove(item)
            return True
    return False
```

`remove()` is an illustrative call. For v1 it must either be in the supported method table **or** use the documented runtime-delegated-call policy below. Simply accepting a call syntactically does not mean every runtime method is semantically verified.

### 4.2 Function calls and Python runtime delegation

We will **parse generic** `name(args)` and `receiver.method(args)` call syntax. We will distinguish three cases:

1. **User-defined calls:** Resolve function names, number of parameters, and lexical scope when statically possible; generate equivalent Python functions/calls.
2. **Supported intrinsic operations:** Whitelist a manageable set (proposed: `len`, `range`, `str`, `int`, `float`, `print`; list `append`, `pop`, `remove`; dictionary `get`, `keys`, `values`). Define arity and simple type rules for these.
3. **Unknown calls:** **Reject in strict v1 mode** with a clear diagnostic, rather than quietly claiming to support all Python libraries. Optional future permissive mode may preserve unresolved calls as Python and defer failures to runtime, but this does not count as semantically validated support.

General functions **can perform any behavior expressible with these supported constructs**, not literally all behaviors available in the complete Python language.

### 4.3 Flask-like web syntax (language feature, not Flask dependency)

**Proposed source-language API:**

```python
app = Application()

@app.route("/", methods=["GET"])
def index():
    return render_template("index.html", items=items)

@app.route("/items/<int:item_id>", methods=["GET"])
def details(item_id):
    return render_template("details.html", item=find_by_id(items, item_id))

@app.route("/items", methods=["POST"])
def create():
    name = request.form["name"]
    # ordinary supported Python statements follow
    return redirect("/")
```

Recognize **generic route path patterns** (static paths plus `<int:name>`), explicit `GET`/`POST`, user-defined route handlers, `request.form`, `render_template`, and `redirect`. The runtime registers routes and calls matching handlers; the compiler does **not** generate code by looking for handler names like `create` or `details`.

The exact source spelling of `Application`, `render_template`, etc. is a **language/runtime contract to freeze before coding**. They are compiler-provided primitives, not Flask imports. Routes are processed as restricted decorator syntax, while function bodies use the general Python subset.

### 4.4 Jinja2 subset

| In scope | Example | Excluded initially |
|---|---|---|
| Interpolation | `{{ item.name }}` | Filters, arbitrary function execution |
| Loop | `{% for item in items %}...{% endfor %}` | Recursive loops, advanced loop helpers |
| Conditional | `{% if item.price > 0 %}...{% else %}...{% endif %}` | Full Jinja test/filter library |
| Nested blocks | Loops within conditions, conditions within loops | `extends`, `include`, `macro`, `import` |
| Expressions | Names, field/index lookup, literals, comparisons, boolean ops | Entire Jinja2 expression syntax |

**Variable scope:** Template loop variables are local to their loop. Render-context names are obtained from the `render_template(..., name=value)` call; the generator binds names from the Python AST and emits rendering code for the Jinja2 AST. Undefined names and invalid fields can be flagged when statically knowable.

### 4.5 HTML subset

- Elements: `html`, `head`, `title`, `body`, `div`, `span`, `p`, `h1`–`h3`, `ul`, `li`, `a`, `img`, `form`, `label`, `input`, `button`, `link`.
- Attributes: `id`, `class`, `href`, `src`, `alt`, `type`, `name`, `value`, `method`, `action`, `rel` plus template interpolations inside attribute values.
- Support opening/closing tags, nesting, text nodes, and void elements (`img`, `input`, `link`).
- Do not attempt the full HTML standard, DOM, scripts, custom tags, or browser parsing edge cases.

### 4.6 CSS subset

- Selectors: element (`p`), class (`.card`), ID (`#main`), simple selector lists separated by commas.
- Declaration blocks: `property: value;` with simple units and color values.
- Properties needed by the demo: `color`, `background-color`, `font-size`, `margin`, `padding`, `width`, `height`, `border`, `display`, `text-align`.
- CSS is parsed and **validated**, then generally emitted as CSS. No obligation to implement layout or rendering in Java.
- No animations, media queries, variables, preprocessors, or comprehensive selector grammar in v1.

### 4.7 Explicit non-goals

- A full CPython compiler, interpreter, bytecode VM, or complete Flask/Jinja2 replacement.
- General-purpose Python import/package handling and third-party dependency management.
- Arbitrary executable Python expressions inside templates.
- Front-end JavaScript frameworks, databases, authentication, or production hosting.
- Compiler rules tied to product fields, route handler names, or particular template filenames.
- Guaranteed compatibility with arbitrary existing Flask projects.

## 5. Compiler architecture

### Phase A — Parsing

- ANTLR 4 grammars target Java.
- Python lexer handles indentation correctly, including `INDENT` and `DEDENT` tokens.
- Template lexer must distinguish **HTML text**, `{{ ... }}` expressions, and `{% ... %}` control blocks; ANTLR lexer modes are a recommended approach.
- CSS can have its own lexer/parser. **Separate ANTLR parse trees do not imply a third required AST.** HTML and CSS constructs belong in (or are referenced by) the template AST.
- ANTLR error listeners collect line/column diagnostics, recover where safe, and reject unsupported syntax rather than misparse it.

### Phase B — Two ASTs

**Python AST examples**

```text
PythonModuleNode
  FunctionDefNode(name, parameters, body)
  RouteDecoratorNode(path, methods)
  AssignNode(target, value)
  ReturnNode(expression)
  IfNode / ForNode / WhileNode
  CallNode / BinaryExprNode / NameNode / LiteralNode
  ListNode / DictNode / AttributeNode / SubscriptNode
```

**Jinja2/template AST examples**

```text
TemplateDocumentNode
  ElementNode(tag, attributes, children)
  TextNode(text)
  InterpolationNode(expression)
  ForBlockNode(variable, iterable, body)
  IfBlockNode(condition, thenBody, elseBody)
  CssStylesheetNode(rules)   # as a child/reference, not a third mandated AST
```

Use Java abstract base types (`AstNode`, `StatementNode`, `ExpressionNode`, `TemplateNode`), visitors, inheritance and polymorphism. Every node records source file and start line/column (and optional end position). ASTs are **constructed with Java visitors**, not equated with ANTLR parse trees.

### Phase C — Symbols and semantic analysis

- Build module/function/template scopes, symbol tables, and a registry of routes and templates.
- Collect function declarations before checking calls so forward references work where allowed by the language contract.
- Check valid usage of variables, function calls, and known collection/object operations.
- Verify template file existence and `render_template` bindings.
- Validate template loops and field access whenever types/shapes can be inferred.
- For dynamically changing Python dictionaries, **static analysis has limits**. Document whether an operation is statically checked or deferred to runtime; do not promise perfect detection.

**Minimum six semantic diagnostics (more than the required five):**

| Code | Error case | Area |
|---|---|---|
| SEM001 | Undefined name in a Python expression | Python |
| SEM002 | Calling a declared function with wrong argument count | Python |
| SEM003 | Duplicate route registration for same path and method | Python/web |
| SEM004 | `render_template` refers to a missing template file | Cross-language |
| SEM005 | Template refers to a name not supplied by context or local scope | Jinja2 |
| SEM006 | Template `for` targets a statically known non-iterable | Jinja2 |

**Additional recommended diagnostic:** Access to an absent property/key on a *statically known* dictionary shape. Report it only when the shape is actually known. Each error needs a small negative test containing syntactically valid input, plus a source location and message.

### Phase D — Cross-language binding and generation

1. Resolve data declarations and route-handler context using the Python AST.
2. For each `render_template("name.html", ...)`, resolve a template AST and its available context bindings. **Do not assume a variable named `products`.**
3. Compile Jinja interpolation, conditions, and loops into safe Python rendering functions.
4. Generate Python equivalents of general supported functions and expressions.
5. Translate restricted web constructs into HTTP route registration and response handling using Python standard-library code (generated or packaged as a generated runtime module).
6. Emit validated HTML/CSS, and ensure HTML interpolation **escapes user-provided text** (including attributes as appropriate).
7. Produce runnable output and source-mapped error messages where reasonably feasible.

**Data transfer clarification:** The announcement requires moving data from a Python data array/list to the Jinja2 tree. We model this through template bindings derived from the Python AST; dynamically mutated data is passed at **request/render time**, rather than assuming all values are fixed at compile time.

## 6. Generated runtime behavior

The generated Python application is a **small, deliberately limited web server**, not Flask. Use standard library building blocks such as `http.server`, `urllib.parse`, and `html` (or equivalent generated helpers).

Minimum runtime functions:

- Start an HTTP server at a configurable local port.
- Match GET/POST requests against compiled static/dynamic routes.
- Decode path parameters and URL-encoded form bodies within a defined size limit.
- Invoke generated route functions and return HTML or redirects with correct status codes and headers.
- Render templates through compiler-generated Python functions.
- Serve predefined static CSS files safely; prevent directory traversal.
- Keep reference-application data **in memory**; changes need not survive a server restart.
- Escape output in HTML; avoid generating `eval`/`exec` of submitted source; return suitable errors (e.g., 404/405/400).

This runtime is **educational/local**, not production-hardened. It will not promise robust concurrency, user isolation, authentication, or internet-facing security.

## 7. Reference integration app (not grammar definition)

Use the generic language to implement these features:

| Page / action | HTTP behavior | Compiler capabilities exercised |
|---|---|---|
| List products | GET `/` | list iteration, template loop, interpolation |
| Product details | GET `/products/<int:id>` | route parameter, function call, conditional |
| Add product page | GET `/products/new` | HTML form, navigation |
| Submit new product | POST `/products` | request form, dictionary/list mutation, redirect |
| Delete product | POST `/products/<int:id>/delete` | searching/mutation, redirect |

Make navigation links work among these pages. Product data is seeded from a Python list of dictionaries; field choice (`id`, `name`, `price`) belongs to the **example**, not the grammar. The listing must reflect add/delete changes during the server process lifetime.

For genuine language generality, add a **second tiny program** (e.g., task list or student list) using different variable names, route names, and handler functions. Successful compilation without changing grammars/generator logic is a required design sanity check.

## 8. Printing and diagnostics (explicit announcement requirement)

Provide textual views for:

1. Each AST node with node type, relevant values, source line, and recursively printed children.
2. The complete **Python AST** and complete **template AST**, e.g., indented tree output.
3. The **symbol table**, including name, kind, scope, and source position; optionally inferred type.
4. Syntax and semantic error diagnostics with file, line, column, diagnostic code, and message.

Example:

```text
PythonModule @ app.py:1
  FunctionDef(index) @ app.py:4
    RouteDecorator(GET, /) @ app.py:3
    Return
      Call(render_template)

SYMBOLS
name      kind       scope      location
index     function   module     app.py:4
items     variable   module     app.py:2
```

The outputs need not match this exact textual style, but they must be comprehensive and readable.

## 9. Suggested Java project structure

```text
compiler-project/
  pom.xml                         # Maven / Java / ANTLR plugin
  src/main/antlr4/
    PythonSubsetLexer.g4
    PythonSubsetParser.g4
    TemplateLexer.g4
    TemplateParser.g4
    CssLexer.g4
    CssParser.g4
  src/main/java/edu/compiler/
    Main.java
    frontend/                     # parsers, error listeners
    ast/python/
    ast/template/
    ast/common/
    builder/                      # ANTLR parse tree -> AST visitors
    symbols/
    semantic/
    generator/
    diagnostics/
  src/test/java/
  examples/
    products/
    second-app/
    negative-tests/
  docs/
    language-spec.md
    ast-diagram.md
  README.md
```

Maven and the names shown are recommended organization, not mandated by the announcement.

## 10. How we will implement it — incremental milestones

| Stage | Deliverable | Exit condition |
|---|---|---|
| 0. Contract freeze | Supported syntax specification and positive/negative examples | Every construct is explicitly in/out; source web API settled |
| 1. Python frontend | ANTLR Python subset lexer/parser + indentation tests | General functions, data, conditionals, loops and routes parse |
| 2. Template/CSS frontend | ANTLR Jinja/HTML/CSS subset grammars | Nested template blocks, tags, attributes, CSS parse |
| 3. AST construction | Java AST model + parse-tree visitors | Both ASTs built and printable with source locations |
| 4. Symbols + semantics | Symbol tables, scopes, cross-language context and errors | Six specified semantic cases reported correctly |
| 5. General codegen | Python output for supported statements, expressions, functions | Non-web functions compile and execute correctly |
| 6. Web + template codegen | Route dispatcher, request binding, rendering | Generated server works without Flask or Jinja2 libraries |
| 7. End-to-end app | List/details/add/delete and navigation | All operations work through a browser |
| 8. QA + delivery | Automated tests, AST/symbol output, report, archive | Clean build and demo; required submission contents ready |

**Dependency order matters:** do not begin by hardcoding the product pages. First make generic expression/function generation work, then add web primitives and templates. Build at least one small end-to-end path early (e.g., GET `/` renders one template) to reveal integration risks before all features are complete.

## 11. Acceptance tests / definition of done

- [ ] Compiler code, visitors, nodes, semantics, and code generation written in Java.
- [ ] ANTLR generates Java lexer/parser classes for each supported source language.
- [ ] Valid general-purpose functions (different names and behaviors) parse and produce working Python.
- [ ] Invalid or excluded syntax produces a readable compiler diagnostic.
- [ ] Two ASTs exist as custom Java objects, with polymorphic node types and source locations.
- [ ] Complete ASTs, node children, and symbol table are printable.
- [ ] At least **five distinct semantic errors** are demonstrated across Python and Jinja2; aim for six listed in §5.
- [ ] Python list data reaches Jinja2 template rendering through explicit compiled bindings.
- [ ] Generated Python runs with standard-library Python and **without Flask or Jinja2 installed**.
- [ ] Listing/details/add/delete and navigation work in the integrated product demo.
- [ ] Adding/deleting a product updates subsequent listings without restarting the server.
- [ ] A second application with different domain names compiles with no special-case compiler changes.
- [ ] Generated output correctly escapes dynamic HTML and handles bad routes/forms at a basic level.
- [ ] Full source, report with AST diagram, GitHub/member file, and compressed group-number submission are packaged.

## 12. Risks and boundaries to watch

| Risk | Mitigation |
|---|---|
| Python indentation grammar complexity | Use established ANTLR indentation-token pattern and test nested blocks early |
| Mixing HTML text and template delimiters | Lexer modes and nested template-block tests |
| Treating ANTLR parse tree as AST | Dedicated typed Java AST plus visitor-based construction |
| Claiming arbitrary Python behavior | Explicit supported intrinsic set; reject unknown calls in strict mode |
| Static typing limitations of Python dictionaries | Partial/inferred checks; documented runtime checks where necessary |
| Mutating data before rendering | Bind contexts per HTTP request; do not pre-render all templates at compile time |
| Accidental Flask/Jinja2 dependency | Clean-environment runtime test with standard-library Python only |
| Product-specific special cases | Second domain smoke test and name-independent grammar/generator |
| Over-expanding syntax too early | Freeze v1 syntax and prioritize compiler stages and integration |

## 13. Final deliverables

1. Java/ANTLR compiler source and build instructions.
2. ANTLR grammars and documented supported syntax.
3. Java node classes, both ASTs, AST printing, and symbol-table printing.
4. Semantic diagnostics and at least five demonstrable semantic error cases.
5. Java code-generation pipeline and generated runnable Python web app.
6. Product example templates/CSS and a second domain-generalization example.
7. Automated compiler tests and a repeatable demo script.
8. Final report describing modules, decisions, AST diagram, examples, and limitations.
9. Text file containing GitHub URL and group member names; final archive named per announcement instructions.

## 14. Decisions to freeze in the language specification (before `.g4` files)

The high-level scope is settled, but exact semantics must be specified to avoid divergent implementations:

- Precise grammar for route decorators, supported path converters, and `Application()`.
- Supported Python literals, operator precedence, function defaults (currently excluded), and mutation methods.
- Exact built-ins/method signatures and strict handling of unknown calls.
- Static versus request-time template binding behavior; template variable scope.
- What kinds of attribute access are legal and which can be statically checked.
- Escaping rules, missing-value behavior, form decoding, and redirect semantics.
- Whether HTML/CSS should be structurally validated only, versus transformed by the generator.

**Recommended next document:** `docs/language-spec.md` containing numbered grammar features, legal/illegal examples, node types, semantic rules, and generation behavior for every supported construct.

---

### Source and provenance

- **Source:** attached Arabic *Compiler Project Announcement 2025/2026*, pages 2–3. Requirements in §2.1 reflect the announcement, including ASTs, five semantic errors, working generated parts, product views, and AST/symbol printing.
- **Clarified design:** Java-only compiler, ANTLR, no Flask dependency at execution, generated Python HTTP application, language-subset policy, and reusable general-purpose functions reflect the agreed project decisions. Specific APIs, feature lists, implementation milestones, and example error codes in this document are **proposals** to be frozen by the team, not direct wording from the announcement.
