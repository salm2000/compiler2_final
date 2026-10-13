# Restricted Python Web Language — Language Specification

> **Status:** Proposed normative v1.0 baseline — ready for team review, not an implemented language  
> **Companion:** [`COMPILER_PROJECT_SCOPE.md`](COMPILER_PROJECT_SCOPE.md)  
> **Implementation:** Java 17+ and ANTLR 4 (Java target)  
> **Input:** restricted Python-like application; Jinja2-style HTML templates; restricted CSS  
> **Output:** executable Python 3 application using the Python standard library, with no Flask or Jinja2 dependency

## 1. Purpose, boundaries, and terminology

This document specifies the **language contract** the compiler will accept, check, and generate. It defines **general-purpose Python statements and functions**, not special syntax for product-related functions. A product management app is an integration fixture only; another app using different names must compile without changes to compiler code.

The university announcement requires lexer/parser stages; separate **Python** and **Jinja2/template** ASTs; OOP node design; semantic checks in both parts (at least five errors); code generation; integrated listing/add/details/delete pages; and printed AST/node hierarchy and symbol table. The detailed syntax and semantics below are **our chosen subset**, not additional claims about the announcement.

**Meaning of support**:
- **Parse:** ANTLR accepts the construct and produces a parse tree.
- **Build:** a Java visitor produces a typed custom AST node.
- **Analyze:** semantic checks are applied to the extent specified here.
- **Generate:** Java emits Python, HTML, and/or CSS preserving defined behavior.
- **Run:** the generated code executes under ordinary Python 3 with no Flask/Jinja2 installation.

**Hard boundary:** only explicitly supported constructs have guaranteed behavior. An arbitrary function name is allowed, but an arbitrary full-Python program is **not**. Syntax recognized by the grammar but unresolved at semantic analysis must not be silently claimed as supported.

## 2. Input and compilation units

Recommended project layout:

```text
source/
  app.py
  templates/
    index.html
    details.html
    form.html
  static/
    style.css
```

The compiler entrypoint reads one `app.py`, recursively parses every `.html` file under `templates/` and every selected `.css` file under `static/`. v1 does **not** import arbitrary Python modules. Template filenames are resolved relative to the template directory, with no `../` traversal. The generated output may contain `app.py`, `runtime.py`, generated renderers, and copied/validated static assets.

The compiler must provide `compile`, `ast`, and `symbols` operations; exact CLI flags are implementation-specific (the companion scope document proposes examples). A compilation with errors must exit nonzero and not mark output as successfully built.

## 3. Python lexical rules

| Feature | v1 rule |
|---|---|
| Source encoding | UTF-8 |
| Identifiers | `[A-Za-z_][A-Za-z0-9_]*`; case-sensitive; keyword spellings reserved |
| Keywords | `def`, `return`, `if`, `elif`, `else`, `for`, `in`, `while`, `break`, `continue`, `pass`, `and`, `or`, `not`, `True`, `False`, `None` |
| Indentation | Four spaces recommended; positive indent increases emit `INDENT`, decreases emit `DEDENT`; inconsistent indentation is an error |
| Newlines | End statements except within matched `()`, `[]`, `{}`; blank/comment-only lines ignored for indentation |
| Comments | `# ...` to end of line |
| Integer | Decimal (`0`, `19`, `452`), optionally preceded by unary minus |
| Float | Decimal form (`3.0`, `0.25`, `12.5`); no exponent in v1 |
| String | Single- or double-quoted, with `\\`, `\'`, `\"`, `\n`, `\t` escapes |
| Not supported | Triple quotes, f-strings, bytes, raw strings, tabs for indentation, continuation with `\\`, Unicode identifiers, type annotations |

**Lexer implementation note:** Python indentation cannot be approximated by simply skipping whitespace. Use an explicit indentation-token strategy with a stack and tests for nested blocks and EOF dedents.

## 4. Python syntax and behavior

### 4.1 Values and literals

Accepted literal value categories: `int`, `float`, `str`, `bool`, `None`, **list**, **dictionary**. Lists and dictionaries may contain nested supported expressions; dictionary keys must be strings or integers in v1.

```python
count = 3
ratio = 0.5
active = True
nothing = None
names = ["Ada", "Lin"]
item = {"id": 1, "name": "Book", "tags": ["new"]}
```

No tuple/set literals or comprehensions. An empty list `[]` and dictionary `{}` are valid.

### 4.2 Expressions (high to low precedence)

| Precedence | Operators/forms | Associativity/notes |
|---|---|---|
| 1 | literals, identifiers, parenthesized expression, list/dict literals | primary |
| 2 | `f(...)`, `x[i]`, `x.attr`, `x.method(...)` | postfix; chainable |
| 3 | `+x`, `-x`, `not x` | unary; `not` binds below comparisons in actual Python; grammar must reproduce Python grouping |
| 4 | `*`, `/`, `//`, `%` | left |
| 5 | `+`, `-` | left |
| 6 | `==`, `!=`, `<`, `<=`, `>`, `>=`, `in` | non-chained comparisons in v1 |
| 7 | `not` | below comparisons |
| 8 | `and` | left, short circuit |
| 9 | `or` | left, short circuit |

For avoidance of doubt, the intended binding from tightest to loosest is **postfix → unary `+/-` → multiplication → addition → comparison/`in` → `not` → `and` → `or`**. `not x == y` means `not (x == y)`. Parentheses override precedence. No exponentiation, slicing, ternary, `is`, bitwise operators, or chained comparisons.

Arithmetic and comparison behavior normally follows the Python 3 runtime for supported operand types. The analyzer catches operand-type errors when types are statically known; otherwise Python may raise a runtime exception. Logical operators preserve Python short-circuit evaluation and return-value semantics.

### 4.3 Assignments, mutation, and indexing

```python
amount = 5
amount += 2
amount -= 1
rows = []
rows.append({"id": 1})
rows[0]["id"] = 2
```

Allowed assignment targets: a **name**, a subscript (`x[i]`) or an attribute only when that attribute is explicitly supported by the runtime object contract. Allowed augmented assignment for v1: `+=`, `-=` on names only. A plain reassignment does not create a new local scope. No tuple unpacking, chained assignment, `del`, `global`, or `nonlocal`.

**Important:** list/dictionary mutations are runtime operations; compile-time template binding must never freeze a mutable list at the initial value.

### 4.4 Statements and blocks

```python
if score >= 70:
    result = "pass"
elif score >= 50:
    result = "review"
else:
    result = "retry"

for item in items:
    print(item)

while n > 0:
    n -= 1
```

- A suite begins after `:` + `NEWLINE` + `INDENT` and ends with `DEDENT`.
- Supported statements: assignment, expression statement, `return`, `if/elif/else`, `for`, `while`, `break`, `continue`, `pass`, and function definition.
- `for name in expression:` binds `name` for each element; v1 permits a **single-name** loop target only.
- `break` and `continue` are only legal inside loops.
- `return` is only legal inside a function and may omit its expression (equivalent to `None`).
- No `for/else`, `while/else`, exceptions, pattern matching, async or context managers.

### 4.5 Functions and calls

```python
def combine(a, b):
    return a + b

def total(items):
    result = 0
    for x in items:
        result += x
    return result

answer = combine(2, total([3, 4]))
```

- `def IDENTIFIER(param1, param2, ...): suite` defines an arbitrary named function; zero parameters accepted.
- Calls accept **positional expressions**; no named arguments for ordinary functions in v1. The `render_template` intrinsic is a deliberate exception (§5).
- Support nested calls and recursion. A function with no explicit return returns `None`.
- Module-global functions/variables are visible to functions for reading. **Assignments within a function are local**; mutation of a global list or dictionary through its methods/subscripts is allowed. No assignment to module-level name from a function without `global` (unsupported).
- Function declarations are module-level only for v1 (nested `def` excluded). Functions are collected before call resolution, enabling calls to declarations later in the file. Duplicate declarations in one scope are errors.
- Parameter names must be unique; wrong argument count to a known function is an error.
- **No** `lambda`, default parameters, `*args`, `**kwargs`, closures, keyword-only params, or return annotations.

### 4.6 Built-ins and allowed collection methods

This is a **semantic allowlist**, not a function-name-based grammar. Generic call AST nodes support these names and user-defined functions identically.

| Operation | Accepted arity | Behavior |
|---|---|---|
| `len(value)` | 1 | length of list, dict or string |
| `range(stop)` / `range(start, stop)` / `range(start, stop, step)` | 1–3 | Python range (step must not be zero) |
| `str(value)` | 1 | Python string conversion |
| `int(value)` | 1 | Python integer conversion |
| `float(value)` | 1 | Python float conversion |
| `print(...)` | 0+ | stdout, not HTTP response |
| `list.append(value)` | 1 | mutate list |
| `list.pop()` / `list.pop(index)` | 0–1 | remove/return element |
| `list.remove(value)` | 1 | remove first matching element |
| `dict.get(key)` / `dict.get(key, default)` | 1–2 | lookup |
| `dict.keys()` / `dict.values()` | 0 | iterable view |

Unknown global function calls, methods, and imports are rejected in **strict v1** unless they are recognized web intrinsics. For partially-known runtime types, the analyzer may issue a warning and defer method validity to runtime; diagnostics must say so. The user can define any function name except reserved intrinsic names and the configured `app` identifier.

## 5. Flask-like source web API (without Flask)

The following identifiers are **compiler-provided intrinsics** implemented by generated Python code; they are **not** third-party imports.

```python
app = Application()

@app.route("/", methods=["GET"])
def index():
    return render_template("index.html", items=items)

@app.route("/items/<int:item_id>", methods=["GET"])
def details(item_id):
    return render_template("details.html", item=get_item(item_id))

@app.route("/items", methods=["POST"])
def create():
    name = request.form["name"]
    items.append({"name": name})
    return redirect("/")
```

### 5.1 Route declaration

- Exactly one module-scope declaration `app = Application()` in v1.
- A route is a decorator immediately preceding a module-level function: `@app.route("/path", methods=["GET", "POST"])`.
- `methods=` may be omitted, defaulting to `GET`; allowed methods are `GET` and `POST` only.
- Paths begin with `/`. Segments are static (`/items/new`) or typed dynamic `<int:name>`; dynamic names become positional handler parameters of type integer.
- Matching uses **static routes before dynamic routes** at equal path depth (so `/items/new` doesn't collide with `/items/<int:id>`).
- A pair (HTTP method, route path pattern) must be unique. Conflicting parameter declarations or handler signature mismatches are semantic errors.
- No generic decorators, `app.run()`, Flask imports, route blueprints, middlewares, or wildcards.

### 5.2 Request and response intrinsics

| Construct | Meaning | Required validation |
|---|---|---|
| `request.form["field"]` | URL-encoded POST form field as string | only inside route; missing field produces HTTP 400 |
| `render_template("name.html", key=expr, ...)` | render a named template with explicit context | first arg static string; file must exist; context names unique |
| `redirect("/path")` | HTTP 303 redirect to a local path | static or expression yielding safe local path; prevent external open redirects |
| `return "text"` | plain string response | HTML or plain-text choice fixed by generator policy (v1: `text/html; charset=utf-8`) |

Named arguments are supported **only** in `render_template`, where they define the template context; all other calls use positional arguments. `request.form` is read-only. Optional v1 choice: emit a 404 for a route handler returning `None` (document as policy, not assumed Python behavior).

### 5.3 Runtime contract

- Output runs using `http.server` and other Python standard-library modules.
- Routes support GET and POST; URL-encoded form bodies only, no multipart upload.
- Dynamic `<int:name>` segments decode decimal integers; malformed values produce 404.
- HTTP responses have proper status codes, content type and `Location` on redirect.
- `404` unknown path; `405` known path but unsupported HTTP method; `400` malformed input; internal failures become `500` with no stack trace exposed in browser.
- Output serves validated CSS assets below `/static/` with traversal protection and a request-body size cap.
- State is in memory; list mutations persist across requests while server is running, not across restarts.
- This is a local educational server, **not production-grade hosting**.

## 6. Jinja2-style template subset

Templates mix recognized HTML elements/text with **Jinja2-style** tags. The implementation does not import or execute the Jinja2 library.

### 6.1 Syntax

```jinja2
<h1>{{ title }}</h1>
<ul>
{% for item in items %}
  <li>
    {{ item.name }}
    {% if item.price > 100 %}
      Expensive
    {% elif item.price == 100 %}
      Exact
    {% else %}
      Affordable
    {% endif %}
  </li>
{% endfor %}
</ul>
```

| Tag or expression | v1 support |
|---|---|
| `{{ expr }}` | variable, literal, field `x.name`, index `x[0]` or `x["name"]`, arithmetic/comparison/boolean expressions; no function call |
| `{% for name in expr %}...{% endfor %}` | one loop variable; iterable must be list/string/dict or statically unknown |
| `{% if expr %}...{% elif expr %}...{% else %}...{% endif %}` | zero or more `elif`; `else` optional |
| Nested tags | arbitrary finite nested supported loops/conditions |
| Whitespace | literal whitespace outside tags preserved as written; trimming flags not supported |
| Not supported | `{% extends %}`, `{% include %}`, macros, filters (`|safe` etc.), `set`, tests (`is defined`), function calls, inline Python, comments `{# #}` in v1 |

Template expression operators follow the Python subset precedence and semantics where applicable. In template AST, attribute access `item.name` on dictionary-backed values is treated as lookup of key `"name"`. Undefined values are not silently substituted with empty text.

### 6.2 Binding and scope

- Every `render_template("file.html", a=x, b=y)` makes `a`, `b` available as template symbols at **render time**.
- Each template invocation has a fresh context; the same template can render with different values.
- Loop variable is visible in its body, shadows an outer variable, and is not visible outside the loop.
- `if` does not create a new scope.
- An absent template variable that is **provably** not supplied is `SEM005`; dynamic cases may be deferred, but never invented.
- Dynamic list/dictionary changes are seen by subsequent render requests.
- Context values are passed from generated Python **runtime evaluation** of source AST expressions. The Java compiler is not required to execute full Python expressions at compile time.

### 6.3 Escaping and rendering

- `{{ expression }}` emits HTML-escaped text by default using equivalent of `html.escape(str(value), quote=True)`.
- Interpolation inside HTML attribute values receives attribute-safe escaping too.
- No unescaped/raw HTML interpolation syntax in v1.
- `None` renders as empty text (a documented project policy); booleans/numbers use `str()`.
- Bad runtime lookup fails predictably rather than executing injected code.

## 7. HTML subset

Supported tags: `html`, `head`, `title`, `body`, `div`, `span`, `p`, `h1`, `h2`, `h3`, `ul`, `li`, `a`, `img`, `form`, `label`, `input`, `button`, `link`.

Supported attributes: `id`, `class`, `href`, `src`, `alt`, `type`, `name`, `value`, `method`, `action`, `rel`. Attribute values may contain static text and `{{ expression }}` pieces. Attributes require quoted values; boolean attributes not supported in v1.

- Regular tags require matching closing tags, nesting must be correct.
- Void tags `img`, `input`, `link` do not require a closing tag.
- Text and whitespace between tags preserved.
- HTML comments and character entities can be preserved literally (validation may be limited).
- A supported template may mix HTML nodes and Jinja tags at any legal nesting boundary.
- Unknown HTML tags/attributes produce a clear unsupported-feature diagnostic rather than being misinterpreted.
- No `<script>`, JavaScript, SVG, `<style>` inline CSS, table layouts, or browser DOM simulation.

HTML rendering is handled by the browser; the compiler checks structure and emits HTML, not a Java-side browser renderer.

## 8. CSS subset

Supported selector syntax:

```css
body { color: black; }
.card, #main { background-color: white; padding: 10px; }
a { text-align: center; }
```

- Simple type (`div`), class (`.card`), ID (`#main`) selectors; comma-separated groups allowed.
- Rules are `selector-list { property: value; ... }`.
- Allowed properties: `color`, `background-color`, `font-size`, `margin`, `padding`, `width`, `height`, `border`, `display`, `text-align`.
- Values: identifier keywords (e.g. `black`, `block`, `center`), hex colors (`#RGB`, `#RRGGBB`), integers/decimals optionally suffixed `px`, `%`, `em`, or `rem`, and a short sequence of these tokens for e.g. `border: 1px solid gray`.
- Final semicolon may be optional if explicitly handled by grammar; **v1 policy: require a semicolon on every declaration**.
- CSS is checked then emitted essentially unchanged, preserving semantic order.
- No selector combinators, pseudo-selectors, `@media`, animation, variables, functions such as `calc()`, or shorthands outside the stated token grammar.

**AST policy:** CSS may have internal parse structures or CSS nodes referenced by the template AST; the announcement requires **two primary ASTs**, not a separate mandatory third AST.

## 9. Abstract syntax tree contracts

ANTLR parse trees must **not** be treated as the final ASTs. Use Java AST node classes with common inheritance and polymorphic visitors; record `file`, `line`, and `column` on every node.

### 9.1 Python AST nodes (minimum)

```text
Module, FunctionDef, Parameter, RouteDecorator, Block
Assign, AugAssign, ExprStmt, Return, If, For, While, Break, Continue, Pass
Name, Literal, ListLiteral, DictLiteral, UnaryExpr, BinaryExpr, CompareExpr
Call, AttributeAccess, SubscriptAccess
```

### 9.2 Jinja/template AST nodes (minimum)

```text
TemplateDocument, HtmlElement, HtmlAttribute, Text, Interpolation
ForBlock, IfBlock, TemplateName, TemplateLiteral
TemplateBinaryExpr, TemplateUnaryExpr, TemplateAttribute, TemplateSubscript
CssStylesheetRef / CssRule / CssDeclaration (as nodes or attached structures)
```

`If` nodes carry ordered `elif` branches and optional `else` body. `For` nodes carry iterator variable, iterable expression and body. Route decorators attach to function nodes (no special `delete_product` node).

### 9.3 Example AST outputs

```text
Module @ app.py:1
  FunctionDef(total, params=[items]) @ app.py:1
    Block
      Assign(result)
        Literal(0)
      For(name=x)
        Name(items)
        AugAssign(result, +=)
          Name(x)
      Return
        Name(result)
```

Provide full-tree and per-node recursive printing for **both** trees, plus symbol-table dump. These are compiler diagnostics, not webpage printing.

## 10. Static semantics and type policy

The compiler is **partially statically checked**; Python is dynamically typed. Implement a bounded inferred type domain: `Int`, `Float`, `String`, `Bool`, `None`, `List[T]`, `Dict[K,V]` (optionally known literal keys), `Function(arity)`, `Unknown`.

Recommended validation passes:

1. Gather module functions, parameters, module names and web routes.
2. Build scopes and resolve symbol uses.
3. Validate expression/statement contexts, call arities, supported intrinsics and `break`/`return` placement.
4. Validate route patterns, route-handler parameter bindings, and uniqueness.
5. Resolve template files and infer context bindings at all reachable `render_template` call sites.
6. Validate template locals, expressions, and statically known iterable/key access.
7. Validate HTML structure/attributes and CSS rules/properties.

| Diagnostic | Meaning | Minimal invalid example |
|---|---|---|
| `SEM001` | Undefined Python identifier | `answer = missing_name` |
| `SEM002` | Wrong positional arity to declared function | `def f(x): return x` then `f()` |
| `SEM003` | Duplicate route for same method and pattern | two `@app.route("/")` functions |
| `SEM004` | Template file not found | `render_template("missing.html")` |
| `SEM005` | Template variable not supplied | `{{ absent }}` with no `absent` context binding |
| `SEM006` | Template loop over known non-iterable | `{% for x in 3 %}` |
| `SEM007` | Unsupported/unknown method or function | `mystery(1)` |
| `SEM008` | Illegal `break` / `continue` / `return` context | top-level `return 1` |
| `SEM009` | Duplicate variable/parameter declaration where forbidden | `def f(x, x): ...` |
| `SEM010` | Known nonexistent dictionary field | `{{ item.unlisted }}` with known exact shape |
| `SEM011` | Route parameter/handler mismatch | path has `<int:id>`, handler has no `id` |
| `SEM012` | Illegal operand pairing when statically known | `3 + []` |

Demonstrate at least five **distinct** semantic cases across Python and Jinja2, including at least two in each area. Static analysis should avoid false certainty about values mutated dynamically. Unresolved runtime properties must be documented as checks deferred to Python execution.

Syntax/lexical errors use `LEXxxx`/`SYNxxx` codes, not semantic codes. All diagnostics include file path, 1-based line/column, code, description, and severity.

## 11. Code generation semantics

The generated Python must derive from the **AST**, never by copying unparsed input source wholesale.

1. Emit validated Python statements and functions preserving indentation and control flow.
2. Translate route metadata into registration/dispatch with no Flask import.
3. Emit Python render functions from template AST; interpolation escapes text.
4. Pass route-time context values to those render functions; do not pre-render static snapshots of mutable lists.
5. Include or generate bounded HTTP helpers (`http.server`, `urllib.parse`, `html` etc.).
6. Emit validated static CSS safely; only serve files from an output static directory.
7. Fail compilation on unsupported source features or semantic errors; diagnostics should be inspectable even after an error.

Generated helper identifiers should use reserved internal prefixes (for example, `__cpl_`) to avoid collision with source identifiers; source identifiers using that prefix are rejected.

### 11.1 Worked input-to-output sketch

Source:

```python
app = Application()
items = [{"name": "Notebook"}]

@app.route("/")
def home():
    return render_template("home.html", things=items)
```

Template `home.html`:

```html
<ul>{% for thing in things %}<li>{{ thing.name }}</li>{% endfor %}</ul>
```

**Required effect:** generated Python registers GET `/`, evaluates `things=items` on each request, loops through the current list, HTML-escapes each name, and returns an HTTP 200 HTML response. The output contains no Flask/Jinja2 import. The exact emitted Python helper function names are an implementation choice.

## 12. Normative acceptance corpus

| ID | Scenario | Expected result |
|---|---|---|
| `P01` | Arithmetic precedence and parentheses | correct result |
| `P02` | Arbitrary `def`, nested calls and `return` | correct result |
| `P03` | Recursion and forward-declared function call | supported |
| `P04` | `if/elif/else`, `for`, `while`, `break`, `continue` | supported |
| `P05` | List/dictionary reads and mutation | supported |
| `P06` | Unknown import or unknown function call | compiler diagnostic |
| `P07` | Bad indentation | lexer/parser diagnostic |
| `W01` | GET static route | HTTP 200 expected body |
| `W02` | Typed path parameter | parsed integer in handler |
| `W03` | POST form submission then redirect | 303; subsequent list reflects change |
| `W04` | Unknown route / unsupported method | 404 / 405 |
| `T01` | Nested Jinja loop and conditions | correct rendered HTML |
| `T02` | Template variable context binding | correct values at request time |
| `T03` | HTML escaping `<`, `>`, `&`, quote | no raw injection |
| `T04` | Undefined template binding | semantic diagnostic if statically provable |
| `H01` | Valid supported tags/attributes | accepted |
| `H02` | Unmatched HTML tag | diagnostic |
| `C01` | Simple CSS class/ID/type rules | accepted and emitted |
| `C02` | Unsupported CSS rule/property | diagnostic |
| `G01` | Second non-product web app | compiles without special-case Java changes |
| `D01` | AST + symbols dumping | all nodes, children, locations and scopes printed |
| `D02` | Six or more distinct semantic-negative fixtures | expected diagnostics |

For each test keep source inputs, expected diagnostics or outputs, and generated-run checks under version control. Tests of generated Python should run in an environment **without** Flask or Jinja2 installed.

## 13. Deliberate exclusions and extensions

**Not in v1:** classes, inheritance in source Python, decorators other than `@app.route`, imports, `try/except`, comprehensions, slicing, tuples/sets, f-strings, generators, async, modules/packages, arbitrary Python libraries, full Flask, full Jinja2 inheritance/filters, JS, full HTML/CSS, database persistence, authentication, and production deployment.

**Possible v2:** named arguments generally, `list`/`dict` comprehensions, template includes, HTML forms with additional controls, more CSS properties, `request.query`, safe persistent JSON storage, selected import allowlist. These are **not** acceptance prerequisites.

## 14. Implementation mapping (ANTLR → Java → Python)

| Layer | Implementation approach | Produced artifact |
|---|---|---|
| Lexers/parsers | ANTLR 4 `.g4` with Java target; indentation strategy for Python; lexer modes for templates | generated Java classes |
| AST visitors | Java visitors of ANTLR parse trees | Python AST + Jinja2/template AST |
| Symbols/semantics | Java scope graph, symbol table, type approximations, validators | diagnostics + bound ASTs |
| Python emitter | Java AST visitor, deterministic formatting | emitted user function/control-flow Python |
| Template emitter | Java template AST visitor | generated render functions |
| Web emitter | Java route-table generation + Python standard library helpers | runnable web server |
| QA | Java tests plus subprocess/browser-level HTTP checks | evidence that accepted source works |

**Recommended build order:** lex/parse subset → AST printing → symbols & semantic-negative tests → generic Python emitter → template emitter → generated HTTP runtime → product integration scenario → second-domain scenario → regression and submission.

## 15. Freeze decisions and unresolved details

This v1 document chooses explicit default behavior. Before coding, the team should approve these decisions together:

1. **Function scope:** module-level functions only; global reads and collection mutation permitted, no `global` keyword.
2. **Call policy:** strict allowlist of standard built-ins/methods; unknown calls rejected.
3. **Web contract:** `Application()`, `@app.route`, `request.form`, `render_template`, `redirect`; no Flask package.
4. **Template contract:** `for`, `if/elif/else`, interpolation, no filters/functions/includes; escaped output.
5. **Storage:** in-memory across requests; no persistence across restarts.
6. **Grammar strictness:** excluded syntax fails explicitly, rather than passing through as unparsed text.
7. **HTML/CSS:** structural validation and emission, not implementation of a browser.
8. **Type checking:** best effort, no promise of full static inference for dynamic Python.

If any choice changes, update this file, the syntax tests, and the scope document **before** changing grammar files.

---

## References and provenance

- **Academic requirements:** attached *Compiler Project Announcement (2025/2026)*, pages 2–3; it fixes required compiler phases, two ASTs, minimum semantic-error demonstration, integrated product operations and AST/symbol printing. It does **not** prescribe the exact subset specified here.
- **Engineering decisions:** team discussion and [`COMPILER_PROJECT_SCOPE.md`](COMPILER_PROJECT_SCOPE.md). The Java/ANTLR choice, restricted syntax, generated standard-library Python runtime, and feature-specific grammar/semantic rules are project decisions.
