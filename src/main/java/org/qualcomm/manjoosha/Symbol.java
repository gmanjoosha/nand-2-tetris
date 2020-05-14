package org.qualcomm.manjoosha;

public class Symbol {

    private final String type;
    private final Kind kind;
    private final int index;
    
    public Symbol(String type, Kind kind, int index) {
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public Kind getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return String.format("Symbol{type='%s', kind=%s, index=%d}", type, kind, index);
    }

    public enum Kind {STATIC, FIELD, ARG, VAR, NONE}
}
