package org.qualcomm.manjoosha;

import java.util.EnumMap;
import java.util.HashMap;

public class SymbolTable {

    private final HashMap<String, Symbol> classSymbols;
    private final HashMap<String, Symbol> subroutineSymbols;
    private final EnumMap<Symbol.Kind, Integer> indices;
    
    public SymbolTable() {
        classSymbols = new HashMap<>();
        subroutineSymbols = new HashMap<>();

        indices = new EnumMap<>(Symbol.Kind.class);
        indices.put(Symbol.Kind.ARG, 0);
        indices.put(Symbol.Kind.FIELD, 0);
        indices.put(Symbol.Kind.STATIC, 0);
        indices.put(Symbol.Kind.VAR, 0);
    }
    
    public void startSubroutine() {
        subroutineSymbols.clear();
        indices.put(Symbol.Kind.VAR, 0);
        indices.put(Symbol.Kind.ARG, 0);
    }
    
    public void define(String name, String type, Symbol.Kind kind) {
        if (kind == Symbol.Kind.ARG || kind == Symbol.Kind.VAR) {
            int index = indices.get(kind);
            var symbol = new Symbol(type, kind, index);
            indices.put(kind, indices.get(kind) + 1);
            subroutineSymbols.put(name, symbol);
        } else if (kind == Symbol.Kind.STATIC || kind == Symbol.Kind.FIELD) {
            int index = indices.get(kind);
            var symbol = new Symbol(type, kind, index);
            indices.put(kind, index + 1);
            classSymbols.put(name, symbol);
        }
    }
    
    public int varCount(Symbol.Kind kind) {
        return indices.get(kind);
    }
    
    public Symbol.Kind kindOf(String name) {
        var symbol = lookUp(name);
        return symbol != null ? symbol.getKind() : Symbol.Kind.NONE;
    }

    public String typeOf(String name) {
        var symbol = lookUp(name);
        if (symbol != null) return symbol.getType();
        return "";
    }

    public int indexOf(String name) {

        var symbol = lookUp(name);

        if (symbol != null) return symbol.getIndex();

        return -1;
    }

    private Symbol lookUp(String name) {
        if (classSymbols.get(name) != null) {
            return classSymbols.get(name);
        } else if (subroutineSymbols.get(name) != null) {
            return subroutineSymbols.get(name);
        } else {
            return null;
        }

    }

}
