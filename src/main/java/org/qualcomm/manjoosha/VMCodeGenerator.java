package org.qualcomm.manjoosha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EnumMap;

public class VMCodeGenerator {

    private static final EnumMap<SEGMENT, String> SEGMENT_TO_STRING = new EnumMap<>(SEGMENT.class);
    private static final EnumMap<COMMAND, String> COMMAND_TO_STRING = new EnumMap<>(COMMAND.class);

    static {
        SEGMENT_TO_STRING.put(SEGMENT.CONST, "constant");
        SEGMENT_TO_STRING.put(SEGMENT.ARG, "argument");
        SEGMENT_TO_STRING.put(SEGMENT.LOCAL, "local");
        SEGMENT_TO_STRING.put(SEGMENT.STATIC, "static");
        SEGMENT_TO_STRING.put(SEGMENT.THIS, "this");
        SEGMENT_TO_STRING.put(SEGMENT.THAT, "that");
        SEGMENT_TO_STRING.put(SEGMENT.POINTER, "pointer");
        SEGMENT_TO_STRING.put(SEGMENT.TEMP, "temp");

        COMMAND_TO_STRING.put(COMMAND.ADD, "add");
        COMMAND_TO_STRING.put(COMMAND.SUB, "sub");
        COMMAND_TO_STRING.put(COMMAND.NEG, "neg");
        COMMAND_TO_STRING.put(COMMAND.EQ, "eq");
        COMMAND_TO_STRING.put(COMMAND.GT, "gt");
        COMMAND_TO_STRING.put(COMMAND.LT, "lt");
        COMMAND_TO_STRING.put(COMMAND.AND, "and");
        COMMAND_TO_STRING.put(COMMAND.OR, "or");
        COMMAND_TO_STRING.put(COMMAND.NOT, "not");
    }

    private PrintWriter printWriter;
    
    public VMCodeGenerator(File fOut) {
        try {
            printWriter = new PrintWriter(fOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void genPush(SEGMENT segment, int index) {
        genCommand("push", SEGMENT_TO_STRING.get(segment), String.valueOf(index));
    }


    public void genPop(SEGMENT segment, int index) {
        genCommand("pop", SEGMENT_TO_STRING.get(segment), String.valueOf(index));
    }

    public void genArithmetic(COMMAND command) {
        genCommand(COMMAND_TO_STRING.get(command), "", "");
    }

    public void genLabel(String label) {
        genCommand("label", label, "");
    }

    public void genGoto(String label) {
        genCommand("goto", label, "");
    }

    public void genIf(String label) {
        genCommand("if-goto", label, "");
    }

    public void genCall(String name, int nArgs) {
        genCommand("call", name, String.valueOf(nArgs));
    }

    public void genFunction(String name, int nLocals) {
        genCommand("function", name, String.valueOf(nLocals));
    }

    public void genReturn() {
        genCommand("return", "", "");
    }

    public void genCommand(String cmd, String arg1, String arg2) {

        printWriter.print(cmd + " " + arg1 + " " + arg2 + "\n");

    }

    public void close() {
        printWriter.close();
    }

    public enum SEGMENT {CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP, NONE}

    public enum COMMAND {ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT}
}

