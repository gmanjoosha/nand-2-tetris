package org.qualcomm.manjoosha;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JackTokenizer {

    public enum TYPE {
        KEYWORD,
        SYMBOL,
        IDENTIFIER,
        INT_CONST,
        STRING_CONST,
        NONE
    }

    public enum KEYWORD {
        CLASS,
        METHOD, FUNCTION, CONSTRUCTOR,
        INT, BOOLEAN, CHAR, VOID,
        VAR, STATIC, FIELD,
        LET, DO, IF, ELSE, WHILE,
        RETURN,
        TRUE, FALSE, NULL,
        THIS
    }

    private String currentToken;
    private TYPE currentTokenType;
    private int pointer;
    private final ArrayList<String> tokens;

    private static final HashMap<String, KEYWORD> KEYWORD_TO_STRING = new HashMap<>();
    private static final HashSet<Character> OPERATIONS = new HashSet<>();
    private static final EnumMap<TYPE, String> TYPE_TO_REGEX = new EnumMap<>(TYPE.class);
    private static final Pattern tokenPatterns;

    static {
        KEYWORD_TO_STRING.put("class", KEYWORD.CLASS);
        KEYWORD_TO_STRING.put("constructor", KEYWORD.CONSTRUCTOR);
        KEYWORD_TO_STRING.put("function", KEYWORD.FUNCTION);
        KEYWORD_TO_STRING.put("method", KEYWORD.METHOD);
        KEYWORD_TO_STRING.put("field", KEYWORD.FIELD);
        KEYWORD_TO_STRING.put("static", KEYWORD.STATIC);
        KEYWORD_TO_STRING.put("var", KEYWORD.VAR);
        KEYWORD_TO_STRING.put("int", KEYWORD.INT);
        KEYWORD_TO_STRING.put("char", KEYWORD.CHAR);
        KEYWORD_TO_STRING.put("boolean", KEYWORD.BOOLEAN);
        KEYWORD_TO_STRING.put("void", KEYWORD.VOID);
        KEYWORD_TO_STRING.put("true", KEYWORD.TRUE);
        KEYWORD_TO_STRING.put("false", KEYWORD.FALSE);
        KEYWORD_TO_STRING.put("null", KEYWORD.NULL);
        KEYWORD_TO_STRING.put("this", KEYWORD.THIS);
        KEYWORD_TO_STRING.put("let", KEYWORD.LET);
        KEYWORD_TO_STRING.put("do", KEYWORD.DO);
        KEYWORD_TO_STRING.put("if", KEYWORD.IF);
        KEYWORD_TO_STRING.put("else", KEYWORD.ELSE);
        KEYWORD_TO_STRING.put("while", KEYWORD.WHILE);
        KEYWORD_TO_STRING.put("return", KEYWORD.RETURN);

        OPERATIONS.add('+');
        OPERATIONS.add('-');
        OPERATIONS.add('*');
        OPERATIONS.add('/');
        OPERATIONS.add('&');
        OPERATIONS.add('|');
        OPERATIONS.add('<');
        OPERATIONS.add('>');
        OPERATIONS.add('=');

        var keyWordRegex = String.join("|", KEYWORD_TO_STRING.keySet());
        var symbolRegex = "[&*+()./,\\-\\];~}|{>=\\[<]";
        var intRegex = "[0-9]+";
        var strRegex = "\"[^\"\n]*\"";
        var idRegex = "[a-zA-Z_]\\w*";
        TYPE_TO_REGEX.put(TYPE.KEYWORD, keyWordRegex);
        TYPE_TO_REGEX.put(TYPE.SYMBOL, symbolRegex);
        TYPE_TO_REGEX.put(TYPE.INT_CONST, intRegex);
        TYPE_TO_REGEX.put(TYPE.STRING_CONST, strRegex);
        TYPE_TO_REGEX.put(TYPE.IDENTIFIER, idRegex);

        tokenPatterns = Pattern.compile(String.join("|", TYPE_TO_REGEX.values()));
    }

    public JackTokenizer(File jackFile) {
        var parsed = readAndConcatAllLines(jackFile);
        var tokenMatcher = tokenPatterns.matcher(parsed);
        tokens = new ArrayList<>();
        pointer = 0;
        while (tokenMatcher.find()) {
            tokens.add(tokenMatcher.group());
        }

        currentToken = "";
        currentTokenType = TYPE.NONE;

    }

    private String readAndConcatAllLines(File jackFile) {
        try {
            return Files.readAllLines(jackFile.toPath()).stream()
                    .filter(line -> !line.isEmpty())
                    .map(JackTokenizer::removeAllComments)
                    .map(String::trim)
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean hasNextToken() {
        return pointer < tokens.size();
    }

    public void next() {
        if (hasNextToken()) {
            currentToken = tokens.get(pointer);
            pointer++;
        } else {
            throw new IllegalStateException("No more tokens");
        }
        currentTokenType = matchCurrentTokenType();
    }

    private TYPE matchCurrentTokenType() {
        return TYPE_TO_REGEX.entrySet().stream()
                .filter(entry -> currentToken.matches(entry.getValue()))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown token:" + currentToken));
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public TYPE tokenType() {
        return currentTokenType;
    }

    public KEYWORD keyWord() {
        if (currentTokenType == TYPE.KEYWORD) {
            return KEYWORD_TO_STRING.get(currentToken);
        } else {
            throw new IllegalStateException(" keyword token expected!");
        }
    }
    
    public char symbol() {
        if (currentTokenType == TYPE.SYMBOL) {
            return currentToken.charAt(0);
        } else {
            throw new IllegalStateException("symbol token expected!");
        }
    }
    
    public String identifier() {
        if (currentTokenType == TYPE.IDENTIFIER) {
            return currentToken;
        } else {
            throw new IllegalStateException("identifier is expected! current type:" + currentTokenType);
        }
    }
    
    public int intVal() {
        if (currentTokenType == TYPE.INT_CONST) {
            return Integer.parseInt(currentToken);
        } else {
            throw new IllegalStateException("Integer constant is expected!");
        }
    }
    
    public String stringVal() {

        if (currentTokenType == TYPE.STRING_CONST) {

            return currentToken.substring(1, currentToken.length() - 1);

        } else {
            throw new IllegalStateException("Current token is not a string constant!");
        }
    }
    
    public void previous() {
        if (pointer > 0) {
            pointer--;
            currentToken = tokens.get(pointer);
        }

    }

    public boolean isOperation() {
        return OPERATIONS.contains(symbol());
    }
    
    public static String removeAllComments(String strIn) {
        return strIn.replaceAll("//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1");
    }
    
}
