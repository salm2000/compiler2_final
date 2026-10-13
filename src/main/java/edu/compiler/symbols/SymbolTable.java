package edu.compiler.symbols;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SymbolTable {
    private final List<Symbol> symbols = new ArrayList<>();
    public void add(Symbol symbol) { symbols.add(symbol); }
    public List<Symbol> symbols() { return List.copyOf(symbols); }
    public String dump() {
        StringBuilder out = new StringBuilder("SYMBOLS\nname                 kind              scope                 location                 type\n");
        symbols.stream().sorted(Comparator.comparing(Symbol::scope).thenComparing(Symbol::name)).forEach(s ->
            out.append("%-20s %-17s %-21s %-24s %s%n".formatted(s.name(),s.kind(),s.scope(),s.location(),s.inferredType())));
        return out.toString();
    }
}

