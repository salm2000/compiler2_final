package edu.compiler.symbols;

import edu.compiler.ast.SourceLocation;

public record Symbol(String name, Kind kind, String scope, SourceLocation location, String inferredType) {
    public enum Kind { VARIABLE, FUNCTION, PARAMETER, LOOP_VARIABLE, TEMPLATE_BINDING }
}

