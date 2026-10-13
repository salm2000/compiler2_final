# AST architecture

ANTLR parse trees are converted into two independent object-oriented ASTs. Every concrete node inherits from `AstNode`, carries a `SourceLocation`, exposes its children polymorphically, and accepts an `AstVisitor`.

```mermaid
classDiagram
  class AstNode {
    +SourceLocation location
    +kind()
    +children()
    +accept(AstVisitor)
  }
  class PythonNode
  class Statement
  class Expression
  class PythonModule
  class FunctionDef
  class Assign
  class If
  class Call
  class TemplateNode
  class TemplateDocument
  class HtmlElement
  class Interpolation
  class ForBlock
  class IfBlock

  AstNode <|-- PythonNode
  PythonNode <|-- Statement
  PythonNode <|-- Expression
  PythonNode <|-- PythonModule
  Statement <|-- FunctionDef
  Statement <|-- Assign
  Statement <|-- If
  Expression <|-- Call
  AstNode <|-- TemplateNode
  TemplateNode <|-- TemplateDocument
  TemplateNode <|-- HtmlElement
  TemplateNode <|-- Interpolation
  TemplateNode <|-- ForBlock
  TemplateNode <|-- IfBlock
```

The Python AST supplies route handlers and the explicit arguments of each `render_template` call. Semantic analysis uses those call sites to establish template scopes. The generator then emits Python functions for both ASTs and joins them through a generated render-function registry; no source file is copied through as unparsed Python.

