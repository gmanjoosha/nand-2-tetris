package org.qualcomm.manjoosha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CompilationEngine {

    private final VMCodeGenerator vmCodeGenerator;
    private final JackTokenizer jackTokenizer;
    private final SymbolTable symbolTable;
    private String currentClass;
    private String currentSubroutine;

    private int labelIndex;

    public CompilationEngine(File inputFile, File outputFile) {
        jackTokenizer = new JackTokenizer(inputFile);
        vmCodeGenerator = new VMCodeGenerator(outputFile);
        symbolTable = new SymbolTable();
        labelIndex = 0;
    }

    private String currentFunction() {
        return !currentClass.isEmpty() && !currentSubroutine.isEmpty() ? currentClass + "." + currentSubroutine : "";
    }

    private String compileType() {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD
                && (jackTokenizer.keyWord() == JackTokenizer.KEYWORD.INT
                || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.CHAR
                || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.BOOLEAN)) {
            return jackTokenizer.getCurrentToken();
        }
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.IDENTIFIER) {
            return jackTokenizer.identifier();
        }
        unexpectedToken("in|char|boolean|className");
        return "";
    }

    public void compileClass() {
        jackTokenizer.next();
        checkForKeyWordClass();

        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
            unexpectedToken("className");
        }
        currentClass = jackTokenizer.identifier();
        expectedSymbol('{');
        compileClassVarDec();
        compileSubroutine();
        expectedSymbol('}');
        if (jackTokenizer.hasNextToken()) {
            throw new IllegalStateException("Unexpected tokens");
        }
        vmCodeGenerator.close();
    }

    private void checkForKeyWordClass() {
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD
                || jackTokenizer.keyWord() != JackTokenizer.KEYWORD.CLASS) {
            System.out.println(jackTokenizer.getCurrentToken());
            unexpectedToken("class");
        }
    }

    private void compileClassVarDec() {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jackTokenizer.symbol() == '}') {
            jackTokenizer.previous();
            return;
        }
        if (isSubroutineKeyword()) {
            jackTokenizer.previous();
            return;
        } else {
            unexpectedToken("Keywords");
        }
        if (jackTokenizer.keyWord() != JackTokenizer.KEYWORD.STATIC && jackTokenizer.keyWord() != JackTokenizer.KEYWORD.FIELD) {
            unexpectedToken("static or field");
        }
        Symbol.Kind kind = null;
        switch (jackTokenizer.keyWord()) {
            case STATIC -> kind = Symbol.Kind.STATIC;
            case FIELD -> kind = Symbol.Kind.FIELD;
        }

        var compileType = compileType();
        do {
            jackTokenizer.next();
            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
                unexpectedToken("identifier");
            }
            var variableName = jackTokenizer.identifier();
            symbolTable.define(variableName, compileType, kind);
            jackTokenizer.next();
            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL
                    || (jackTokenizer.symbol() != ','
                    && jackTokenizer.symbol() != ';')) {
                unexpectedToken("',' or ';'");
            }
        } while (jackTokenizer.symbol() != ';');
        compileClassVarDec();
    }

    private boolean isSubroutineKeyword() {
        return jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD
                && (jackTokenizer.keyWord() == JackTokenizer.KEYWORD.CONSTRUCTOR
                || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.FUNCTION
                || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.METHOD);
    }

    private void compileSubroutine() {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jackTokenizer.symbol() == '}') {
            jackTokenizer.previous();
            return;
        }

        if (!isSubroutineKeyword()) {
            unexpectedToken("constructor|function|method");
        }

        var keyword = jackTokenizer.keyWord();

        symbolTable.startSubroutine();

        if (jackTokenizer.keyWord() == JackTokenizer.KEYWORD.METHOD) {
            symbolTable.define("this", currentClass, Symbol.Kind.ARG);
        }

        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD || jackTokenizer.keyWord() != JackTokenizer.KEYWORD.VOID) {
            jackTokenizer.previous();
        }

        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
            unexpectedToken("subroutineName");
        }
        currentSubroutine = jackTokenizer.identifier();
        expectedSymbol('(');
        compileParameterList();
        expectedSymbol(')');
        compileSubroutineBody(keyword);
        compileSubroutine();
    }

    private void compileSubroutineBody(JackTokenizer.KEYWORD keyword) {
        expectedSymbol('{');
        compileVarDec();
        genFunctionDec(keyword);
        compileStatements();
        expectedSymbol('}');
    }

    private void genFunctionDec(JackTokenizer.KEYWORD keyword) {
        vmCodeGenerator.genFunction(currentFunction(), symbolTable.varCount(Symbol.Kind.VAR));
        if (keyword == JackTokenizer.KEYWORD.METHOD) {
            vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.ARG, 0);
            vmCodeGenerator.genPop(VMCodeGenerator.SEGMENT.POINTER, 0);
        } else if (keyword == JackTokenizer.KEYWORD.CONSTRUCTOR) {
            vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.CONST, symbolTable.varCount(Symbol.Kind.FIELD));
            vmCodeGenerator.genCall("Memory.alloc", 1);
            vmCodeGenerator.genPop(VMCodeGenerator.SEGMENT.POINTER, 0);
        }
    }

    private void compileStatements() {
        jackTokenizer.next();

        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jackTokenizer.symbol() == '}') {
            jackTokenizer.previous();
            return;
        }

        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD) {
            unexpectedToken("keyword");
        } else {
            switch (jackTokenizer.keyWord()) {
                case LET -> compileLet();
                case IF -> compileIf();
                case WHILE -> compileWhile();
                case DO -> compileDo();
                case RETURN -> compileReturn();
                default -> unexpectedToken("'let'|'if'|'while'|'do'|'return'");
            }
        }

        compileStatements();
    }

    private void compileParameterList() {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jackTokenizer.symbol() == ')') {
            jackTokenizer.previous();
            return;
        }

        jackTokenizer.previous();
        do {
            var compileType = compileType();

            jackTokenizer.next();
            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
                unexpectedToken("identifier");
            }

            symbolTable.define(jackTokenizer.identifier(), compileType, Symbol.Kind.ARG);

            jackTokenizer.next();
            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ')')) {
                unexpectedToken("',' or ')'");
            }

            if (jackTokenizer.symbol() == ')') {
                jackTokenizer.previous();
                break;
            }

        } while (true);

    }

    private void compileVarDec() {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD || jackTokenizer.keyWord() != JackTokenizer.KEYWORD.VAR) {
            jackTokenizer.previous();
            return;
        }
        var type = compileType();

        do {
            jackTokenizer.next();

            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
                unexpectedToken("identifier");
            }

            symbolTable.define(jackTokenizer.identifier(), type, Symbol.Kind.VAR);
            jackTokenizer.next();

            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ';')) {
                unexpectedToken("',' or ';'");
            }
        } while (jackTokenizer.symbol() != ';');
        compileVarDec();
    }

    private void compileDo() {
        compileSubroutineCall();
        expectedSymbol(';');
        vmCodeGenerator.genPop(VMCodeGenerator.SEGMENT.TEMP, 0);
    }

    private void compileLet() {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
            unexpectedToken("varName");
        }

        var varName = jackTokenizer.identifier();
        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (jackTokenizer.symbol() != '[' && jackTokenizer.symbol() != '=')) {
            unexpectedToken("'['|'='");
        }

        var isexp = false;
        if (jackTokenizer.symbol() == '[') {
            isexp = true;
            vmCodeGenerator.genPush(getSeg(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
            compileExpression();
            expectedSymbol(']');
            vmCodeGenerator.genArithmetic(VMCodeGenerator.COMMAND.ADD);
        }

        if (isexp) jackTokenizer.next();
        compileExpression();
        expectedSymbol(';');

        if (isexp) {
            vmCodeGenerator.genPop(VMCodeGenerator.SEGMENT.TEMP, 0);
            vmCodeGenerator.genPop(VMCodeGenerator.SEGMENT.POINTER, 1);
            vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.TEMP, 0);
            vmCodeGenerator.genPop(VMCodeGenerator.SEGMENT.THAT, 0);
        } else {
            vmCodeGenerator.genPop(getSeg(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
        }
    }

    private VMCodeGenerator.SEGMENT getSeg(Symbol.Kind kind) {
        return switch (kind) {
            case FIELD -> VMCodeGenerator.SEGMENT.THIS;
            case STATIC -> VMCodeGenerator.SEGMENT.STATIC;
            case VAR -> VMCodeGenerator.SEGMENT.LOCAL;
            case ARG -> VMCodeGenerator.SEGMENT.ARG;
            default -> VMCodeGenerator.SEGMENT.NONE;
        };

    }

    private void compileWhile() {
        var continueLabel = newLabel();
        var topLabel = newLabel();
        vmCodeGenerator.genLabel(topLabel);

        compileBlock(continueLabel, topLabel);
    }

    private void compileBlock(String label1, String label2) {
        expectedSymbol('(');
        compileExpression();
        expectedSymbol(')');
        vmCodeGenerator.genArithmetic(VMCodeGenerator.COMMAND.NOT);
        vmCodeGenerator.genIf(label1);
        expectedSymbol('{');
        compileStatements();
        expectedSymbol('}');
        vmCodeGenerator.genGoto(label2);
        vmCodeGenerator.genLabel(label1);
    }

    private String newLabel() {
        return "LABEL_" + (labelIndex++);
    }

    private void compileReturn() {
        jackTokenizer.next();

        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jackTokenizer.symbol() == ';') {
            vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.CONST, 0);
        } else {
            jackTokenizer.previous();
            compileExpression();
            expectedSymbol(';');
        }
        vmCodeGenerator.genReturn();

    }

    private void compileIf() {
        var elseLabel = newLabel();
        var endLabel = newLabel();
        compileBlock(elseLabel, endLabel);
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && jackTokenizer.keyWord() == JackTokenizer.KEYWORD.ELSE) {
            expectedSymbol('{');
            compileStatements();
            expectedSymbol('}');
        } else {
            jackTokenizer.previous();
        }
        vmCodeGenerator.genLabel(endLabel);
    }

    private void compileTerm() {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.IDENTIFIER) {
            var id = jackTokenizer.identifier();
            jackTokenizer.next();
            if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL) {
                final var symbol = jackTokenizer.symbol();
                switch (symbol) {
                    case '[' -> {
                        vmCodeGenerator.genPush(getSeg(symbolTable.kindOf(id)), symbolTable.indexOf(id));
                        compileExpression();
                        expectedSymbol(']');
                        vmCodeGenerator.genArithmetic(VMCodeGenerator.COMMAND.ADD);
                        vmCodeGenerator.genPop(VMCodeGenerator.SEGMENT.POINTER, 1);
                        vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.THAT, 0);
                    }
                    case '(', '.' -> {
                        jackTokenizer.previous();
                        jackTokenizer.previous();
                        compileSubroutineCall();
                    }
                    default -> {
                        jackTokenizer.previous();
                        vmCodeGenerator.genPush(getSeg(symbolTable.kindOf(id)), symbolTable.indexOf(id));
                    }
                }
            } else {
                jackTokenizer.previous();
                vmCodeGenerator.genPush(getSeg(symbolTable.kindOf(id)), symbolTable.indexOf(id));
            }
        } else {
            if (jackTokenizer.tokenType() == JackTokenizer.TYPE.INT_CONST) {
                vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.CONST, jackTokenizer.intVal());
            } else if (jackTokenizer.tokenType() == JackTokenizer.TYPE.STRING_CONST) {
                var str = jackTokenizer.stringVal();

                vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.CONST, str.length());
                vmCodeGenerator.genCall("String.new", 1);

                for (var c : str.toCharArray()) {
                    vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.CONST, c);
                    vmCodeGenerator.genCall("String.appendChar", 2);
                }
            } else if (jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD) {
                if (jackTokenizer.keyWord() == JackTokenizer.KEYWORD.TRUE) {
                    vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.CONST, 0);
                    vmCodeGenerator.genArithmetic(VMCodeGenerator.COMMAND.NOT);
                } else if (jackTokenizer.keyWord() == JackTokenizer.KEYWORD.THIS) {
                    vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.POINTER, 0);
                } else if (jackTokenizer.keyWord() == JackTokenizer.KEYWORD.FALSE || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.NULL) {
                    vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.CONST, 0);
                }
            } else if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL) {
                final var symbol = jackTokenizer.symbol();
                switch (symbol) {
                    case '(' -> {
                        compileExpression();
                        expectedSymbol(')');
                    }
                    case '-', '~' -> {
                        compileTerm();
                        if (symbol == '-') {
                            vmCodeGenerator.genArithmetic(VMCodeGenerator.COMMAND.NEG);
                        } else {
                            vmCodeGenerator.genArithmetic(VMCodeGenerator.COMMAND.NOT);
                        }
                    }
                }
            } else {
                unexpectedToken("integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term");
            }
        }
    }

    private void compileSubroutineCall() {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
            unexpectedToken("identifier");
        }

        var name = jackTokenizer.identifier();
        var num = 0;

        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL) {
            switch (jackTokenizer.symbol()) {
                case '(' -> {
                    vmCodeGenerator.genPush(VMCodeGenerator.SEGMENT.POINTER, 0);
                    num = compileExpressionList() + 1;
                    expectedSymbol(')');
                    vmCodeGenerator.genCall(currentClass + '.' + name, num);
                }
                case '.' -> {
                    var objName = name;
                    jackTokenizer.next();
                    if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
                        unexpectedToken("identifier");
                    }
                    name = jackTokenizer.identifier();
                    var type = symbolTable.typeOf(objName);
                    switch (type) {
                        case "int", "boolean", "char", "void" -> unexpectedToken("no built-in type");
                        case "" -> name = objName + "." + name;
                        default -> {
                            num = 1;
                            vmCodeGenerator.genPush(getSeg(symbolTable.kindOf(objName)), symbolTable.indexOf(objName));
                            name = symbolTable.typeOf(objName) + "." + name;
                        }
                    }
                    expectedSymbol('(');
                    num += compileExpressionList();
                    expectedSymbol(')');
                    vmCodeGenerator.genCall(name, num);
                }
                default -> unexpectedToken("'('|'.'");
            }
        } else {
            unexpectedToken("'('|'.'");
        }
    }
    

    private void compileExpression() {
        compileTerm();
        do {
            jackTokenizer.next();
            if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jackTokenizer.isOperation()) {
                var opCmd = "";
                switch (jackTokenizer.symbol()) {
                    case '+' -> opCmd = "add";
                    case '-' -> opCmd = "sub";
                    case '*' -> opCmd = "call Math.multiply 2";
                    case '/' -> opCmd = "call Math.divide 2";
                    case '<' -> opCmd = "lt";
                    case '>' -> opCmd = "gt";
                    case '=' -> opCmd = "eq";
                    case '&' -> opCmd = "and";
                    case '|' -> opCmd = "or";
                    default -> unexpectedToken("Unknown op!");
                }
                compileTerm();
                vmCodeGenerator.genCommand(opCmd, "", "");

            } else {
                jackTokenizer.previous();
                break;
            }

        } while (true);

    }

    private int compileExpressionList() {
        var num = 0;

        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jackTokenizer.symbol() == ')') {
            jackTokenizer.previous();
        } else {
            num = 1;
            jackTokenizer.previous();
            compileExpression();
            do {
                jackTokenizer.next();
                if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jackTokenizer.symbol() == ',') {
                    compileExpression();
                    num++;
                } else {
                    jackTokenizer.previous();
                    break;
                }

            } while (true);
        }

        return num;
    }

    private void unexpectedToken(String expectedToken) {
        throw new IllegalStateException("Expected token missing : " + expectedToken + " Current token:" + jackTokenizer.getCurrentToken());
    }

    private void expectedSymbol(char symbol) {
        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || jackTokenizer.symbol() != symbol) {
            unexpectedToken("'" + symbol + "'");
        }
    }
}
