package org.qualcomm.manjoosha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CompilationEngine2 {

    private PrintWriter printWriter;
    private PrintWriter tokenPrintWriter;
    private JackTokenizer jackTokenizer;

    public CompilationEngine2(File inFile, File outFile, File outTokenFile) {

        try {

            jackTokenizer = new JackTokenizer(inFile);
            printWriter = new PrintWriter(outFile);
            tokenPrintWriter = new PrintWriter(outTokenFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
    
    private void compileType(){
        jackTokenizer.next();
        boolean isType = false;

        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD
                && (jackTokenizer.keyWord() == JackTokenizer.KEYWORD.INT
                || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.CHAR
                || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.BOOLEAN)) {
            printWriter.print("<keyword>" + jackTokenizer.getCurrentToken() + "</keyword>\n");
            tokenPrintWriter.print("<keyword>" + jackTokenizer.getCurrentToken() + "</keyword>\n");
            isType = true;
        }
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.IDENTIFIER){
            printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
            tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
            isType = true;
        }

        if (!isType) unexpectedToken("in|char|boolean|className");
    }
    
    public void compileClass(){
        jackTokenizer.next();
        checkForKeyWordClass();

        printWriter.print("<class>\n");
        tokenPrintWriter.print("<tokens>\n");

        printWriter.print("<keyword>class</keyword>\n");
        tokenPrintWriter.print("<keyword>class</keyword>\n");

        jackTokenizer.next();

        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
            unexpectedToken("className");
        }

        printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
        tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
        expectedSymbol('{');
        compileClassVarDec();
        compileSubroutine();
        expectedSymbol('}');

        if (jackTokenizer.hasNextToken()){
            throw new IllegalStateException("Unexpected tokens");
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
        if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == '}'){
            jackTokenizer.previous();
            return;
        }
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD){
            error("Keywords");
        }
        if (isSubroutineKeyword()){
            jackTokenizer.previous();
            return;
        } else {
            unexpectedToken("Keywords");
        }

        printWriter.print("<classVarDec>\n");
        if (jackTokenizer.keyWord() != JackTokenizer.STATIC && jackTokenizer.keyWord() != JackTokenizer.FIELD){
            unexpectedToken("static or field");
        }

        printWriter.print("<keyword>" + jackTokenizer.getCurrentToken() + "</keyword>\n");
        tokenPrintWriter.print("<keyword>" + jackTokenizer.getCurrentToken() + "</keyword>\n");

        compileType();

        boolean varNamesDone = false;

        do {
            jackTokenizer.next();
            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
                unexpectedToken("identifier");
            }

            printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
            tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");

            jackTokenizer.next();

            if (jackTokenizer.tokenType() != JackTokenizer.SYMBOL || (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ';')){
                unexpectedToken("',' or ';'");
            }
            if (jackTokenizer.symbol() == ','){

                printWriter.print("<symbol>,</symbol>\n");
                tokenPrintWriter.print("<symbol>,</symbol>\n");
            }else {
                printWriter.print("<symbol>;</symbol>\n");
                tokenPrintWriter.print("<symbol>;</symbol>\n");
                break;
            }
        }while(true);
        printWriter.print("</classVarDec>\n");
        compileClassVarDec();
    }
    private boolean isSubroutineKeyword() {
        return jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD
                && (jackTokenizer.keyWord() == JackTokenizer.KEYWORD.CONSTRUCTOR
                || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.FUNCTION
                || jackTokenizer.keyWord() == JackTokenizer.KEYWORD.METHOD);
    }
    private void compileSubroutine(){
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == '}'){
            jackTokenizer.previous();
            return;
        }

        if (!isSubroutineKeyword()) {
            unexpectedToken("constructor|function|method");
        }

        printWriter.print("<subroutineDec>\n");

        printWriter.print("<keyword>" + jackTokenizer.getCurrentToken() + "</keyword>\n");
        tokenPrintWriter.print("<keyword>" + jackTokenizer.getCurrentToken() + "</keyword>\n");

        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && jackTokenizer.keyWord() == JackTokenizer.VOID){
            printWriter.print("<keyword>void</keyword>\n");
            tokenPrintWriter.print("<keyword>void</keyword>\n");
        }else {
            jackTokenizer.previous();
            compileType();
        }
        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
            unexpectedToken("subroutineName");
        }
        printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
        tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");

        expectedSymbol('(');
        printWriter.print("<parameterList>\n");
        compileParameterList();
        printWriter.print("</parameterList>\n");
        expectedSymbol(')');
        compileSubroutineBody();

        printWriter.print("</subroutineDec>\n");

        compileSubroutine();

    }

    private void compileSubroutineBody(){
        printWriter.print("<subroutineBody>\n");
        expectedSymbol('{');
        compileVarDec();
        printWriter.print("<statements>\n");
        compileStatement();
        printWriter.print("</statements>\n");
        requireSymbol('}');
        printWriter.print("</subroutineBody>\n");
    }

    private void compileStatement(){
        jackTokenizer.next();

        //next is a '}'
        if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == '}'){
            jackTokenizer.previous();
            return;
        }
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD){
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

    private void compileParameterList(){
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == ')'){
            jackTokenizer.previous();
            return;
        }

        jackTokenizer.previous();
        do {
            compileType();

            jackTokenizer.next();
            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
                unexpectedToken("identifier");
            }
             printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
            tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");

            jackTokenizer.next();
            if (jackTokenizer.tokenType() != JackTokenizer.SYMBOL || (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ')')){
                unexpectedToken("',' or ')'");
            }

            if (jackTokenizer.symbol() == ','){
                printWriter.print("<symbol>,</symbol>\n");
                tokenPrintWriter.print("<symbol>,</symbol>\n");
            }else {
                jackTokenizer.previous();
                break;
            }

        }while(true);

    }

    private void compileVarDec(){
        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD || jackTokenizer.keyWord() != JackTokenizer.VAR){
            jackTokenizer.previous();
            return;
        }

        printWriter.print("<varDec>\n");

        printWriter.print("<keyword>var</keyword>\n");
        tokenPrintWriter.print("<keyword>var</keyword>\n");

        compileType();

        boolean varNamesDone = false;

        do {
            jackTokenizer.next();

            if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
                unexpectedToken("identifier");
            }

            printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
            tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");

            jackTokenizer.next();

            if (jackTokenizer.tokenType() != JackTokenizer.SYMBOL || (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ';')){
                unexpectedToken("',' or ';'");
            }

            if (jackTokenizer.symbol() == ','){

                printWriter.print("<symbol>,</symbol>\n");
                tokenPrintWriter.print("<symbol>,</symbol>\n");

            }else {

                printWriter.print("<symbol>;</symbol>\n");
                tokenPrintWriter.print("<symbol>;</symbol>\n");
                break;
            }


        }while(true);

        printWriter.print("</varDec>\n");

        compileVarDec();

    }
    private void compileDo(){
        printWriter.print("<doStatement>\n");

        printWriter.print("<keyword>do</keyword>\n");
        tokenPrintWriter.print("<keyword>do</keyword>\n");
        compileSubroutineCall();
        expectedSymbol(';');

        printWriter.print("</doStatement>\n");
    }

    private void compileLet(){

        printWriter.print("<letStatement>\n");

        printWriter.print("<keyword>let</keyword>\n");
        tokenPrintWriter.print("<keyword>let</keyword>\n");

        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
            expectedSymbol("varName");
        }

        printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
        tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");

        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.SYMBOL || (jackTokenizer.symbol() != '[' && jackTokenizer.symbol() != '=')){
            expectedSymbol("'['|'='");
        }

        boolean expExist = false;

        if (jackTokenizer.symbol() == '['){

            expExist = true;

            printWriter.print("<symbol>[</symbol>\n");
            tokenPrintWriter.print("<symbol>[</symbol>\n");

            compileExpression();

            jackTokenizer.next();
            if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == ']'){
                printWriter.print("<symbol>]</symbol>\n");
                tokenPrintWriter.print("<symbol>]</symbol>\n");
            }else {
                unexpectedtoken("']'");
            }
        }

        if (expExist) jackTokenizer.next();

        printWriter.print("<symbol>=</symbol>\n");
        tokenPrintWriter.print("<symbol>=</symbol>\n");

        compileExpression();

        expectedSymbol(';');

        printWriter.print("</letStatement>\n");
    }

    private void compilesWhile(){
        printWriter.print("<whileStatement>\n");

        printWriter.print("<keyword>while</keyword>\n");
        tokenPrintWriter.print("<keyword>while</keyword>\n");

        expectedSymbol('(');
        compileExpression();
        expectedSymbol(')');
        expectedSymbol('{');
        printWriter.print("<statements>\n");
        compileStatements();
        printWriter.print("</statements>\n");
        expectedSymbol('}');

        printWriter.print("</whileStatement>\n");
    }

    private void compileReturn(){
        printWriter.print("<returnStatement>\n");

        printWriter.print("<keyword>return</keyword>\n");
        tokenPrintWriter.print("<keyword>return</keyword>\n");

        jackTokenizer.next();

        if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == ';'){
            printWriter.print("<symbol>;</symbol>\n");
            tokenPrintWriter.print("<symbol>;</symbol>\n");
            printWriter.print("</returnStatement>\n");
            return;
        }

        jackTokenizer.previous();
        compileExpression();
        expectedSymbol(';');

        printWriter.print("</returnStatement>\n");
    }

    private void compileIf(){
        printWriter.print("<ifStatement>\n");

        printWriter.print("<keyword>if</keyword>\n");
        tokenPrintWriter.print("<keyword>if</keyword>\n");
        expectedSymbol('(');
        compileExpression();
        expectedSymbol(')');
        expectedSymbol('{');
        printWriter.print("<statements>\n");
        compileStatements();
        printWriter.print("</statements>\n");
        expectedSymbol('}');

        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && jackTokenizer.keyWord() == JackTokenizer.ELSE){
            printWriter.print("<keyword>else</keyword>\n");
            tokenPrintWriter.print("<keyword>else</keyword>\n");
            expectedSymbol('{');
            printWriter.print("<statements>\n");
            compileStatements();
            printWriter.print("</statements>\n");
            expectedSymbol('}');
        }else {
            jackTokenizer.previous();
        }

        printWriter.print("</ifStatement>\n");

    }
    private void compileTerm() {
        printWriter.print("<term>\n");
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.IDENTIFIER){
            String id = jackTokenizer.identifier();

            jackTokenizer.next();
            if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL) {
                final var symbol = jackTokenizer.symbol();
                switch (symbol) {
                    case '[' -> {
                        printWriter.print("<identifier>" + id + "</identifier>\n");
                        tokenPrintWriter.print("<identifier>" + id + "</identifier>\n");
                        printWriter.print("<symbol>[</symbol>\n");
                        tokenPrintWriter.print("<symbol>[</symbol>\n");
                        compileExpression();
                        expectedSymbol(']');
                    }
                    case '(', '.' -> {
                        jackTokenizer.previous();
                        jackTokenizer.previous();
                        compileSubroutineCall();
                    }
                    default -> {
                        printWriter.print("<identifier>" + id + "</identifier>\n");
                        tokenPrintWriter.print("<identifier>" + id + "</identifier>\n");
                        jackTokenizer.previous();
                    }
                }
            } else {
                printWriter.print("<identifier>" + id + "</identifier>\n");
                tokenPrintWriter.print("<identifier>" + id + "</identifier>\n");
                jackTokenizer.previous();
            }

        }else{
            if (jackTokenizer.tokenType() == JackTokenizer.KEYWORD.INT_CONST){
                printWriter.print("<integerConstant>" + jackTokenizer.intVal() + "</integerConstant>\n");
                tokenPrintWriter.print("<integerConstant>" + jackTokenizer.intVal() + "</integerConstant>\n");
            }else if (jackTokenizer.tokenType() == JackTokenizer.STRING_CONST){
                printWriter.print("<stringConstant>" + jackTokenizer.stringVal() + "</stringConstant>\n");
                tokenPrintWriter.print("<stringConstant>" + jackTokenizer.stringVal() + "</stringConstant>\n");
            }else if(jackTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD &&
                            (jackTokenizer.keyWord() == JackTokenizer.TRUE ||
                            jackTokenizer.keyWord() == JackTokenizer.FALSE ||
                            jackTokenizer.keyWord() == JackTokenizer.NULL ||
                            jackTokenizer.keyWord() == JackTokenizer.THIS)){
                    printWriter.print("<keyword>" + jackTokenizer.getCurrentToken() + "</keyword>\n");
                    tokenPrintWriter.print("<keyword>" + jackTokenizer.getCurrentToken() + "</keyword>\n");
            }else if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == '('){
                printWriter.print("<symbol>(</symbol>\n");
                tokenPrintWriter.print("<symbol>(</symbol>\n");
                compileExpression();
                expectedSymbol(')');
            }else if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && (jackTokenizer.symbol() == '-' || jackTokenizer.symbol() == '~')){
                printWriter.print("<symbol>" + jackTokenizer.symbol() + "</symbol>\n");
                tokenPrintWriter.print("<symbol>" + jackTokenizer.symbol() + "</symbol>\n");
                compileTerm();
            }else {
                unexpectedToken("integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term");
            }
        }

        printWriter.print("</term>\n");
    }
    private void compileSubroutineCall(){

        jackTokenizer.next();
        if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
            unexpectedToken("identifier");
        }

        printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
        tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");

        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL) {
            switch (jackTokenizer.symbol()) {
                case '(' -> {
                    //'(' expressionList ')'
                    printWriter.print("<symbol>(</symbol>\n");
                    tokenPrintWriter.print("<symbol>(</symbol>\n");
                    //expressionList
                    printWriter.print("<expressionList>\n");
                    compileExpressionList();
                    printWriter.print("</expressionList>\n");
                    //')'
                    requireSymbol(')');
                }
                case '.' -> {
                    //(className|varName) '.' subroutineName '(' expressionList ')'
                    printWriter.print("<symbol>.</symbol>\n");
                    tokenPrintWriter.print("<symbol>.</symbol>\n");
                    //subroutineName
                    jackTokenizer.next();
                    if (jackTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
                        error("identifier");
                    }
                    printWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
                    tokenPrintWriter.print("<identifier>" + jackTokenizer.identifier() + "</identifier>\n");
                    //'('
                    requireSymbol('(');
                    //expressionList
                    printWriter.print("<expressionList>\n");
                    compileExpressionList();
                    printWriter.print("</expressionList>\n");
                    //')'
                    requireSymbol(')');
                }
                default -> unexpectedToken("'('|'.'");
            }
        }else {
            unexpectedToken("'('|'.'");
        }
    }

    private void compileExpression(){
        printWriter.print("<expression>\n");
        compileTerm();
        do {
            jackTokenizer.next();
            if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.isOp()){
                switch (jackTokenizer.symbol()) {
                    case '>' -> {
                        printWriter.print("<symbol>&gt;</symbol>\n");
                        tokenPrintWriter.print("<symbol>&gt;</symbol>\n");
                    }
                    case '<' -> {
                        printWriter.print("<symbol>&lt;</symbol>\n");
                        tokenPrintWriter.print("<symbol>&lt;</symbol>\n");
                    }
                    case '&' -> {
                        printWriter.print("<symbol>&amp;</symbol>\n");
                        tokenPrintWriter.print("<symbol>&amp;</symbol>\n");
                    }
                    default -> {
                        printWriter.print("<symbol>" + jackTokenizer.symbol() + "</symbol>\n");
                        tokenPrintWriter.print("<symbol>" + jackTokenizer.symbol() + "</symbol>\n");
                    }
                }
                compileTerm();
            }else {
                jackTokenizer.previous();
                break;
            }

        }while (true);

        printWriter.print("</expression>\n");
    }
    private void compileExpressionList(){
        jackTokenizer.next();

        if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == ')'){
            jackTokenizer.previous();
        }else {
            jackTokenizer.previous();
            compileExpression();
            do {
                jackTokenizer.next();
                if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == ','){
                    printWriter.print("<symbol>,</symbol>\n");
                    tokenPrintWriter.print("<symbol>,</symbol>\n");
                    compileExpression();
                }else {
                    jackTokenizer.previous();
                    break;
                }

            }while (true);

        }
    }

    private void unexpectedToken(String val){
        throw new IllegalStateException("Expected token missing : " + val + " Current token:" + jackTokenizer.getCurrentToken());
    }

    private void expectedSymbol(char symbol){
        jackTokenizer.next();
        if (jackTokenizer.tokenType() == JackTokenizer.SYMBOL && jackTokenizer.symbol() == symbol){
            printWriter.print("<symbol>" + symbol + "</symbol>\n");
            tokenPrintWriter.print("<symbol>" + symbol + "</symbol>\n");
        }else {
            unexpectedToken("'" + symbol + "'");
        }
    }
}
